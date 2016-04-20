package com.zchi88.android.libdiff.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Contains methods for hashing files and mapping the files to their hash code.
 * 
 */
public class FileMapper {
	/**
	 * @param file
	 *            - The file to hash
	 * @param algorithm
	 *            - The encryption algorithm to use for hashing
	 * @return The hash value of the file as a String
	 */
	public static String hashFile(File file, String algorithm) {
		try (FileInputStream inputStream = new FileInputStream(file)) {
			MessageDigest digest = MessageDigest.getInstance(algorithm);

			byte[] bytesBuffer = new byte[1024];
			int bytesRead = -1;

			while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
				digest.update(bytesBuffer, 0, bytesRead);
			}

			byte[] hashedBytes = digest.digest();

			return convertByteArrayToHexString(hashedBytes);
		} catch (Exception ex) {
			return "";
		}
	}

	private static String convertByteArrayToHexString(byte[] arrayBytes) {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < arrayBytes.length; i++) {
			stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return stringBuffer.toString();
	}

	/**
	 * Given a directory containing files, returns a hash map where the key is
	 * each file in the directory(and sub-directories) and the value is the
	 * file's hash code as a string.
	 * 
	 * @param directory
	 * @return HashMap<File, String>
	 * @throws IOException
	 */
	public static HashMap<File, String> getFileMap(final Path directory) throws IOException {
		final HashMap<File, String> filesMap = new HashMap<File, String>();
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path dir, BasicFileAttributes attrs) throws IOException {
				if (!Files.isDirectory(dir) && !dir.getFileName().toString().equals("diff.txt")) {
					String checksum = FileMapper.hashFile(dir.toFile(), "MD5");
					// Get the path to a library's source files relative to its
					// decompiled folder for easier comparison
					Path relativeFilePath = directory.relativize(dir);
					filesMap.put(relativeFilePath.toFile(), checksum);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return filesMap;
	}

	/**
	 * Given a directory containing files, returns an arraylist of files that
	 * are in that directory. This is useful for iterating through the hashmap
	 * and modifying the hashmap.
	 * 
	 * @param directory
	 * @return HashMap<File, String>
	 * @throws IOException
	 */
	public static ArrayList<File> getFileList(final Path directory) throws IOException {
		final ArrayList<File> filesList = new ArrayList<File>();
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path dir, BasicFileAttributes attrs) throws IOException {
				if (!Files.isDirectory(dir) && !dir.getFileName().toString().equals("diff.txt")) {
					filesList.add(dir.toFile());
				}
				return FileVisitResult.CONTINUE;
			}
		});

		Collections.sort(filesList);
		return filesList;
	}
}
