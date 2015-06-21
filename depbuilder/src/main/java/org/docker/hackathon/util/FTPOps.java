package org.docker.hackathon.util;

import java.io.*;
import org.apache.commons.net.ftp.FTPClient;

public class FTPOps {
	
	// Placeholder code that Jyoti implemented. Moving to a separate class for future use.
	
	// Write to FTP
	private String writeToFTPServer(String XML) {

		FTPClient client = new FTPClient();
		FileInputStream fis = null;
		String filename = "";
		try {
			client.connect("ftp.domain.com");
			client.login("admin", "secret");

			// Create an InputStream of the file to be uploaded
			// ?????????? Filename needs to be flushed out
			// a possible solution is to use the project name, test name,
			// build number and time stamp jdp_unit_186.02.xx_<TS>
			// beginning time stamp would be preferred

			filename = "project" + ".xml";
			fis = new FileInputStream(filename);

			// Store file to server
			client.storeFile(filename, fis);
			client.logout();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (fis != null) {
					fis.close();
				}
				client.disconnect();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return filename;
	}

}
