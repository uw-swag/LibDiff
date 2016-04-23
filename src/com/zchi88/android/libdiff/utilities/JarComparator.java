package com.zchi88.android.libdiff.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarComparator {
	/**
	 * Given a library directory containing multiple versions of the library,
	 * computes the release order of the versions by looking at the time of the
	 * most recently updated file in the library.
	 * 
	 * @param library
	 *            - the library file
	 * 
	 * @return A linked list of the different versions of the library as
	 *         Files sorted by their date of release.
	 * 
	 * @throws IOException
	 *
	 */
	public static LinkedList<File> getVersionOrder(Path libraryPath) throws IOException {
		File[] libraryVersions = libraryPath.toFile().listFiles();
		LinkedList<File> sortedVersions = new LinkedList<File>();

		// If the library is not empty
		if (libraryVersions.length > 0) {
			// Iterate through the files in the library
			for (File libFile : libraryVersions) {
				String libName = libFile.toString();
				Boolean isDecompiled = new File(libFile.toString().replace(".jar", "")).exists();
				// If the file is a jar file and has already been decompiled
				if (libName.endsWith(".jar") && isDecompiled) {
					// Insert the library into the linked list in its correct
					// position
					if (sortedVersions.size() == 0) {
						sortedVersions.add(libFile);
					} else {
						int insertIndex = 0;
						while (insertIndex < sortedVersions.size() && getReleaseTime(sortedVersions.get(insertIndex)) > getReleaseTime(libFile)) {
							insertIndex++;
						}
						sortedVersions.add(insertIndex, libFile);
					}
				}
			}
		}
		return sortedVersions;
	}

	/**
	 * 
	 * @param aar
	 *            - the jar file as a File object
	 * @return the time of the most recently modified file in the JAR, which
	 *         will be the estimated release time of the entire JAR
	 * @throws IOException
	 */
	public static long getReleaseTime(File jarPath) throws IOException {
		// Open up the file as a JAR in order to explore its properties
		JarFile jar = new JarFile(jarPath);
		long releaseTime = 0;
		long tempTime;
		Enumeration<JarEntry> filesInJar = jar.entries();

		// Find the most recently updated file, and store its time of
		// modification in "releaseTime"
		while (filesInJar.hasMoreElements()) {
			tempTime = filesInJar.nextElement().getTime();
			if (tempTime > releaseTime) {
				releaseTime = tempTime;
			}
		}
		jar.close();
		return releaseTime;
	}
	
	public static void main(String[] args) throws IOException {
		Path path = Paths.get("C:\\Users\\zhiha\\Desktop\\LibDetector\\LibrariesWhitelist\\facebook");
		System.out.println(getVersionOrder(path));
	}
}
