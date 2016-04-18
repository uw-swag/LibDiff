package com.zchi88.android.libdiff.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StartupScanner {
	/**
	 * Scans the whitelist of libraries to see if there are any diffs that need
	 * to re-computed. This makes sure that the libraries and their diffs are
	 * always in sync even if the tool crashes and must be restarted.
	 * 
	 * @throws IOException
	 */
	public static void scan(File[] whitelistedLibraries) throws IOException {
		System.out.println();
		System.out.println("==================================================");
		System.out.println("Scanning for new libraries since last run...");

		if (whitelistedLibraries.length > 0) {
			for (File library : whitelistedLibraries) {
				Path pathToLibrary = library.toPath();
				AarToJar.convertAarToJar(pathToLibrary);
				JarDecompiler.decompileAllJars(pathToLibrary);
				DiffComputer.checkDiff(pathToLibrary);
			}
			System.out.println("Startup scan complete.");
		} else {
			System.out.println("There were no libraries found at this directory.");
		}
		System.out.println("==================================================\n");
	}

}
