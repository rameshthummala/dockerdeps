package org.docker.hackathon.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class NexusUtils {

    public static String RCNexusRepoLuceneService = "YOURNEXUSSERVER";
    public static void main(String[] args) throws ClientProtocolException, IOException {
	System.out.println("Latest Nexus version for "+args[0]+":"+args[1]+":"+args[2]+" is : "+NexusUtils.getLatestArtifactVersion(args[0],args[1],args[2]));
    }

    public static String getLatestArtifactVersion(String groupId, String artifactId, String versionMatchStr) throws ClientProtocolException, IOException {
	HttpClient client = new DefaultHttpClient();
  	HttpGet request = new HttpGet(RCNexusRepoLuceneService+"&g="+groupId+"&a="+artifactId+"&v="+versionMatchStr+"*");
  	HttpResponse response = client.execute(request);
  	BufferedReader rd = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));
  	String line = "";
  	String latestVersion = "";
  	while ((line = rd.readLine()) != null) {
    	    if (line.contains("<latestRelease>") && line.contains("</latestRelease>")) {
		latestVersion = (line.split("[<>]"))[2];
		return latestVersion;
    	    }
  	}
	return latestVersion;
    }
}
