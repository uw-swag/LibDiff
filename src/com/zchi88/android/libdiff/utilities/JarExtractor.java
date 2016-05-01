package com.zchi88.android.libdiff.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 
 * Contains methods to extract the bytecode (.class files) from a Java library
 * compiled as a JAR
 *
 */
public class JarExtractor {

	/**
	 * 
	 * Extract the bytecode of a single Jar file.
	 *
	 * @param jarPath
	 *            - the path to the jar file
	 * @throws IOException
	 */
	public static void extractByteCode(Path jarPath) throws IOException {
		System.out.println("Extracting all .class files from " + jarPath.getFileName() + "...");

		Path jarExtractionPath = Paths.get(jarPath.toFile().toString().replace(".jar", ""));
		JarFile jar = new JarFile(jarPath.toString());
		Enumeration<JarEntry> filesInJar = jar.entries();

		while (filesInJar.hasMoreElements()) {
			java.util.jar.JarEntry file = filesInJar.nextElement();

			String nameOfFile = file.getName();

			if (nameOfFile.endsWith(".class")) {
				File outputFile = jarExtractionPath.resolve(nameOfFile).toFile();
				File outputDirectory = outputFile.getParentFile();

				if (!outputDirectory.exists()) {
					outputDirectory.mkdirs();
				}

				InputStream is = jar.getInputStream(file);
				FileOutputStream os = new FileOutputStream(outputFile);
				while (is.available() > 0) { // Write contents of InputStream to
												// FileOutputStream
					os.write(is.read());
				}
				os.close();
				is.close();
			}
		}

		jar.close();
	}

	/**
	 * Checks to see if all JARs in a given library have been extracted. If not,
	 * extract them.
	 * 
	 * @throws IOException
	 * 
	 * 
	 */
	public static void extractAllJars(Path libraryPath) throws IOException {
		System.out.format("Checking if bytecode for all JAR's at %s has been extracted...\n", libraryPath);

		File[] libraryVersions = libraryPath.toFile().listFiles();

		if (libraryVersions.length > 0) {
			for (File libFile : libraryVersions) {
				String nameOfLib = libFile.toString();
				if (nameOfLib.endsWith(".jar")) {
					// Get the name of the folder where a JAR's extracted files
					// would go
					String folderName = nameOfLib.replace(".jar", "");

					// Construct the file path for the extracted files folder.
					Path extractionPath = Paths.get(folderName);

					// Check if that folder exists
					if (!extractionPath.toFile().exists()) {
						// Extract the JAR and create this directory if it
						// does not exist
						JarExtractor.extractByteCode(libFile.toPath());
					}
				}
			}
		}
		System.out.println("Done.");
	}

}
