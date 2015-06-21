package org.docker.hackathon.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.docker.hackathon.depbuilder.ChangeList;
import org.docker.hackathon.depbuilder.LogEntry;
import org.docker.hackathon.depbuilder.PathEntry;
import org.docker.hackathon.depbuilder.SCMFactory;


public class ChangeListUtil {
    
    static Logger logger = Logger.getLogger(ChangeListUtil.class.getName());
    
    public static ChangeList getChangeList(String project, String workspacePath,
            String inputXml) throws Exception {
        
        ChangeList changeList = new ChangeList();
        LogEntry logEntry = new LogEntry();
        StringBuilder unVersionedFileList = new StringBuilder();
        boolean hasUnversionedFiles = false;
        
        // TODO - Add a map for the list of users whose windows login name
        // differs from their kerberos id.
        String userName = System.getProperty("user.name");
        logEntry.setAuthor(userName);
        
        long sourceAreaRevision = SCMFactory.getSourceAreaRevision(workspacePath);
        logEntry.setRevision(sourceAreaRevision);
        
        String currentTime = DateUtils.getCurrentTime();
        logEntry.setCommitTime(currentTime);
        
        currentTime = DateUtils.getCurrentTime();
        logEntry.setTestRunTimeStamp(currentTime);
        
        logEntry.setProjectName(project);
        
        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new ByteArrayInputStream(inputXml.getBytes("utf-8"))));
            
            XPath statusXp = XPathFactory.newInstance().newXPath();
            NodeList changeListNl = (NodeList) (statusXp.evaluate("/status/target/entry",
                    doc, XPathConstants.NODESET));
            for (int i = 0; i < changeListNl.getLength(); i++) {
                
                String filePath = changeListNl.item(i).getAttributes().getNamedItem(
                        "path").getNodeValue();
                Set<String> supportedFileTypes = new HashSet<String>(
                        Arrays.asList(ChangeListUtil.configProperties("changelist.inclusionlist")));
                if (ChangeListUtil.excludedDirectories(filePath.toLowerCase())) {
                    continue;
                }
                if ((supportedFileTypes.contains(filePath.substring(
                        filePath.lastIndexOf(".") + 1).toLowerCase()))) {
                    logger.debug("filepath:" + filePath);
                    String action = changeListNl.item(i).getFirstChild().getAttributes().getNamedItem(
                            "item").getNodeValue();
                    logger.debug("action:" + action);
                    // TODO: Notify user of unversioned file
                    if (action.equalsIgnoreCase("unversioned")) {
                        hasUnversionedFiles = true;
                        unVersionedFileList.append("unversioned file:" + filePath + "\n");
                    }
                    
                    if (action.equalsIgnoreCase("deleted")) {
                        continue;
                    }

                    PathEntry pathEntry = new PathEntry(filePath);
                    pathEntry.setAction(changeListNl.item(i).getFirstChild().getAttributes().getNamedItem(
                            "item").getNodeValue());
                    
                    if (new File(filePath).exists()) {
                        if (new File(filePath).isDirectory()) {
                            pathEntry.setEntryType("directory");
                            logger.debug("entrytype:directory");
                        }
                        else {
                            pathEntry.setEntryType("file");
                            logger.debug("entrytype:file");
                            FileInputStream fis = new FileInputStream(filePath);
                            String checkSum = DigestUtils.shaHex(fis);
                            logger.debug("checksum:" + checkSum);
                            pathEntry.setCheckSum(checkSum);
                            String extension = filePath.substring(
                                    filePath.lastIndexOf(".") + 1, filePath.length());
                            logger.debug("filetype:" + extension);
                            pathEntry.setFileType(extension);
                            long fileSize = FileUtils.getFileSize(filePath);
                            logger.debug("filesize:" + fileSize);
                            pathEntry.setFileSize(fileSize);
                        }
                    }
                    logEntry.getPathEntries().add(pathEntry);
                }
            }
        }
        catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (XPathExpressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (hasUnversionedFiles) {
            Exception unversionedException = new Exception(
                    "Please add the files above to SVN");
            logger.error(unVersionedFileList.toString(), unversionedException);
            throw unversionedException;
        }

        changeList.getLogEntries().add(logEntry);
        return changeList;
    }
    
    public static void saveChangeListXmlToFile(ChangeList changeList) {
        try {
            FileWriter changeListWriter = new FileWriter(
                    changeList.getChangeListFile().getAbsoluteFile());
            BufferedWriter changeListBufferWriter = new BufferedWriter(changeListWriter);
            String changeListFile = XmlUtils.buildXmlString(changeList, ChangeList.class);
            logger.info(changeListFile);
            changeListWriter.write(changeListFile);
            changeListBufferWriter.close();
            FileInputStream clfis = new FileInputStream(changeList.getChangeListFile());
            changeList.setCheckSum(DigestUtils.shaHex(clfis));
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
    public static String[] configProperties(String propertyName) throws IOException {
        InputStream propertyFile = ClassLoader.getSystemClassLoader().getResourceAsStream(
                "config.properties");
        if (propertyFile == null) {
            propertyFile = ChangeListUtil.class.getResourceAsStream("/config.properties");
        }
        Properties configProperties = new Properties();
        configProperties.load(propertyFile);

        return StringUtils.split(configProperties.getProperty(propertyName), ',');
    }
    
    public static boolean excludedDirectories(String filePath) throws IOException {
        String[] excludedDirectories = ChangeListUtil.configProperties("changelist.excluded.directories");
        for (String directory : excludedDirectories) {
            if (filePath.endsWith(directory)) {
                return true;
            }
        }
        return false;
    }
}
