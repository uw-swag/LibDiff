package com.zchi88.android.libdiff.versionobject;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.zchi88.android.libdiff.utilities.FileMapper;

/**
 * The library version object represents one version of an Android library. It
 * contains information about that version, such as its name, path, a map of its
 * files to their hash code, files that are exclusive to that version, and files
 * that are unique to that version because they have been modified.
 *
 */
public class LibraryVersion {
	public LibraryVersion(final Path versionPath) throws IOException {
		this.versionPath = versionPath;
		this.versionName = versionPath.getFileName();
		this.filesList = FileMapper.getFileList(versionPath);
		this.exclusiveFiles = new ArrayList<File>();
		this.moddedFiles = new ArrayList<File>();
		this.copiedFiles = new ArrayList<File>();

		this.filesMap = new HashMap<File, String>();
		this.filesList = new ArrayList<File>();
		Files.walkFileTree(versionPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path dir, BasicFileAttributes attrs) throws IOException {
				if (!Files.isDirectory(dir) && !dir.getFileName().toString().endsWith(".txt")) {
					String checksum = FileMapper.hashFile(dir.toFile(), "MD5");
					// Get the path to a library's source files relative to its
					// decompiled folder for easier comparison
					Path relativeFilePath = versionPath.relativize(dir);
					filesMap.put(relativeFilePath.toFile(), checksum);
					filesList.add(relativeFilePath.toFile());
				}
				return FileVisitResult.CONTINUE;
			}
		});
		Collections.sort(filesList);
	}

	// The name of the version of the library
	private final Path versionName;

	// The path to the folder holding the decompiled version source code
	private final Path versionPath;

	// A mapping of all source files in the folder as <File, String> pairs where
	// the string is the hash code of the file
	private final HashMap<File, String> filesMap;

	//
	private ArrayList<File> filesList;

	// A list of files in that version not found in any other version
	private final ArrayList<File> exclusiveFiles;

	// A list of files in that version that exist in other versions, but with
	// different content
	private final ArrayList<File> moddedFiles;
	
	// A list of files in that version that are found as a direct copy in other versions
	private final ArrayList<File> copiedFiles;

	public Path getVersionName() {
		return versionName;
	}

	public Path getVersionPath() {
		return versionPath;
	}

	public HashMap<File, String> getFilesMap() {
		return filesMap;
	}

	public ArrayList<File> getFilesList() {
		return filesList;
	}

	public ArrayList<File> getExclusiveFiles() {
		return exclusiveFiles;
	}

	public ArrayList<File> getModdedFiles() {
		return moddedFiles;
	}
	
	public ArrayList<File> getCopiedFiles() {
		return copiedFiles;
	}
}
