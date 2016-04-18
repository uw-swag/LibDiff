package com.zchi88.android.libdiff.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * The purpose of this class to to compute diffs between successive versions of
 * a software library.
 */
public class DiffComputer {
	/**
	 * Checks to see if diffs for a library are up to date. If not, it calls on
	 * the computeDiffs() method to re-compute the diffs for the library.
	 * 
	 * This is done by checking that there is a "diff.txt" file corresponding to
	 * each version of the library. Note: The very first version of any library
	 * will never have a diff, since there is nothing to compare to. Therefore,
	 * if n = number of library versions, the number of diff files for a library
	 * will be n-1.
	 * 
	 * @throws IOException
	 */
	public static void checkDiff(Path libraryPath) throws IOException {
		System.out.format("Checking to see if diffs have been computed for all versions of %s...\n", libraryPath);

		File[] libraryVersions = libraryPath.toFile().listFiles();

		if (libraryVersions.length > 0) {
			LinkedList<File> versionOrder = JarComparator.getVersionOrder(libraryPath);
			// If a diff exists for the very first version of a library, delete
			// it.

			if (versionOrder.size() > 0) {
				Files.deleteIfExists(Paths
						.get(versionOrder.get(0).toString().replace(".jar", "") + java.io.File.separator + "diff.txt"));

				int libCount = 0;
				int diffCount = 0;
				for (File libFile : libraryVersions) {
					String nameOfLib = libFile.toString();
					if (nameOfLib.endsWith(".jar")) {
						libCount++;
					}

					if (libFile.isDirectory() && new File(libFile, "diff.txt").exists()) {
						diffCount++;
					}
				}

				if (diffCount < libCount && diffCount != (libCount - 1)) {
					findMissingDiffs(libraryPath);
				} else {
					System.out.println("Diffs for " + libraryPath + " are up to date.\n");
				}
			}

		}
	}

	/**
	 * This method finds any missing diffs for all versions of a library, and
	 * computes them if they are missing.
	 * 
	 * @param libraryVersions
	 *            - list of all the files in a given library
	 * @throws IOException
	 */
	public static void findMissingDiffs(Path libraryPath) throws IOException {
		System.out.format("Diffs for %s appear to be out of date. Recomputing diffs...\n", libraryPath);

		LinkedList<File> versionOrder = JarComparator.getVersionOrder(libraryPath);
		Boolean recomputeDiff = false;// Tracks if previously existing diff
										// should be re-created since a new
										// version is inserted

		// Iterate through the list of versions in backwards order, so that
		// the most recent versions that are missing diffs are found first
		// and re-calculated if missing.
		for (int i = (versionOrder.size() - 1); i > 0; i--) {
			// If the diff.txt file for the file in the sorted versions list
			// is missing, create it.
			String libDiffFolder = versionOrder.get(i).toString().replace(".jar", "");
			File libDiffFilePath = new File(libDiffFolder + java.io.File.separator + "diff.txt");
			if (!libDiffFilePath.exists()) {
				if (recomputeDiff == true) {
					constructDiffs(versionOrder.get(i).toString().replace(".jar", ""),
							versionOrder.get(i + 1).toString().replace(".jar", ""));
					recomputeDiff = false;
				}
				constructDiffs(versionOrder.get(i - 1).toString().replace(".jar", ""),
						versionOrder.get(i).toString().replace(".jar", ""));
			} else {
				// If a diff exists and the previous version's diff does not
				// exist, then we know that the existing diff needs to be
				// re-computed
				recomputeDiff = true;
			}
		}

		System.out.format("Diffs for %s are now up to date.\n\n", libraryPath);
	}

	/**
	 * Constructs the diffs for a target version of a library given its previous
	 * version
	 * 
	 * @param prevVersion
	 *            The String of the file path of the previous version of the
	 *            library
	 * @param targetVersion
	 *            The String of the file path the diff must be computed for
	 * @throws FileNotFoundException
	 */
	private static void constructDiffs(String prev, String target) throws FileNotFoundException {
		File prevVersion = new File(prev);
		File targetVersion = new File(target);

		HashMap<File, String> targetFilesMap = new HashMap<File, String>();

		ArrayList<String> newFiles = new ArrayList<String>();
		ArrayList<String> deletedFiles = new ArrayList<String>();
		ArrayList<String> ModifiedFiles = new ArrayList<String>();

		ArrayList<File> prevFileList = getAllFiles(prevVersion, new ArrayList<File>());
		ArrayList<File> targetFileList = getAllFiles(targetVersion, new ArrayList<File>());

		// Map all the files in the target directory to its checksum
		for (File file : targetFileList) {
			String checksum = FileHasher.hashFile(file, "MD5");
			targetFilesMap.put(file, checksum);
		}

		for (File file : prevFileList) {
			// Construct the expected path of the file in the target version
			// found in the previous version
			String endOfPath = file.toString().replace(prevVersion.toString(), "");
			String compFileName = targetVersion + endOfPath;
			File compFile = new File(compFileName);

			// Check if a checksum exists for the file in the targetFilesMap. If
			// it does not exist, then the file must have been deleted.
			String targetChecksum = targetFilesMap.get(compFile);
			if (targetChecksum == null) {
				deletedFiles.add(compFile.toString());
			} else {
				String prevChecksum = FileHasher.hashFile(file, "MD5");
				// If the checksum exists and is different, then the files have
				// been modified
				if (!targetChecksum.equals(prevChecksum)) {
					ModifiedFiles.add(compFile.toString());
				}
				// Remove files that match in both libraries
				targetFilesMap.remove(compFile);
			}
		}

		// Any files still left in the target directory's map must be files new
		// to the target directory
		for (File key : targetFilesMap.keySet()) {
			newFiles.add(key.toString());
		}

		// Logic for outputing the diff data to a text file
		File libDiffFilePath = new File(targetVersion + java.io.File.separator + "diff.txt");
		try (PrintWriter writer = new PrintWriter(libDiffFilePath)) {
			writer.println("Current version: " + targetVersion.getName());
			writer.println("Previous version: " + prevVersion.getName());
			writer.println();

			writer.println("NEW: " + newFiles.size() + " files");
			writer.println("====================");
			for (String filePath : newFiles) {
				writer.println(filePath);
			}
			writer.println("");

			writer.println("");
			writer.println("DELETED: " + deletedFiles.size() + " files");
			writer.println("====================");
			for (String filePath : deletedFiles) {
				writer.println(filePath);
			}
			writer.println("");

			writer.println("");
			writer.println("MODIFIED: " + ModifiedFiles.size() + " files");
			writer.println("====================");
			for (String filePath : ModifiedFiles) {
				writer.println(filePath);
			}
			writer.println("");
		}
	}

	/**
	 * Given a library, returns a list of all the files in that directory
	 * exlcuding folders
	 * 
	 * @param library
	 * @return
	 */
	private static ArrayList<File> getAllFiles(File library, ArrayList<File> allFiles) {
		File[] filesInLib = library.listFiles();

		for (File file : filesInLib) {
			if (file.isDirectory()) {
				getAllFiles(file, allFiles);
			} else {
				// Exclude generated diff files
				if (!file.getName().equals("diff.txt")) {
					allFiles.add(file);
				}
			}
		}
		return allFiles;
	}
}
