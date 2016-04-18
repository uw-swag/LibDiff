package com.zchi88.android.libdiff.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;


/**
 * Allows us to hash files using specified encryption algorithms (MD-5, SHA-1, etc)
 * 
 */
public class FileHasher {
	/**
	 * @param file - The file to hash
	 * @param algorithm - The encryption algorithm to use for hashing
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
}
