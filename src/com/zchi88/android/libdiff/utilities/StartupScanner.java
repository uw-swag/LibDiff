package com.zchi88.android.libdiff.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class StartupScanner implements Runnable {

	// Synchronized libraries pool shared among all scanner instances
	private ConcurrentLinkedQueue<File> whiteListedLibrariesQueue = null;
	private ExecutorService threadPool = null;

	/**
	 * Scans the whitelist of libraries to see if there are any diffs that need
	 * to re-computed. This makes sure that the libraries and their diffs are
	 * always in sync even if the tool crashes and must be restarted.
	 * 
	 * @throws IOException
	 */
	public static void scan(File[] whitelistedLibraries, int numThreads) throws IOException {
		System.out.println();
		System.out.println("==================================================");
		System.out.println("Scanning for new libraries since last run...");

		// Create a synchronized queue
		ConcurrentLinkedQueue<File> whiteListedLibrariesQueue = new ConcurrentLinkedQueue<File>(Arrays.asList(whitelistedLibraries));

		// Start a threadpool service to increase processing power
		ExecutorService libDiffThreads = Executors.newFixedThreadPool(numThreads);

		if (whitelistedLibraries.length > 0) {

			for (int i = 0; i < numThreads; i++) {
				StartupScanner scanner = new StartupScanner();
				scanner.whiteListedLibrariesQueue = whiteListedLibrariesQueue;
				scanner.threadPool = libDiffThreads;
				libDiffThreads.execute(scanner);
			}

			// Wait for all threads to finish executing before displaying completion
			// message
			try {
				libDiffThreads.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				System.out.println("Libdiff threads execution interrupted.");
			}

			System.out.println("Startup scan complete.");
		} else {
			System.out.println("There were no libraries found at this directory.");
		}
		System.out.println("==================================================\n");
	}


	@Override
	public void run() {

		File library = this.whiteListedLibrariesQueue.poll();

		while (library != null) {

			try {
				if (library != null && library.isDirectory()) {
					Path pathToLibrary = library.toPath();
					AarToJar.convertAarToJar(pathToLibrary);
					JarExtractor.extractAllJars(pathToLibrary);
					DiffComputer.syncDiffs(pathToLibrary);
				}
			}
			catch (IOException e) {
				System.out.println("IOException reading " + library.toString() + ": " + e.getMessage());
			}
			
			library = this.whiteListedLibrariesQueue.poll();
		}
		
		// Signal threadPool that the queue is empty.
		// The threadPool should wait until all threads are done.
		threadPool.shutdown();
	}

}
