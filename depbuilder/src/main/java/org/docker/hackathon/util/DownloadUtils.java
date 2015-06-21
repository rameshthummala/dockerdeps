package org.docker.hackathon.util;

import java.io.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class DownloadUtils {

	public static void downloadURL(String passedURL, String destFile)
			throws IOException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(passedURL);
		HttpResponse response = httpclient.execute(httpget);
		System.out.println(response.getStatusLine());
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			InputStream instream = entity.getContent();
			try {
				BufferedInputStream bis = new BufferedInputStream(instream);
				BufferedOutputStream bos = new BufferedOutputStream(
						new FileOutputStream(new File(destFile)));
				int inByte;
				while ((inByte = bis.read()) != -1) {
					bos.write(inByte);
				}
				bis.close();
				bos.close();
			} catch (IOException ex) {
				throw ex;
			} catch (RuntimeException ex) {
				httpget.abort();
				throw ex;
			} finally {
				instream.close();
			}
			httpclient.getConnectionManager().shutdown();
		}
	}

}
