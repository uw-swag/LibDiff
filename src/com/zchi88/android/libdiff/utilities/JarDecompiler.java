package com.zchi88.android.libdiff.utilities;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

import com.strobel.decompiler.DecompilerDriver;

/**
 * 
 * This jar decompiler is simply using procyon 5.3.0 to decompile java JAR's.
 * Modify this class if you wish to use a different decompiler.
 *
 */
public class JarDecompiler {

	/**
	 * 
	 * Decompile a single Jar file.
	 *
	 * @param jarPath
	 *            - the path to the jarFile
	 * @param outputPath
	 *            - the directory where the decompiled contents will be placed
	 */
	public static void decompileJar(String jarPath, String outputPath) {
		System.out.println(new File(jarPath).getName() + " has not been decompiled. Decompiling now...");
		final PrintStream showStream = System.out;

		final PrintStream hideStream = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) {
			}
		});
		final String[] arg = { jarPath, "-o", outputPath };

		// The procyon decompiler prints superfluous decompiling information to
		// the console which we don't care about. This hides it.
		System.setOut(hideStream);
		DecompilerDriver.main(arg);
		System.setOut(showStream);
		hideStream.close();
		System.out.println(new File(jarPath).getName() + " has been successfully decompiled.");
	}

	/**
	 * Checks to see if all JARs in a given library have been decompiled. If not, decompile them.
	 * 
	 * 
	 */
	public static void decompileJars(File library) {
		Boolean isDecompiled = true;
		System.out.format("Checking to see if all libary JAR's for %s have been decompiled...\n", library);

		File[] libraryVersions = library.listFiles();

		if (libraryVersions.length > 0) {
			for (File libFile : libraryVersions) {
				String nameOfLib = libFile.toString();
				if (nameOfLib.endsWith(".jar")) {
					// Get the name of the folder where a JAR's decompiled files
					// would go
					String decompiledFolder = nameOfLib.replace(".jar", "");

					// Construct the file path for the decompiled files folder.
					File decompiledFolderPath = new File(decompiledFolder);

					// Check if that folder exists
					if (!decompiledFolderPath.exists()) {
						isDecompiled = false;
						// Decompile the JAR and create this directory if it
						// does not exist
						JarDecompiler.decompileJar(nameOfLib, decompiledFolder);
					}
				}
			}
		}
		if (isDecompiled) {
			System.out.println("Done.");
		}

	}
}
