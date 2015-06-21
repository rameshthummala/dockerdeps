package org.docker.hackathon.depbuilder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElement.DEFAULT;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;


/**
 * The ChangeList class will generate a unique changelist for each commit
 * request through the precommit tool
 */

@XmlRootElement(name = "changelist")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ChangeList implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    private String checkSum;
    private List<LogEntry> logEntries;
    private File changeListFile;
    private String machineName;
    
    public ChangeList () {
        super();
        changeListFile = new File("changelist.xml");
        try {
            if (changeListFile.exists()) {
                changeListFile.delete();
            }
            
            changeListFile.createNewFile();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Set the machine name where the pre-commit is running.
        try {
            setMachineName(InetAddress.getLocalHost().getHostName());
        }
        catch (UnknownHostException uhe) {
            uhe.printStackTrace();
        }
        this.logEntries = new ArrayList<LogEntry>();
    }
    
    public ChangeList (LogEntry logEntry) {
        super();
        this.logEntries = new ArrayList<LogEntry>();
        this.logEntries.add(logEntry);
    }
    
    @XmlElement(name = "logentry", type = DEFAULT.class)
    public List<LogEntry> getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(List<LogEntry> logEntries) {
        this.logEntries = logEntries;
    }
    
    @XmlElement(name = "checksum")
    public String getCheckSum() {
        return checkSum;
    }
    
    public void setCheckSum(String checkSum) {
        this.checkSum = checkSum;
    }
    
    /**
     * @return the changeListFile
     */
    @XmlTransient
    public File getChangeListFile() {
        return changeListFile;
    }
    
    /**
     * @param changeListFile
     *            the changeListFile to set
     */
    public void setChangeListFile(File changeListFile) {
        this.changeListFile = changeListFile;
    }
    
    /**
     * @return the machineName
     */
    @XmlElement(name = "machinename")
    public String getMachineName() {
        return machineName;
    }
    
    /**
     * @param machineName
     *            the machineName to set
     */
    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

}
