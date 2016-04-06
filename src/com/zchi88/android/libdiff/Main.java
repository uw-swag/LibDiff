package com.zchi88.android.libdiff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.zchi88.android.libdiff.utilities.StartupScanner;
import com.zchi88.android.librarieswhitelist.librarywatcher.LibraryWatcher;

/**
 * The main class for the Android Library Diff tool. This tool takes a whitelist
 * of android libraries and calculates the changes between consecutive versions
 * of a library, logging them in a diff.txt file.
 * 
 * @author Zhihao Chi
 */
public class Main {

	/**
	 * Display correct usage information for this tool.
	 */
	private static void showHowToUse() {
		System.err.println("Error. One argument(the path to the whitelist library) is expected. Example:");
		System.err.println("java -jar AndroidLibDiff.jar PATH/TO/LIBRARIES/DIRECTORY");
		System.exit(-1);
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			showHowToUse();
		}

		String path_name = args[0];
		Path libDirectory = Paths.get(path_name);

		// Check to make sure that the directory exists.
		File[] whitelistedLibraries = libDirectory.toFile().listFiles();
		if (whitelistedLibraries == null) {
			System.err.println("The specified directory does not exist. Please check that the provided path exists.");
			System.err.println("Exiting program.");
			System.exit(-1);
		}

		System.out.format("Android Library Diff tool started for library whitelist located at %s.\n", libDirectory);

		// Scan the directory upon startup to see if there are libraries that do
		// not have diffs computed for them, and compute them if needed.
		StartupScanner.scan(whitelistedLibraries);

		// Initialize a new library watcher.
		LibraryWatcher watcher = new LibraryWatcher(libDirectory);

		// Have the library watcher monitor and process changes to the library.
		watcher.processEvents();
	}
}