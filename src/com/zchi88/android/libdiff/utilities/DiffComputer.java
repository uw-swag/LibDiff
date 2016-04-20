package com.zchi88.android.libdiff.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
	 */
	private static Boolean isDiffMissing(File[] libraryVersions) {
		int libCount = 0;
		int diffCount = 0;
		for (File libFile : libraryVersions) {
			String nameOfLib = libFile.toString();
			// Check for number of jar files in the library
			if (nameOfLib.endsWith(".jar")) {
				libCount++;
			}

			// Check for number of diff files in that directory.
			if (libFile.isDirectory() && new File(libFile, "diff.txt").exists()) {
				diffCount++;
			}
		}
		return (diffCount != libCount);
	}

	/**
	 * This method re-computes the diffs for all versions of a library.
	 * 
	 * @param libraryVersions
	 *            - list of all the files in a given library
	 * @throws IOException
	 */
	private static void computeDiffs(File[] libraryVersions) throws IOException {
		// Create an arraylist to temporarily hold the <File, hash code>
		// key/value pairs for analysis
		ArrayList<LibraryVersion> versionsList = new ArrayList<LibraryVersion>();

		// Iterate through each file in a given library. If the file is a
		// directory with a corresponding JAR, we know that it is a directory
		// containing source code decompiled from that jar.
		for (File libFile : libraryVersions) {
			// This checks to see if the file is the folder where a jar has been
			// decompiled to
			if (libFile.isDirectory()) {
				String nameOfFile = libFile.toString();
				String nameOfJar = nameOfFile.concat(".jar");
				File jarFile = new File(nameOfJar);
				// This ensures that there is a jar corresponding to this folder
				if (jarFile.exists()) {
					// Create a library version object to capture the properties
					// of that version
					LibraryVersion libVersion = new LibraryVersion(libFile.toPath());
					// Add the library version object to an arraylist for future
					// computation
					versionsList.add(libVersion);
				}
			}
		}

		// If there is only one version of a library, then all its files must be
		// version exclusive
		if (versionsList.size() == 1) {
			LibraryVersion libVersion = versionsList.get(0);
			ArrayList<File> exclusiveFiles = libVersion.getExclusiveFiles();
			for (File libFile : libVersion.getFilesMap().keySet()) {
				exclusiveFiles.add(libFile);
			}
		} else {
			// For each version's file mapping, compare the mapping to the other
			// versions' files maps. If a file does not exist in any other
			// version, it is version exclusive. If it does exist in other
			// versions but theirhash codes never match, then it exists as a
			// uniquely modified file.

			// Iterate through the file maps and populate the modified files
			// list for each library version
			for (int currentIndex = 0; currentIndex < versionsList.size(); currentIndex++) {
				LibraryVersion currentVersion = versionsList.get(currentIndex);
				HashMap<File, String> currentFileMap = currentVersion.getFilesMap();
				ArrayList<File> currentFilesList = currentVersion.getFilesList();
				ArrayList<File> currentModdedFiles = currentVersion.getModdedFiles();
				ArrayList<File> currentCopiedFiles = currentVersion.getCopiedFiles();

				for (File file : currentFilesList) {
					Boolean isCopied = false;
					Boolean isModded = false;
					String currentHashCode = currentFileMap.get(file);

					if (currentHashCode != null) {
						for (int compIndex = (currentIndex + 1); compIndex < versionsList.size(); compIndex++) {
							LibraryVersion compVersion = versionsList.get(compIndex);
							HashMap<File, String> compFileMap = compVersion.getFilesMap();
							ArrayList<File> compModdedFiles = compVersion.getModdedFiles();
							ArrayList<File> compCopiedFiles = compVersion.getCopiedFiles();
							String compHashCode = compFileMap.get(file);
	
							if (compHashCode != null) {
								if (compHashCode.equals(currentHashCode)) {
									isCopied = true;
									compCopiedFiles.add(file);
									compFileMap.remove(file);
								} else {
									isModded = true;
									compModdedFiles.add(file);
									compFileMap.remove(file);
								}
							}
						}
					}
					if (isCopied) {
						currentCopiedFiles.add(file);
						currentFileMap.remove(file);
						isCopied = false;
					}
					if (isModded) {
						currentModdedFiles.add(file);
						currentFileMap.remove(file);
						isModded = false;
					}
				}
			}

			// Anything still left in the hash map should be version exclusive
			// files
			for (int currentIndex = 0; currentIndex < versionsList.size(); currentIndex++) {
				LibraryVersion currentVersion = versionsList.get(currentIndex);
				HashMap<File, String> currentFileMap = currentVersion.getFilesMap();
				Set<File> filesInMap = currentFileMap.keySet();
				HashSet<File> deepCopyMap = new HashSet<File>(filesInMap);
				ArrayList<File> exclusiveFiles = currentVersion.getExclusiveFiles();

				for (File file : deepCopyMap) {
					exclusiveFiles.add(file);
					currentFileMap.remove(file);
				}
			}
		}

		writeToDiff(versionsList);
	}



	/**
	 * Takes an arraylist of library version objects and writes their diff data to the diff.txt file
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
		System.out.format("Checking to see if diffs have been computed for all versions of %s...\n", libraryPath);
		File[] libraryVersions = libraryPath.toFile().listFiles();

		if (libraryVersions.length > 0) {
			if (isDiffMissing(libraryVersions)) {
				System.out.format("Diffs for '%s' appear to be out of date. Recomputing diffs...\n", libraryPath);
				computeDiffs(libraryVersions);
				System.out.format("Diffs for '%s' are now up to date.\n\n", libraryPath);
			} else {
				System.out.println("Diffs for " + libraryPath + " are up to date.\n");
			}
		}

	}
}
