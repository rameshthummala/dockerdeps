package org.docker.hackathon.util;

import java.io.*;
import java.util.*;

/**
 * The FileUtils provides helper functions to do typical filesystem operations
 * like recursive directory copy, recursive directory removal.
 */
public class FileUtils {
	/**
	 * This function will copy files or directories from one location to
	 * another. note that the source and the destination must be mutually
	 * exclusive. This function can not be used to copy a directory to a sub
	 * directory of itself. The function will also have problems if the
	 * destination files already exist.
	 * 
	 * @param src
	 *            -- A File object that represents the source for the copy
	 * @param dest
	 *            -- A File object that represnts the destination for the copy.
	 * @throws IOException
	 *             if unable to copy.
	 */
	public static void dirCopy(File src, File dest) throws IOException {
		// Check to ensure that the source is valid...
		if (!src.exists()) {
			throw new IOException("dirCopy: Can not find source: "
					+ src.getAbsolutePath() + ".");
		} else if (!src.canRead()) { // check to ensure we have rights to the
										// source...
			throw new IOException("dirCopy: No right to source: "
					+ src.getAbsolutePath() + ".");
		}
		// is this a directory copy?
		if (src.isDirectory()) {
			if (!dest.exists()) { // does the destination already exist?
				// if not we need to make it exist if possible (note this is
				// mkdirs not mkdir)
				if (!dest.mkdirs()) {
					throw new IOException(
							"dirCopy: Could not create direcotry: "
									+ dest.getAbsolutePath() + ".");
				}
			}
			// get a listing of files...
			String list[] = src.list();
			// copy all the files in the list.
			for (int i = 0; i < list.length; i++) {
				File dest1 = new File(dest, list[i]);
				File src1 = new File(src, list[i]);
				dirCopy(src1, dest1);
			}
		} else {
			copyFile(src, dest);
		}
	}

	public static void copyFile(File src, File dest) throws IOException {
		// This was not a directory, so lets just copy the file
		FileInputStream fin = null;
		FileOutputStream fout = null;
		byte[] buffer = new byte[4096]; // Buffer 4K at a time (you can change
										// this).
		int bytesRead;
		try {
			// open the files for input and output
			fin = new FileInputStream(src);
			fout = new FileOutputStream(dest);
			// while bytesRead indicates a successful read, lets write...
			while ((bytesRead = fin.read(buffer)) >= 0) {
				fout.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) { // Error copying file...
			IOException wrapper = new IOException(
					"dirCopy: Unable to copy file: " + src.getAbsolutePath()
							+ "to" + dest.getAbsolutePath() + ".");
			wrapper.initCause(e);
			wrapper.setStackTrace(e.getStackTrace());
			throw wrapper;
		} finally { // Ensure that the files are closed (if they were open).
			if (fin != null) {
				fin.close();
			}
			if (fout != null) {
				fout.close();
			}
		}
	}

	// Delete directory
	/**
	 * Delete dir.
	 * 
	 * @param strFile
	 *            the str file
	 * @return true, if successful
	 */
	public static boolean deleteDir(String strFile) {
		// System.out.println("Calling deleteDir with "+strFile);
		// Declare variables variables
		File fDir = new File(strFile);
		String[] strChildren = null;
		boolean bRet = false;

		// Validate directory
		if (fDir.isDirectory()) {
			// -- Get children
			strChildren = fDir.list();

			// -- Go through each
			for (int i = 0; i < strChildren.length; i++) {
				// System.out.println("Traversing "+strChildren[i]);
				bRet = deleteDir(new File(fDir, strChildren[i])
						.getAbsolutePath());
				if (!bRet) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		// System.out.println("Removing "+strFile);
		return fDir.delete();
	}

	public static long getFileSize(String filePath) {
		File file = new File(filePath);
		if (file.exists()) {
			return file.length();
		} else {
			return -1;
		}
	}

	public static HashSet<String> findFiles(File rootDir, String pattern) {
		HashSet<String> matchedFiles = new HashSet<String>();
		if (rootDir == null) { return matchedFiles; }
		if (pattern == null) { return matchedFiles; }
		File[] listOfFiles = rootDir.listFiles();
		if ( listOfFiles == null ) { return matchedFiles; }
		for (int i = 0; i < listOfFiles.length; i++) {
			String iName = listOfFiles[i].getName();
			if (listOfFiles[i].isFile()) {
				if (iName.endsWith(pattern)) {
					matchedFiles.add(rootDir.getPath()+"/"+iName);
				}
			} else if (listOfFiles[i].isDirectory()) {
				matchedFiles.addAll(findFiles(new File(rootDir.getPath()+"/"+listOfFiles[i].getName()), pattern));
			}
		}
		return matchedFiles;
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		// Remove everything from buildout
		System.out.println("Recursively removing "
				+ System.getenv("JIGSAW_SRC_HOME") + "/buildout");
		FileUtils.deleteDir(System.getenv("JIGSAW_SRC_HOME") + "/buildout");
	}

}
