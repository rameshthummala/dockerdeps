package org.docker.hackathon.depbuilder;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.docker.hackathon.util.ChangeListUtil;
import org.docker.hackathon.util.Executor;
import org.docker.hackathon.util.VersionComparator;
//import org.docker.hackathon.util.DigestUtils;


/**
 * A factory for creating SCM objects and perform SCM(Only SVN right now)
 * operations.
 */
public class SCMFactory {
    
    private static String className = "SCMFactory";
    Level logLevel = Level.toLevel("INFO");
    private static final Log logger = LogFactory.getLog(SCMFactory.class);
    
    /** The svn repo url. */
    private static String svnRepoURL = "YOURSVNSERVER";
    
    /**
     * Gets the sVN repo url.
     * 
     * @return the sVN repo url
     */
    public static String getSVNRepoURL() {
        return svnRepoURL;
    }
    
    public static long getLatestRevision(String project, String branch) {
    	return getRevision(project+"/"+branch);
    }
    
    public static long getSourceAreaRevision(String sourceArea) {
        long revision = -1;
        String methodSig = className + ":" + "getRevision";
        System.out.println("Entering " + methodSig);
        
        String svnLogCommand = "svn  --non-interactive log  -v --stop-on-copy --limit 1 --xml " + sourceArea;
        System.out.println("getRevision : svnLogCommand being invoked is : " + svnLogCommand);
        Executor exec = new Executor();
        List<String> outputLines = exec.runWithOutput(svnLogCommand);
        String commandOutput = "";
        for (String line : outputLines) {
            commandOutput = commandOutput + line;
        }
        System.out.println("getRevision : svnLogCommand " + svnLogCommand + " output is "
                + commandOutput);
        try {
            Document XMLDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new ByteArrayInputStream(
                            commandOutput.getBytes("utf-8"))));
            
            XPath projxp = XPathFactory.newInstance().newXPath();
            NodeList projnl = (NodeList) (projxp.evaluate("/log/logentry", XMLDoc,
                    XPathConstants.NODESET));
            String revisionStr = projnl.item(0).getAttributes().getNamedItem("revision").getNodeValue();
            revision = Long.valueOf(revisionStr);
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
        return revision;
    }
    
    public static String getLatestTag(String project, String version) {
        System.out.println("getLatestTag : Passed project name is :" + project
                + " and version is : " + version);
        String taglistcmd = "svn list " + svnRepoURL + project + "/tags";
        String[] versionParts = Pattern.compile(".", Pattern.LITERAL).split(version);
        System.out.println("getLatestTag : versionParts[1] is : " + versionParts[1]
                + " versionParts[2] is : " + versionParts[2]);
        String majminver = versionParts[0] + "." + versionParts[1];
        String latestTag = "";
        Executor exec = new Executor();
        List<String> outputLines = exec.runWithOutput(taglistcmd);
        if (outputLines.size() > 0) {
            Vector<String> filteredVersionVector = new Vector<String>();
            for (String ver : outputLines) {
                System.out.println("getLatestTag : " + majminver + " Processing : " + ver);
                //Remove the projectname and the hash in the beginning
                ver = ver.replaceAll(project + "-", ""); 
                ver = ver.replaceAll("/", ""); // Remove the training slash
                ver = ver.replaceAll("-", "."); // Replace the last hyphen with a dot
                ver = ver.trim(); // Remove new line at the end , if any
                System.out.println("getLatestTag : " + majminver + " Processing 1 : " + ver);
                if (ver.startsWith(majminver)) {
                    System.out.println("getLatestTag : Adding " + ver
                            + " to the filteredVersionVector.");
                    filteredVersionVector.add(ver);
                }
            }
            for (String ver : filteredVersionVector) {
                System.out.println("getLatestTag : filteredVersionVector : Before : " + ver);
            }
            VersionComparator vc = new VersionComparator();
            Collections.sort(filteredVersionVector, vc);
            for (String ver : filteredVersionVector) {
                System.out.println("getLatestTag : filteredVersionVector : After : " + ver);
            }
            if (filteredVersionVector.size() > 0) {
                String[] newVersionParts = Pattern.compile(".", Pattern.LITERAL).split(
                        filteredVersionVector.get(filteredVersionVector.size() - 1));
                latestTag = newVersionParts[0] + "." + newVersionParts[1] + "."
                        + newVersionParts[2] + "-" + newVersionParts[3];
            }
        }
        return latestTag;
    }
    
    public static boolean exists(String url) {
        boolean retval = true;
        String methodSig = className + ":" + "exists";
        String urlToUse = url;
        System.out.println("Entering " + methodSig);
        if (!urlToUse.contains(svnRepoURL)) {
            // Prefix the SVNRepoURL in the front
            urlToUse = svnRepoURL + "/" + urlToUse;
        }
        String svnListCmd = "svn list " + urlToUse;
        System.out.println("exists : Command being invoked is : " + svnListCmd);
        Executor exec = new Executor();
        List<String> outputLines = exec.runWithOutput(svnListCmd);
        System.out.println("exists : " + svnListCmd + " Command output is : ");
        for (String line : outputLines) {
            System.out.println("\t" + line);
        }
        if (outputLines.contains("non-existent in")) {
            System.out.println(urlToUse + " does not exist in SCM.");
            retval = false;
        }
        else {
            System.out.println(urlToUse + " exists in SCM.");
        }
        System.out.println("Exiting " + methodSig);
        return retval;
    }
    
    public static boolean isChanged(String oldURL, String newURL) {
        boolean retval = true;
        String methodSig = className + ":" + "codeChanged";
        System.out.println("Entering " + methodSig);
        if (!oldURL.contains(svnRepoURL)) {
            oldURL = svnRepoURL + "/" + oldURL;
        }
        if (!newURL.contains(svnRepoURL)) {
            newURL = svnRepoURL + "/" + newURL;
        }
        if (newURL.equals(oldURL)) {
            System.out.println("The " + oldURL + " and " + newURL + " are the same. No changes.");
            return false;
        }
        if (!exists(oldURL)) {
            System.out.println(oldURL + " does not exist in SCM.");
            return false;
        }
        if (!exists(newURL)) {
            System.out.println(oldURL + " does not exist in SCM.");
            return false;
        }
        long oldRevision = getRevision(oldURL);
        long newRevision = getRevision(newURL);
        
        if (oldRevision == -1) {
            System.out.println("Could not get revision number for " + oldURL);
        }
        if (newRevision == -1) {
            System.out.println("Could not get revision number for " + newURL);
        }
        
        if (oldRevision > newRevision) {
            // Could be true only if last operation was a tag creation
            return false;
        }
        long oldRevisionToConsider = oldRevision + 1;
        
        String svnLogCmd = "svn --non-interactive log -v -r" + oldRevisionToConsider
                + ":" + newRevision + " " + newURL + " --xml";
        System.out.println("isChanged : svnlogcommand being used for diffing revision :"
                + oldRevisionToConsider + " and revision: " + newRevision + " is : "
                + svnLogCmd);
        Executor exec = new Executor();
        List<String> outputLines = exec.runWithOutput(svnLogCmd);
        String commandOutput = "";
        for (String line : outputLines) {
            commandOutput = commandOutput + line;
        }
        Vector<String> filters = new Vector<String>();
        filters.add("src/test");
        Vector<LogEntry> changedPaths = SCMFactory.getChangedPaths(commandOutput, filters);
        if (changedPaths.size() > 0) {
            System.out.println("The following code has changed from " + oldRevision + " to "
                    + newRevision + " in SVN:");
            for (int logiter = 0; logiter < changedPaths.size(); logiter++) {
                LogEntry le = changedPaths.elementAt(logiter);
                Vector<PathEntry> pathEntries = le.getPathEntries();
                for (int pathiter = 0; pathiter < pathEntries.size(); pathiter++) {
                    System.out.println("\t" + pathEntries.elementAt(pathiter).getAction() + " "
                            + pathEntries.elementAt(pathiter).getFilePath());
                }
            }
        }
        else {
            System.out.println("Code has NOT changed from " + oldRevision + " to " + newRevision
                    + " in SVN.");
            retval = false;
        }
        System.out.println("Exiting " + methodSig);
        return retval;
    }
    
    
    public static Vector<LogEntry> getChangedPaths(String buffer, Vector<String> filters) {
        String methodSig = className + ":" + "getChangedPaths";
        Vector<LogEntry> changeList = new Vector<LogEntry>();
        System.out.println("Entering " + methodSig);
        System.out.println("getChangedPaths : incoming buffer is :" + buffer);
        try {
            Document XMLDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new ByteArrayInputStream(buffer.getBytes("utf-8"))));
            
            XPath projxp = XPathFactory.newInstance().newXPath();
            NodeList pathnl = (NodeList) (projxp.evaluate("/log/logentry/paths/path",
                    XMLDoc, XPathConstants.NODESET));
            for (int iter = 0; iter < pathnl.getLength(); iter++) {
                Node nd = pathnl.item(iter);
                String pathElem = nd.getTextContent();
                // version.sh update is a vestige of older master_build.sh
                // For time being, we will ignore this change since this is not
                // genuine developer change.
                // Once builder is fully rolled out, there will not be any more
                // version.sh updates
                if (!pathElem.contains("buildenv/version.sh")) {
                    System.out.println("Adding " + pathElem + " to the list of changed files.");
                    changeList.add(new LogEntry(pathElem));
                }
            }
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
        return changeList;
    }
    
    public static long getRevision(String url) {
        long revision = -1;
        String methodSig = className + ":" + "getRevision";
        System.out.println("Entering " + methodSig);
        if (!url.contains(svnRepoURL)) {
            url = svnRepoURL + "/" + url;
        }
        String svnLogCommand = "svn  --non-interactive log  -v --stop-on-copy --limit 1 --xml " + url;
        System.out.println("getRevision : svnLogCommand being invoked is : " + svnLogCommand);
        Executor exec = new Executor();
        List<String> outputLines = exec.runWithOutput(svnLogCommand);
        String commandOutput = "";
        for (String line : outputLines) {
            commandOutput = commandOutput + line;
        }
        System.out.println("getRevision : svnLogCommand " + svnLogCommand + " output is "
                + commandOutput);
        try {
            Document XMLDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new ByteArrayInputStream(
                            commandOutput.getBytes("utf-8"))));
            
            XPath projxp = XPathFactory.newInstance().newXPath();
            NodeList projnl = (NodeList) (projxp.evaluate("/log/logentry", XMLDoc,
                    XPathConstants.NODESET));
            String revisionStr = projnl.item(0).getAttributes().getNamedItem("revision").getNodeValue();
            revision = Long.valueOf(revisionStr);
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
        return revision;
    }
    
    public static ChangeList getChangeList(String project, String projectSourcePath, String oldURL, String newURL) throws Exception {
        ChangeList chglst = new ChangeList();
        String methodSig = className + ":" + "getChangeList";
        System.out.println("Entering " + methodSig);
        if (!oldURL.contains(svnRepoURL)) {
            oldURL = svnRepoURL + "/" + oldURL;
        }
        if (!newURL.contains(svnRepoURL)) {
            newURL = svnRepoURL + "/" + newURL;
        }
        if (newURL.equals(oldURL)) {
            System.out.println("The " + oldURL + " and " + newURL + " are the same. No changes.");
            return chglst;
        }
        if (!exists(oldURL)) {
            System.out.println(oldURL + " does not exist in SCM.");
            return chglst;
        }
        if (!exists(newURL)) {
            System.out.println(oldURL + " does not exist in SCM.");
            return chglst;
        }
        long oldRevision = getRevision(oldURL);
        long newRevision = getRevision(newURL);
        
        if (oldRevision == -1) {
            System.out.println("Could not get revision number for " + oldURL);
        }
        if (newRevision == -1) {
            System.out.println("Could not get revision number for " + newURL);
        }
        
        if (oldRevision > newRevision) {
            // Could be true only if last operation was a tag creation
            return chglst;
        }
        long oldRevisionToConsider = oldRevision + 1;
        
        String svnLogCmd = "svn --non-interactive log -v -r" + oldRevisionToConsider
                + ":" + newRevision + " " + newURL + " --xml";
        System.out.println("isChanged : svnlogcommand being used for diffing revision :"
                + oldRevisionToConsider + " and revision: " + newRevision + " is : "
                + svnLogCmd);
        Executor exec = new Executor();
        List<String> outputLines = exec.runWithOutput(svnLogCmd);
        String commandOutput = "";
        for (String line : outputLines) {
            commandOutput = commandOutput + line;
        }
        
        return ChangeListUtil.getChangeList(project, projectSourcePath, commandOutput);
    }
    
    public static ChangeList getChangeList(String project, String workspacePath)
            throws Exception {
        String methodSig = className + ":" + "getStatus";
        String projectSourcePath = workspacePath + "/" + project;
        System.out.println("Entering " + methodSig);
        try {
            if (!new File(projectSourcePath).exists()) {
                throw new FileNotFoundException(projectSourcePath + " does not exist");
            }
        }
        catch (FileNotFoundException exc) {
            
            exc.printStackTrace();
        }
        String svnCommand = "svn status --xml " + projectSourcePath;
        System.out.println("getStatus : svnCommand being invoked is : " + svnCommand);
        Executor exec = new Executor();
        List<String> outputLines = exec.runWithOutput(svnCommand);
        String commandOutput = "";
        for (String line : outputLines) {
            commandOutput = commandOutput + line;
        }
        System.out.println("getStatus : svnCommand " + svnCommand + " output is "
                + commandOutput);

        return ChangeListUtil.getChangeList(project, projectSourcePath, commandOutput);
    }
    
    public static boolean commitChangeList(String projectSourceHome, String project, String gusId, String reviewer, ChangeList passedChangeList) {
    	boolean retval = Constants.SUCCESS;
    	try {
			String commitLog = "GusID Description: " + gusId + "\n";
			commitLog = commitLog + "Reviewer: "+reviewer+"\n";
			commitLog = commitLog + "Checksum : "+ passedChangeList.getCheckSum() + "\n";
			commitLog = commitLog + "CommitAgent : Precheckin tool.\n";
			File commentFile = new File("commitcomments.txt");
			if (!commentFile.exists()) {
				commentFile.createNewFile();
			}
			FileWriter commentWriter = new FileWriter(commentFile.getAbsoluteFile());
			BufferedWriter commentBufferWriter = new BufferedWriter(commentWriter);
			commentBufferWriter.write(commitLog);
			commentBufferWriter.close();

			String commitCmd = "svn commit --file commitcomments.txt " + projectSourceHome + "/"+ project;
			System.out.println("SCMFactory::commitChangeList : Committing code.");
			Executor exec = new Executor();
			exec.setLogLevel(Level.toLevel("DEBUG"));
			exec.setShowOutput(true);
			retval = exec.run(commitCmd);
			System.out.println("SCMFactory::commitChangeList : Committing code completed with "+ retval);
    	} catch(Exception e) {
			e.printStackTrace();
			return Constants.FAILURE;
		}
		return retval;
    }
    
    /**
     * The main method.
     * 
     * @param args
     *            the arguments
     */
    public static void main(String[] args) {
        // checkoutCode("api", SCMFactory.getSVNRepoURL() + "/api/trunk",
        // "trunk",
        // true, true);
        // long ret = getLatestRevision("api", "trunk");
        System.out.println("Latest tag for web is " + getLatestTag("jdp", "186.0.35-1"));
    }
}
