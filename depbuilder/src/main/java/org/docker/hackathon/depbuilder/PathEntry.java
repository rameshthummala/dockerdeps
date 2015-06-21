package org.docker.hackathon.depbuilder;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "pathentry")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class PathEntry implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = 3L;
    String entryType;
	String action;
	String filePath;
	String fileType;
	long fileSize;
	String checkSum;
	
    public PathEntry () {
    }

	/**
	 * @return
	 */
    @XmlElement(name = "filetype")
	public String getFileType() {
		return fileType;
	}
	/**
	 * @param fileType
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	/**
	 * @return
	 */
    @XmlElement(name = "filesize")
	public long getFileSize() {
		return fileSize;
	}
	/**
	 * @param fileSize
	 */
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	/**
	 * @return
	 */
    @XmlElement(name = "checksum")
	public String getCheckSum() {
		return checkSum;
	}
	/**
	 * @param checkSum
	 */
	public void setCheckSum(String checkSum) {
		this.checkSum = checkSum;
	}
	/**
	 * @return the kind
	 */
    @XmlElement(name = "entrytype")
	public String getEntryType() {
		return entryType;
	}
	/**
	 * @param kind the kind to set
	 */
	public void setEntryType(String entryType) {
		this.entryType = entryType;
	}
	/**
	 * @return the action
	 */
    @XmlElement(name = "action")
	public String getAction() {
		return action;
	}
	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
	}
	/**
	 * @return the filePath
	 */
    @XmlElement(name = "filepath")
	public String getFilePath() {
		return filePath;
	}
	/**
	 * @param filePath the filePath to set
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public PathEntry(String path) {
		setFilePath(path);
	}
	
}
