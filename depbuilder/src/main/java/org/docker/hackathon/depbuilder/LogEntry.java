package org.docker.hackathon.depbuilder;

import java.io.Serializable;
import java.util.Vector;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElement.DEFAULT;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "logentry")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class LogEntry implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 2L;
    long revision;
	String author;
	String commitTime;
	Vector <PathEntry> pathEntries;
	String message;
	String projectName;
	// TODO - Add support for running different tests for different projects.
	String testType = "sanity";
	boolean result;
    String testRunTimeStamp;
	
	/**
	 * @return the revision
	 */
    @XmlElement(name = "revision")
	public long getRevision() {
		return revision;
	}
    
	/**
	 * @param revision the revision to set
	 */
	public void setRevision(long revision) {
		this.revision = revision;
	}
	
	/**
	 * @return the author
	 */
    @XmlElement(name = "author")
	public String getAuthor() {
		return author;
	}
    
	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}
	
	/**
	 * @return the commitTime
	 */
    @XmlTransient
	public String getCommitTime() {
		return commitTime;
	}
    
	/**
	 * @param commitTime the commitTime to set
	 */
	public void setCommitTime(String commitTime) {
		this.commitTime = commitTime;
	}
	
	/**
	 * @return the msg
	 */
    @XmlElement(name = "message")
    public String getMessage() {
		return message;
	}
    
	/**
	 * @param msg the msg to set
	 */
    public void setMessage(String message) {
        this.message = message;
	}

 	public LogEntry(String pathElem) {
		pathEntries = new Vector <PathEntry> ();
		pathEntries.add(new PathEntry(pathElem));
	}

	public LogEntry() {
		pathEntries = new Vector <PathEntry> ();
	}
	
    @XmlElement(name = "pathentry", type = DEFAULT.class)
	public Vector <PathEntry> getPathEntries() {
		return pathEntries;
	}
    
	/**
	 * @return the projectName
	 */
    @XmlElement(name = "projectname")
	public String getProjectName() {
		return projectName;
	}
	
	/**
	 * @param projectName the projectName to set
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
	/**
	 * @return the testType
	 */
	@XmlElement(name = "testtype")
	public String getTestType() {
		return testType;
	}
	
	/**
	 * @param testType the testType to set
	 */
	public void setTestType(String testType) {
		this.testType = testType;
	}
	
	/**
	 * @return the result
	 */
	@XmlTransient
	public boolean isResult() {
		return result;
	}
	
	/**
	 * @param result the result to set
	 */
	public void setResult(boolean result) {
		this.result = result;
	}
	
	/**
	 * @return the testRunTimeStamp
	 */
	@XmlTransient
	public String getTestRunTimeStamp() {
		return testRunTimeStamp;
	}
	
	/**
	 * @param testRunTimeStamp the testRunTimeStamp to set
	 */
	public void setTestRunTimeStamp(String testRunTimeStamp) {
		this.testRunTimeStamp = testRunTimeStamp;
	}

}
