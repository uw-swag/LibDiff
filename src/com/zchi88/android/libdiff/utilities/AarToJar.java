package com.zchi88.android.libdiff.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AarToJar {
	/**
	 * Searches a library for any AAR files. If found, the JAR file will be
	 * extracted and the AAR file will be removed. This is necessary since
	 * current Java decompilers are designed to decompile JAR files.
	 * 
	 * @throws IOException
	 */
	public static void convertAarToJar(Path library) throws IOException {
		File[] libraryVersions = library.toFile().listFiles();

		if (libraryVersions.length > 0) {
			for (File libFile : libraryVersions) {
				String nameOfLib = libFile.toString();
				if (nameOfLib.endsWith(".aar")) {
					extractJar(nameOfLib);
				}
			}
		}
	}

	/**
	 * Extracts the classes.jar file found in the AAR, and then deletes the AAR.
	 * Note that although this uses the java JarFile class, it works for
	 * unzipping AAR files as well.
	 * 
	 * @param aarPath
	 *            - the String representation of the file path to the AAR
	 * @throws IOException
	 */
	public static void extractJar(String aarPath) throws IOException {
		File pathToAar = new File(aarPath);
		String nameOfAar = pathToAar.getName();
		JarFile aar = new JarFile(aarPath);
		Enumeration<JarEntry> filesInAar = aar.entries();

		System.out.format("AAR file found. Attempting to extract classes.jar from %s...\n", pathToAar);

		while (filesInAar.hasMoreElements()) {
			java.util.jar.JarEntry file = filesInAar.nextElement();

			long sizeTracker = 0;

			if (file.getName().endsWith(".jar")) {
				// Get the largest jar, which should contain most if not all of
				// the library's code
				long fileSize = file.getSize();

				String nameOfJar;
				nameOfJar = nameOfAar.replace(".aar", ".jar");

				File newFile = new File(pathToAar.getParent() + java.io.File.separator + nameOfJar);

				if (fileSize > sizeTracker) {
					sizeTracker = fileSize;
					if (newFile.exists()) {
						newFile.delete();
					}
				}

				InputStream is = aar.getInputStream(file);
				FileOutputStream os = new FileOutputStream(newFile);
				while (is.available() > 0) { // Write contents of InputStream to
												// FileOutputStream
					os.write(is.read());
				}
				os.close();
				is.close();
			}
		}

		aar.close();

		System.out.println("JAR extraction completed!");
		deleteAar(aarPath);
	}

	// Deletes the AAR file
	private static void deleteAar(String aarPath) throws IOException {
		System.out.println("Deleting the AAR file...");
		Files.delete(new File(aarPath).toPath());
		System.out.println("Done!");
	}
}
