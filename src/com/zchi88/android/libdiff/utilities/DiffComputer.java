package com.zchi88.android.libdiff.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Scanner;

import com.zchi88.android.libdiff.versionobject.LibraryVersion;

/**
 * The purpose of this class to to compute diffs between successive versions of
 * a software library.
 */
public class DiffComputer {
	/**
	 * Checks to see if all versions of a library have diffs computed for them.
	 * 
	 * @param libraryPath
	 * @return True if diffs have been computed for all versions of the library.
	 *         False otherwise
	 * @throws IOException
	 */
	private static Boolean isDiffMissing(File[] libraryVersions) throws IOException {
		int libCount = 0;
		int diffCount = 0;
		for (File libFile : libraryVersions) {
			String nameOfLib = libFile.toString();
			// Check for number of jar files in the library
			if (nameOfLib.endsWith(".jar")) {
				libCount++;
			}

			// Check for number of diff files in that directory.
			File diffFile = new File(libFile, "diff.txt");
			if (libFile.isDirectory() && diffFile.exists()) {
				if (isDiffValid(diffFile)) {
					diffCount++;
				}
			}
		}
		return (diffCount != libCount);
	}

	/**
	 * This method re-computes the diffs for all versions of a library.
	 * 
	 * @param versionOrder
	 *            - list of all the files in a given library
	 * @throws IOException
	 */
	private static void computeDiffs(LinkedList<File> versionOrder) throws IOException {
		// Create an arraylist to temporarily hold the LibraryVersion objects
		// for comparison
		ArrayList<LibraryVersion> versionsList = new ArrayList<LibraryVersion>();

		// Iterate through each file in a given library. If the file is a
		// directory with a corresponding JAR, we know that it is a directory
		// containing source code decompiled from that jar.
		for (int i = 0; i < versionOrder.size(); i++) {
			// Check that both the JAR and its decompiled folder exists
			File libFile = versionOrder.get(i);
			Path libPath = Paths.get(libFile.toString().replace(".jar", ""));
			if (libPath.toFile().exists() && libFile.exists()) {
				// Create a library version object to capture the properties
				// of that version
				LibraryVersion libVersion = new LibraryVersion(libPath, i);
				// Add the library version object to an arraylist for future
				// computation
				versionsList.add(libVersion);
			}
		}

		// If there is only one version of a library, then all its files must be
		// version exclusive
		if (versionsList.size() == 1) {
			LibraryVersion libVersion = versionsList.get(0);
			for (File libFile : libVersion.getFilesMap().keySet()) {
				libVersion.appendToExclusiveList(libFile);
			}
		} else {
			// For each version's file mapping, compare the mapping to the other
			// versions' files maps. If a file does not exist in any other
			// version, it is version exclusive. If it does exist in other
			// versions but their hash codes never match, then it exists as a
			// uniquely modified file.

			// Iterate through the file maps and populate the modified files
			// list for each library version
			LibraryVersion currentVersion;
			LibraryVersion compVersion;
			String currentHashCode;
			String compHashCode;
			ArrayList<File> currentFilesList;

			// Filter out the exclusive files
			for (int currentIndex = 0; currentIndex < versionsList.size(); currentIndex++) {
				currentVersion = versionsList.get(currentIndex);
				currentFilesList = currentVersion.getFilesList();

				for (File file : currentFilesList) {
					Boolean isExclusive = true;
					currentHashCode = currentVersion.getHashValue(file);
					if (currentHashCode != null) {
						for (int compIndex = 0; compIndex < versionsList.size(); compIndex++) {
							if (compIndex != currentIndex) {
								compVersion = versionsList.get(compIndex);
								compHashCode = compVersion.getHashValue(file);
								if (compHashCode != null) {
									isExclusive = false;
								}
							}
						}
					}
					if (isExclusive) {
						currentVersion.appendToExclusiveList(file);
						currentVersion.removeFromMap(file);
						isExclusive = true;
					}
				}
			}

			// Filter out the copied files
			for (int currentIndex = 0; currentIndex < versionsList.size(); currentIndex++) {
				currentVersion = versionsList.get(currentIndex);
				currentFilesList = currentVersion.getFilesList();

				for (File file : currentFilesList) {
					Boolean isCopied = false;
					currentHashCode = currentVersion.getHashValue(file);

					if (currentHashCode != null) {
						for (int compIndex = (currentIndex + 1); compIndex < versionsList.size(); compIndex++) {
							compVersion = versionsList.get(compIndex);
							compHashCode = compVersion.getHashValue(file);

							if (compHashCode != null) {
								if (compHashCode.equals(currentHashCode)) {
									isCopied = true;
									compVersion.appendToCopiedList(file);
									compVersion.removeFromMap(file);
								}
							}
						}
					}
					if (isCopied) {
						currentVersion.appendToCopiedList(file);
						currentVersion.removeFromMap(file);
						isCopied = false;
					}
				}
			}

			// Anything still left in the hash map should be unique file
			// mods
			for (int currentIndex = 0; currentIndex < versionsList.size(); currentIndex++) {
				currentVersion = versionsList.get(currentIndex);
				for (File file : currentVersion.getFilesMap().keySet()) {

					currentVersion.appendToModdedList(file);
				}
			}
		}

		writeToDiff(versionsList);
	}

	/**
	 * Takes an arraylist of library version objects and writes their diff data
	 * to the diff.txt file
	 * 
	 * @param versionsList
	 * @throws IOException
	 */
	private static void writeToDiff(ArrayList<LibraryVersion> versionsList) {
		for (LibraryVersion libraryVersion : versionsList) {
			File libDiffFilePath = new File(libraryVersion.getVersionPath() + java.io.File.separator + "diff.txt");

			ArrayList<File> exclusiveFiles = libraryVersion.getExclusiveFiles();
			ArrayList<File> moddedFiles = libraryVersion.getModdedFiles();
			ArrayList<File> copiedFiles = libraryVersion.getCopiedFiles();

			Collections.sort(exclusiveFiles);
			Collections.sort(moddedFiles);
			Collections.sort(copiedFiles);

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(libDiffFilePath));) {
				writer.write("Showing diffs for: " + libraryVersion.getVersionName());
				writer.newLine();
				writer.write("Version Age: " + libraryVersion.getVersionAge());
				writer.newLine();
				writer.newLine();

				writer.write("Version-exclusive files: " + exclusiveFiles.size() + " files");
				writer.newLine();
				writer.write("====================");
				writer.newLine();
				for (File file : exclusiveFiles) {
					writer.write(file.toString());
					writer.newLine();
				}

				writer.newLine();
				writer.newLine();
				writer.write("Uniquely modified files: " + moddedFiles.size() + " files");
				writer.newLine();
				writer.write("====================");
				writer.newLine();
				for (File file : moddedFiles) {
					writer.write(file.toString());
					writer.newLine();
				}

				writer.newLine();
				writer.newLine();
				writer.write("Copied files: " + copiedFiles.size() + " files");
				writer.newLine();
				writer.write("====================");
				writer.newLine();
				for (File file : copiedFiles) {
					writer.write(file.toString());
					writer.newLine();
				}
				writer.newLine();
				writer.write("=====END OF DIFF=====");
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	/**
	 * Checks to see if diffs for a library are up to date. If not, it calls on
	 * the computeDiffs() method to re-compute the diffs for the library.
	 * 
	 * This is done by checking that there is a "diff.txt" file corresponding to
	 * each version of the library.
	 * 
	 * @throws IOException
	 */
	public static void syncDiffs(Path libraryPath) throws IOException {
		System.out.format("Checking if diffs have been computed for all versions of %s...\n", libraryPath);
		File[] libraryVersions = libraryPath.toFile().listFiles();

		if (libraryVersions.length > 0) {
			if (isDiffMissing(libraryVersions)) {
				System.out.format("Diffs for '%s' appear to be out of date. Recomputing diffs...\n", libraryPath);
				LinkedList<File> versionOrder = JarComparator.getVersionOrder(libraryPath);
				computeDiffs(versionOrder);
				System.out.format("Diffs for '%s' are now up to date.\n\n", libraryPath);
			} else {
				System.out.println("Diffs for " + libraryPath + " are up to date.\n");
			}
		}

	}

	/**
	 * Checks to see if a diff was properly created by looking for the
	 * "End of diff" marker in the text file.
	 * 
	 * @throws IOException
	 */
	public static Boolean isDiffValid(File diffFile) throws IOException {
		Scanner scanner = new Scanner(diffFile.toPath());
		String nextLine;
		while (scanner.hasNextLine()) {
			nextLine = scanner.nextLine();
			if (nextLine.equals("=====END OF DIFF=====")) {
				return true;
			}
		}
		return false;
	}
}
