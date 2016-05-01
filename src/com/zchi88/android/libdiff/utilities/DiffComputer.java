package com.zchi88.android.libdiff.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

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
		for (File libFile : libraryVersions) {
			String nameOfLib = libFile.toString();
			// Check if each jar has a diff file created for it
			if (nameOfLib.endsWith(".jar")) {
				File diffFile = new File(nameOfLib.replace(".jar", ""), "diff.txt");
				if (!diffFile.exists()) {
					return true;
				}
	
				if (!isDiffValid(diffFile)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method re-computes the diffs for all versions of a library.
	 * 
	 * @param versionOrder
	 *            - list of all the files in a given library
	 * @throws IOException
	 */
	private static void computeDiffs(LinkedList<File> versionOrder) throws IOException {
		// Iterate through the list of versions in order, so that
		// the most recent versions that are missing diffs are found first
		// and re-calculated if missing.
		for (int versionAge = 0; versionAge < versionOrder.size(); versionAge++) {
			// If the diff.txt file for the file in the sorted versions list
			// is missing, create it.
			File currentVersion = versionOrder.get(versionAge);
			File currentDiffFolder = new File(currentVersion.toString().replace(".jar", ""));
			File previousDiffFolder;
			if ((versionAge + 1) >= versionOrder.size()) {
				previousDiffFolder = null;
			} else {
				previousDiffFolder = new File(versionOrder.get(versionAge + 1).toString().replace(".jar", ""));
			}

			constructDiff(previousDiffFolder, currentDiffFolder, versionAge);
		}
	}

	/**
	 * Constructs the diffs for a the current version of a library given its
	 * previous version
	 * 
	 * @param prevVersion
	 *            The String of the file path of the previous version of the
	 *            library
	 * @param targetVersion
	 *            The String of the file path the diff must be computed for
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static void constructDiff(File previousVersion, File currentVersion, int versionAge) throws IOException {
		Path currentVersionPath = Paths.get(currentVersion.toString().replace(".jar", ""));
		HashMap<File, String> currentFilesMap = FileMapper.getFileMap(currentVersionPath);
		ArrayList<File> prevFileList = new ArrayList<File>();

		if (previousVersion != null) {
			Path previousVersionPath = Paths.get(previousVersion.toString().replace(".jar", ""));
			prevFileList = FileMapper.getFileList(previousVersionPath);
		}

		ArrayList<File> newFiles = new ArrayList<File>();
		ArrayList<File> modifiedFiles = new ArrayList<File>();
		ArrayList<File> deletedFiles = new ArrayList<File>();
		ArrayList<File> copiedFiles = new ArrayList<File>();

		for (File file : prevFileList) {
			File relativeFilePath = previousVersion.toPath().relativize(file.toPath()).toFile();

			// Check if a hash value exists for the file in the targetFilesMap.
			// If
			// it does not exist, then the file must have been deleted.
			String currentHash = currentFilesMap.get(relativeFilePath);

			if (currentHash == null) {
				// If the file does not exist in the current version, it returns
				// null when we try to find it in the hash map
				deletedFiles.add(relativeFilePath);
			} else {
				String previousHash = FileMapper.hashFile(file, "MD5");
				// If the checksum exists and is different, then the files have
				// been modified
				if (currentHash.equals(previousHash)) {
					// If their hash values are the same, the files are copies
					copiedFiles.add(relativeFilePath);
				} else {
					// If their hash values exist and are different, they are
					// modified versions of each other
					modifiedFiles.add(relativeFilePath);
				}
				// Remove the file from the current file map
				currentFilesMap.remove(relativeFilePath);
			}
		}

		// Any files still left in the target directory's map must be files new
		// to the current version
		for (File key : currentFilesMap.keySet()) {
			newFiles.add(key);
		}

		File libDiffFilePath = new File(currentVersion + java.io.File.separator + "diff.txt");

		// Sort the list of files
		Collections.sort(newFiles);
		Collections.sort(deletedFiles);
		Collections.sort(modifiedFiles);
		Collections.sort(copiedFiles);

		// Write the results to diff.txt
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(libDiffFilePath));) {
			writer.write("Showing Diffs For: " + currentVersion.getName());
			writer.newLine();
			writer.write("Version Age: " + versionAge);
			writer.newLine();
			if (previousVersion == null) {
				writer.write("Previous Version Was: N/A");
			} else {
				writer.write("Previous Version Was: " + previousVersion.getName());
			}
			writer.newLine();
			writer.newLine();
			writer.newLine();

			writer.write("New Files: " + newFiles.size() + " files");
			writer.newLine();
			writer.write("====================");
			writer.newLine();
			for (File file : newFiles) {
				writer.write(file.toString());
				writer.newLine();
			}

			writer.newLine();
			writer.newLine();
			writer.write("Modified Files: " + modifiedFiles.size() + " files");
			writer.newLine();
			writer.write("====================");
			writer.newLine();
			for (File file : modifiedFiles) {
				writer.write(file.toString());
				writer.newLine();
			}

			writer.newLine();
			writer.newLine();
			writer.write("Deleted Files: " + deletedFiles.size() + " files");
			writer.newLine();
			writer.write("====================");
			writer.newLine();
			for (File file : deletedFiles) {
				writer.write(file.toString());
				writer.newLine();
			}

			writer.newLine();
			writer.newLine();
			writer.write("Copied Files: " + copiedFiles.size() + " files");
			writer.newLine();
			writer.write("====================");
			writer.newLine();
			for (File file : copiedFiles) {
				writer.write(file.toString());
				writer.newLine();
			}

			writer.newLine();
			writer.newLine();
			writer.write("=====END OF DIFF=====");
		} catch (Exception e) {
			// TODO: handle exception
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
				System.out.format("Diffs for '%s' are now up to date.\n", libraryPath);
				System.out.println("==========\n");
			} else {
				System.out.println("Diffs for " + libraryPath + " are up to date.");
				System.out.println("==========\n");
			}
		}

	}

	/**
	 * Checks to see if a diff was properly created by first ensuring that it
	 * exists, then looking for the "End of diff" marker in the text file.
	 * 
	 * @throws IOException
	 */
	public static Boolean isDiffValid(File diffFile) throws IOException {
		if (diffFile.exists()) {
			@SuppressWarnings("resource")
			Scanner scanner = new Scanner(diffFile.toPath());
			String nextLine;
			while (scanner.hasNextLine()) {
				nextLine = scanner.nextLine();
				if (nextLine.equals("=====END OF DIFF=====")) {
					return true;
				}
			}
		}
		return false;
	}

}
