package org.docker.hackathon.depbuilder;

/**
 * The Constants class defines the common variables(eg: SUCCESS, FAILURE, NEW_LINE, FILE_SEPARATOR etc) that are used across the mrbuilder code base.
 */
public final class Constants {
	  
  	/** The Constant SUCCESS. */
  	public static final boolean SUCCESS = true;
	  
  	/** The Constant FAILURE. */
  	public static final boolean FAILURE = false;

	  /** The Constant PASSES. */
  	public static final boolean PASSES = true;
	  
  	/** The Constant FAILS. */
  	public static final boolean FAILS = false;

	  /** The Constant NOT_FOUND. */
  	public static final int NOT_FOUND = -1;
		  
	  // line.separator system property
	  /** The Constant NEW_LINE. */
  	public static final String NEW_LINE = System.getProperty("line.separator");
	  // file.separator system property
	  /** The Constant FILE_SEPARATOR. */
  	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	  // path.separator system property
	  /** The Constant PATH_SEPARATOR. */
  	public static final String PATH_SEPARATOR = System.getProperty("path.separator");
		  
	  /** The Constant EMPTY_STRING. */
  	public static final String EMPTY_STRING = "";
	  
  	/** The Constant SPACE. */
  	public static final String SPACE = " ";
	  
  	/** The Constant TAB. */
  	public static final String TAB = "\t";
	  
  	/** The Constant SINGLE_QUOTE. */
  	public static final String SINGLE_QUOTE = "'";
	  
  	/** The Constant PERIOD. */
  	public static final String PERIOD = ".";
	  
  	/** The Constant DOUBLE_QUOTE. */
  	public static final String DOUBLE_QUOTE = "\"";
}
