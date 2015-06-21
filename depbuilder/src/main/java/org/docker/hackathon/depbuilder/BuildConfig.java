package org.docker.hackathon.depbuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * BuildConfig Class contains all the user configurable variables and their getters/setters. Also, contains code for parsing the arguments passed from command line (or from pom.xml in case of plugins) and setup the BuildConfig Object for later use by the builder.
 */
public class BuildConfig {

	/** The branch */
	String branch = null;
	
	/** The projects xml. */
	String projectsXML;
	
	/** The start comp. */
	String startComp;
	
	/** The gen javadoc. */
	boolean genJavadoc = false;
	
	/** The gen coverage. */
	boolean genCoverage = false;
	
	/** The run site. */
	boolean runSite = false;
	
	/** The skip dependees. */
	boolean skipDependees = false;
	
	/** The skip clean. */
	boolean skipClean = false;
	
	/** The build site. */
	boolean buildSite = false;
	
	/** The src home. */
	String sourceHome;
	
	/** The run mode: allowed types: developer,qe*/
	String runMode = "developer";
	
	/** The build type. */
	String buildType = "regular";
	
	/** The build flow. */
	String buildFlow = "serial";
	
	/** The build mode. */
	String buildMode = "all";
	
	/** The activity. */
	String activity = "build";
	
	/** The build location. */
	String buildLocation = "local";
	
	/** The dep traversal. */
	String depTraversal = "bottomup";
	
	boolean skipTests = true;
	
	boolean abortIfNoChanges = true;

	/** The logger. */
	static Logger logger = null;

	/** The log level. The default log level will be INFO. */
	Level logLevel = null;
	
	String buildArena = "inplace";

	/** The custom projects. */
	ArrayList<String> customProjects = new ArrayList<String>();
	
	/** The custom targets. */
	ArrayList<String> customTargets  = new ArrayList<String>();

	public void setLogLocation()  {
		// See if depbuilder.build.log property is set.
                // If not, set it to proper value.
                if ( (System.getProperty("depbuilder.build.log") == null ) ||
                         (System.getProperty("depbuilder.build.log").equals("null")) ||
                         (System.getProperty("depbuilder.build.log").equals(""))) {
                        System.setProperty("depbuilder.build.log", getSourceHome()+"/depbuilder.log");
                }
                // System.out.println("Value of depbuilder.build.log property is : "+System.getProperty("depbuilder.build.log"));
		logger = Logger.getLogger(BuildConfig.class.getName());
		logLevel = Level.toLevel("INFO");
	}
	
	/**
	 * Check prerequisites.
	 */
	public static void checkPrerequisites() {
		if(System.getenv("MAVEN_HOME") == null) {
			logger.error("\nERROR : MAVEN_HOME environment variable need to point to a valid maven installation and <MAVEN_HOME>/bin should be in PATH environment variable. Exiting.\n");
			System.exit(1);
		}
		if(System.getenv("JAVA_HOME") == null) {
			logger.error("\nERROR : JAVA_HOME environment variable need to point to a valid JDK 1.6 (or later) installation and <JAVA_HOME>/bin should be in the PATH environment variable. Exiting.\n");
			System.exit(1);
		}
		String SystemPATH = System.getenv("PATH");
		boolean locatedSVNBinary = false;
		String [] pathParts;
		String osName = System.getProperty("os.name");
		if (osName.contains("Windows")) {
			pathParts = Pattern.compile(";", Pattern.LITERAL).split(SystemPATH);
		} else {
			pathParts = Pattern.compile(":", Pattern.LITERAL).split(SystemPATH);
		}
		for (String part : pathParts) {
			String svnbin = part+"/svn";
			if (osName.contains("Windows")) {
				svnbin = part+"\\svn.exe";
			}
			File partFileObj = new File(svnbin);
			if (partFileObj.exists()) {
				locatedSVNBinary = true;
				//logger.info("Found SVN Binary at : "+svnbin);
			}
		}
		if(locatedSVNBinary == false) {
			logger.error("\nERROR : Could not locate SVN binary in PATH env variable. It should be in the PATH environment variable.\n");
			logger.error("On Windows, the SlikSVN (http://www.sliksvn.com/) package need to be installed for standalone SVN binary to be available.");
			logger.error("On Linux, the subversion rpms need to be installed for the SVN binary to be available.");
			logger.error("Exiting. Please install relevant Subversion package and retry the installer.\n");
			System.exit(1);
		}
	}

	public boolean enforceConstraints() {
		boolean retval = Constants.SUCCESS;
		if(sourceHome == null || sourceHome.equals("")) {
			logger.error("ERROR : Proper source home location is not set or passed through the sourceHome command line option).");
			logger.error("Builder needs a proper source home to do its build. Exiting.\n");
			System.exit(1);
		}
                File sourceDirEntry = new File(sourceHome);
                if(!sourceDirEntry.exists()) {
                        System.out.println("ERROR : Passed sourceHome parameter "+sourceHome+" seems to be invalid : "+getSourceHome());
                        System.out.println("Please set it to a valid value and rerun the plugin.");
                        System.exit(1);
                }
		if(getBranch() == null) {
			// TODO - Add logic to validate whether a passed branch is a real branch in SVN
			logger.error("ERROR : Proper branch has not been passed : "+getBranch());
			logger.error("Builder needs a proper branch information to do its build. Exiting.");
			System.exit(1);
		}
		if ( (!getRunMode().equals("developer")) && (!getRunMode().equals("qe")) ) {
			logger.error("ERROR : Proper runMode parameter has not been passed : "+getRunMode());
			logger.error("Allowed values for runMode are : developer or qe");
			logger.error("Builder needs a proper runMode information to do its build. Exiting.");
			System.exit(1);
		}
		if ( (!getBuildFlow().equals("serial")) && (!getBuildFlow().equals("parallel")) ) {
			logger.error("ERROR : Proper buildFlow parameter has not been passed : "+getBuildFlow());
			logger.error("Allowed values for buildFlow are : serial or parallel");
			logger.error("Builder needs a proper buildFlow information to do its build. Exiting.");
			System.exit(1);
		}
		if ( (!getBuildMode().equals("all")) && (!getBuildMode().equals("incr")) ) {
			logger.error("ERROR : Proper buildMode parameter has not been passed : "+getBuildMode());
			logger.error("Allowed values for buildMode are : all or incr");
			logger.error("Builder needs a proper buildMode information to do its build. Exiting.");
			System.exit(1);
		}
		if ( (!getBuildLocation().equals("local")) && (!getBuildMode().equals("jenkins")) ) {
			logger.error("ERROR : Proper buildLocation parameter has not been passed : "+getBuildLocation());
			logger.error("Allowed values for buildLocation are : local or jenkins");
			logger.error("Builder needs a proper buildLocation information to do its build. Exiting.");
			System.exit(1);
		}
		if ( (!getDepTraversal().equals("bottomup")) && (!getDepTraversal().equals("topdown")) ) {
			logger.error("ERROR : Proper depTraversal parameter has not been passed  : "+ getDepTraversal());
			logger.error("Allowed values for depTraversal are : bottomup or topdown");
			logger.error("Builder needs a proper depTraversal information to do its build. Exiting.");
			System.exit(1);
		}
		// Check if the topdown dependency traversal mode is passed. 
		// If yes, check if the customProjects vector is empty.
		if ( (getDepTraversal().equals("topdown") && (customProjects.size() == 0)) ) {
			logger.error("ERROR : Dependency traversal mode : "+ getDepTraversal()+" is passed.");
			logger.error("But the custom project list is empty. Please pass a valid custom projects.");
			System.exit(1);
		}
		if ( 
			(!getLogLevel().equals(Level.toLevel("OFF"))) && (!getLogLevel().equals(Level.toLevel("FATAL"))) &&
			(!getLogLevel().equals(Level.toLevel("ERROR"))) && (!getLogLevel().equals(Level.toLevel("WARN"))) &&
			(!getLogLevel().equals(Level.toLevel("INFO"))) && (!getLogLevel().equals(Level.toLevel("DEBUG"))) &&
			(!getLogLevel().equals(Level.toLevel("TRACE"))) && (!getLogLevel().equals(Level.toLevel("ALL"))) ) {
			logger.error("ERROR : Proper logLevel parameter has not been passed : "+getLogLevel());
			logger.error("Allowed values for logLevel are one of : OFF,FATAL,ERROR,WARN,INFO,DEBUG,TRACE,ALL");
			logger.error("Builder needs a proper logLevel information to do its build. Exiting.");
			System.exit(1);
		}
		//if(customTargets.size() == 0) {
			// No targets passed. Use Install as the default target.
			//customTargets.add(new String("install"));
		//}
                if ( (getRunMode().equals("qe")) ) {
                        String userHome;
                        String osName = System.getProperty("os.name");
                        if (osName.contains("Windows")) {
                                String userHomeDrive = System.getenv("HOMEDRIVE");
                                String userHomePath = System.getenv("HOMEPATH");
                                userHome = userHomeDrive+userHomePath;
                        } else {
                                userHome = System.getenv("HOME");
                        }
			File credFile = new File(userHome+"/builddb_cred.txt");
			if (!credFile.exists() ) {
                        	logger.error("ERROR : BuildDB credentials file is not found while doing build in : "+getRunMode()+" mode.");
                        	logger.error("ERROR : Please setup a proper builddb_cred.txt file and rerun the builder.");
                        	System.exit(1);
			}
                }

		return retval;
	}
	

	/**
	 * Parses the passed arguments.
	 *
	 * @param args - The arguments that are passed from command line
	 */
	public void parseArgs(String[] args) {
		checkPrerequisites();
		for (int iter = 0; iter < args.length; iter++) {
			if ((args[iter].startsWith("-srchome"))
					|| (args[iter].startsWith("--srchome"))) {
				setSourceHome(args[iter + 1]);
				iter++;
			} else if ((args[iter].startsWith("-branch"))
					|| (args[iter].startsWith("--branch"))) {
				// Required? : Yes - Required
				// Allowed Values : A contigous string of characters without spaces.
				setBranch(args[iter + 1]);
				iter++;	
			} else if ((args[iter].startsWith("-manifest"))
					|| (args[iter].startsWith("--manifest"))) {
				// Required? : No - Optional
				// Allowed Values : An top level xml file - Default : $SRC_HOME/manifest/manifest.xml
				setProjectsXML(args[iter + 1]);
				iter++;
			} else if ((args[iter].startsWith("-runmode"))
					|| (args[iter].startsWith("--runmode")))
			// Required? : No - Optional
			// Allowed Values : [dev|qe|jenkins] - Default : dev
			{
				setRunMode(args[iter + 1]);
				iter++;
			} else if ((args[iter].startsWith("-javadoc"))
					|| (args[iter].startsWith("--javadoc")))
			// Required? : No - Optional
			// Allowed Values : [true|false] - Default : false
			{
				setGenJavadoc(true);
			} else if ((args[iter].startsWith("-codecoverage"))
					|| (args[iter].startsWith("--codecoverage")))
			// Required? : No - Optional
			// Allowed Values : [true|false] - Default : false
			{
				setGenCoverage(true);
			} else if ((args[iter].startsWith("-buildtype"))
					|| (args[iter].startsWith("--buildtype")))
			// Required? : No - Optional
			// Allowed Values : [regular|erelease] - Default : regular
			// If the buildType is regular, the bottomup dependency traversal is used.
			// If the buildType is erelease, the topdown dependency traversal is used.
			{
				setBuildType(args[iter + 1]);
				iter++;
			} else if ((args[iter].startsWith("-buildflow"))
					|| (args[iter].startsWith("--buildflow")))
			// Required? : No - Optional
			// Allowed Values : [serial|parallel] - Default : serial
			{
				setBuildFlow(args[iter + 1]);
				iter++;
			} else if ((args[iter].startsWith("-buildmode"))
					|| (args[iter].startsWith("--buildmode")))
			// Required? : No - Optional
			// Allowed Values : [all|incr] - Default : all
			{
				setBuildMode(args[iter + 1]);
				iter++;
			} else if ((args[iter].startsWith("-buildlocation"))
					|| (args[iter].startsWith("--buildlocation")))
			// Required? : No - Optional
			// Allowed Values : [local|localremote|farm|jenkins] - Default : local
			{
				setBuildLocation(args[iter + 1]);
				iter++;
			} else if ((args[iter].startsWith("-skipdependees"))
					|| (args[iter].startsWith("--skipdependees")))
			// Required? : No - Optional
			// Allowed Values : [true|false]  - Default : false
			{
				setSkipDependees(true);
				iter++;
			} else if ((args[iter].startsWith("-skipclean"))
					|| (args[iter].startsWith("--skipclean")))
			// Required? : No - Optional
			// Allowed Values : [true|false]  - Default : false
			{
				setSkipClean(true);
				iter++;
			} else if ((args[iter].startsWith("-deptrav"))
					|| (args[iter].startsWith("--deptrav")))
			// Required? : No - Optional
			// Allowed Values : [bottomup|topdown]  - Default : bottomup
			{
				setDepTraversal(args[iter + 1]);
				iter++;
			} else if ((args[iter].startsWith("-mvntargets"))
					|| (args[iter].startsWith("--mvntargets")))
			// Required? : No - Optional
			// Allowed Values : A sequence of strings - Default : install
			{
				String [] targets = args[iter + 1].split(",");
				for (int i=0 ; i<targets.length;i++) {
					customTargets.add(targets[i]);
				}
				iter++;
			} else if ((args[iter].startsWith("-projects"))
					|| (args[iter].startsWith("--projects")))
			// Required? : No - Optional
			// Allowed Values : A sequence of strings - Default : install
			{
				String [] projects = args[iter + 1].split(",");
				for (int i=0 ; i<projects.length;i++) {
					customProjects.add(projects[i]);
				}
				iter++;
			} else if ((args[iter].startsWith("-help"))
					|| (args[iter].startsWith("--help"))
					|| (args[iter].startsWith("-h"))
					|| (args[iter].startsWith("--h"))
					|| (args[iter].startsWith("help"))
					|| (args[iter].startsWith("-usage"))
					|| (args[iter].startsWith("--usage"))
					|| (args[iter].startsWith("usage"))) {
				showUsage();
			} else if ((args[iter].startsWith("-verbose"))
					|| (args[iter].startsWith("--verbose"))
					|| (args[iter].startsWith("-v"))
					|| (args[iter].startsWith("--v"))) {
				setLogLevel(Level.toLevel("TRACE"));
			} else if ((args[iter].startsWith("-extremeverbose"))
					|| (args[iter].startsWith("-V"))
					|| (args[iter].startsWith("--extremeverbose"))
					|| (args[iter].startsWith("--V"))) {
				setLogLevel(Level.toLevel("ALL"));
			} else if ((args[iter].startsWith("-loglevel"))
					|| (args[iter].startsWith("--loglevel")))
			// Allowed values: Standard log4j loglevel types(OFF,FATAL,ERROR,WARN,INFO,DEBUG,TRACE,ALL). - Default : INFO
			// Refer to URL for log4j loglevel types:
			// http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Level.html
			{
				setLogLevel(Level.toLevel(args[iter + 1]));
				iter++;
			} else {
				logger.error("Unrecognized option " + args[iter] + " passed. \n");
				showUsage();
			}
		}

	}

	/**
	 * Prints the config.
	 */
	public void printConfig() {
		System.out.println("####################################################################################");
		System.out.println("########################## BUILD OPTIONS USED FOR THIS BUILD #######################");
		System.out.println("####################################################################################");
		System.out.println("#               Top Level Build         : " + getProjectsXML());
		System.out.println("#               Coverage Build          : " + isGenCoverage());
		System.out.println("#               Javadoc Build           : " + isGenJavadoc());
		System.out.println("#               Run Mode                : " + getRunMode());
		System.out.println("#               Build Type              : " + getBuildType());
		System.out.println("#               Build Flow              : " + getBuildFlow());
		System.out.println("#               Build Mode              : " + getBuildMode());
		System.out.println("#               Build Location          : " + getBuildLocation());
		System.out.println("#               AbortIfNoChanges        : " + isAbortIfNoChanges());
		if(customProjects.size() == 0) {
			System.out.println("#               Projects being built    : all    ");
		} else {
			String projectstr = "";
			for (int i=0 ; i<customProjects.size();i++) {
				projectstr = projectstr+","+customProjects.get(i);
			}
			System.out.println("#               Projects being built    : ");
		}
		System.out.println("#               Skip Dependee Build     : " + skipDependees);
		System.out.println("#               Dep Traversal type      : " + depTraversal);
		System.out.println("#               Site                    : " + buildSite);
		System.out.println("####################################################################################\n");
	}

	/**
	 * Show usage information for the Builder.
	 */
	public void showUsage() {
		logger.info("depbuilder.pl [options]");
		logger.info("Where [options] is the following:");
		logger.info("");
		logger.info("  -help                       : Displays usage info about builder");
		logger.info("  -javadoc		              : Enables/Disables build of javadocs - Optional - Default : false");
		logger.info("  -codecoverage               : Enables/Disables code coverage instrumentation - Optional - Default: false");
		logger.info("  -manifest <xml>		      : specifies the manifest.xml file to be used - Optional - Default: <sourceHome>/dcmanifest/manifest.xml");
		logger.info("  -loglevel [OFF|FATAL|ERROR|WARN|INFO|DEBUG|TRACE|ALL]  : Log level for builder log messages - Optional - Default: INFO");
		logger.info("  -runmode [dev|qe]		      : Mode of the build - QE builds do clean check out of code, do dependency updates etc - Optional - Default : dev");
		logger.info("  -buildtype [all|custom]     : Type of the build(build all or build supplied projects - Optional - Default : all");
		logger.info("  -buildflow [serial|parallel]: Flow of the build(build the projects serially or in parallel - Optional - Default : serial");
		logger.info("  -buildmode [all|incremental]: Mode of the build(build all the code or do only incremental build - Optional - Default : all");
		logger.info("  -buildlocation [local|localremote|farm|jenkins]: Location of the build(build the projects locally, in the farm or in Jenkins - Optional - Default : local");
		logger.info("  -skipdependees              : Skip the dependee builds - Optional - Default : false");
		logger.info("  -skipclean                  : Skip the mvn clean step during build - Optional - Default : false");
		logger.info("  -deptrav [bottomup|topdown] : Mode in which dependencies get traversed - Optional - Default : bottomup");
		logger.info("  -site                       : Runs maven site command for each project - Optional - Default: false");
		System.exit(0);
	}
	
	/**
	 * Gets the projects xml.
	 *
	 * @return the projectsXML
	 */
	public String getProjectsXML() {
		return projectsXML;
	}

	/**
	 * Sets the projects xml.
	 *
	 * @param projectsXML the projectsXML to set
	 */
	public void setProjectsXML(String projectsXML) {
		this.projectsXML = projectsXML;
	}

	/**
	 * Checks if is gen javadoc.
	 *
	 * @return the genJavadoc
	 */
	public boolean isGenJavadoc() {
		return genJavadoc;
	}

	/**
	 * Sets the gen javadoc.
	 *
	 * @param genJavadoc the genJavadoc to set
	 */
	public void setGenJavadoc(boolean genJavadoc) {
		this.genJavadoc = genJavadoc;
	}

	/**
	 * Checks if is gen coverage.
	 *
	 * @return the genCoverage
	 */
	public boolean isGenCoverage() {
		return genCoverage;
	}

	/**
	 * Sets the gen coverage.
	 *
	 * @param genCoverage the genCoverage to set
	 */
	public void setGenCoverage(boolean genCoverage) {
		this.genCoverage = genCoverage;
	}

	/**
	 * Checks if is run site.
	 *
	 * @return the runSite
	 */
	public boolean isRunSite() {
		return runSite;
	}

	/**
	 * Sets the run site.
	 *
	 * @param runSite the runSite to set
	 */
	public void setRunSite(boolean runSite) {
		this.runSite = runSite;
	}

	/**
	 * Checks if is skip dependees.
	 *
	 * @return the skipDependees
	 */
	public boolean isSkipDependees() {
		return skipDependees;
	}

	/**
	 * Sets the skip dependees.
	 *
	 * @param skipDependees the skipDependees to set
	 */
	public void setSkipDependees(boolean skipDependees) {
		this.skipDependees = skipDependees;
	}

	/**
	 * Checks if is skip clean.
	 *
	 * @return the skipClean
	 */
	public boolean isSkipClean() {
		return skipClean;
	}

	/**
	 * Sets the skip clean.
	 *
	 * @param skipClean the skipClean to set
	 */
	public void setSkipClean(boolean skipClean) {
		this.skipClean = skipClean;
	}

	/**
	 * Checks if is builds the site.
	 *
	 * @return the buildSite
	 */
	public boolean isBuildSite() {
		return buildSite;
	}

	/**
	 * Sets the builds the site.
	 *
	 * @param buildSite the buildSite to set
	 */
	public void setBuildSite(boolean buildSite) {
		this.buildSite = buildSite;
	}

	/**
	 * Gets the source home
	 *
	 * @return the sourceHome
	 */
	public String getSourceHome() {
		return sourceHome;
	}

	/**
	 * Sets the source home
	 *
	 * @param sourceHome the sourceHome to set
	 */
	public void setSourceHome(String sourceHome) {
		this.sourceHome = sourceHome;
	}

	/**
	 * Gets the run mode.
	 *
	 * @return the runMode
	 */
	public String getRunMode() {
		return runMode;
	}

	/**
	 * Sets the run mode.
	 *
	 * @param runMode the runMode to set
	 */
	public void setRunMode(String runMode) {
		this.runMode = runMode;
	}

	/**
	 * Gets the builds the type.
	 *
	 * @return the buildType
	 */
	public String getBuildType() {
		return buildType;
	}

	/**
	 * Sets the builds the type.
	 *
	 * @param buildType the buildType to set
	 */
	public void setBuildType(String buildType) {
		this.buildType = buildType;
	}

	/**
	 * Gets the builds the flow.
	 *
	 * @return the buildFlow
	 */
	public String getBuildFlow() {
		return buildFlow;
	}

	/**
	 * Sets the builds the flow.
	 *
	 * @param buildFlow the buildFlow to set
	 */
	public void setBuildFlow(String buildFlow) {
		this.buildFlow = buildFlow;
	}

	/**
	 * Gets the builds the mode.
	 *
	 * @return the buildMode
	 */
	public String getBuildMode() {
		return buildMode;
	}

	/**
	 * Sets the builds the mode.
	 *
	 * @param buildMode the buildMode to set
	 */
	public void setBuildMode(String buildMode) {
		this.buildMode = buildMode;
	}

	/**
	 * Gets the activity.
	 *
	 * @return the activity
	 */
	public String getActivity() {
		return activity;
	}

	/**
	 * Sets the activity.
	 *
	 * @param activity the activity to set
	 */
	public void setActivity(String activity) {
		this.activity = activity;
	}

	/**
	 * Gets the builds the location.
	 *
	 * @return the buildLocation
	 */
	public String getBuildLocation() {
		return buildLocation;
	}

	/**
	 * Sets the builds the location.
	 *
	 * @param buildLocation the buildLocation to set
	 */
	public void setBuildLocation(String buildLocation) {
		this.buildLocation = buildLocation;
	}

	/**
	 * Gets the dep traversal.
	 *
	 * @return the depTraversal
	 */
	public String getDepTraversal() {
		return depTraversal;
	}

	/**
	 * Sets the dep traversal.
	 *
	 * @param depTraversal the depTraversal to set
	 */
	public void setDepTraversal(String depTraversal) {
		this.depTraversal = depTraversal;
	}

	/**
	 * Gets the log level.
	 *
	 * @return the logLevel
	 */
	public Level getLogLevel() {
		return logLevel;
	}

	/**
	 * Sets the log level.
	 *
	 * @param logLevel the logLevel to set
	 */
	public void setLogLevel(Level logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * Sets the log level by taking a string as the argument.
	 *
	 * @param logLevel - The logLevel to set - passed in as a String object.
	 */
	public void setLogLevel(String logLevel) {
		this.logLevel = Level.toLevel(logLevel);
	}

	/**
	 * Gets the custom projects.
	 *
	 * @return the customProjects
	 */
	public ArrayList<String> getCustomProjects() {
		return customProjects;
	}

	/**
	 * Sets the custom projects.
	 *
	 * @param customProjects the customProjects to set
	 */
	public void setCustomProjects(ArrayList<String> customProjects) {
		this.customProjects = customProjects;
	}

	/**
	 * Gets the custom targets.
	 *
	 * @return the customTargets
	 */
	public ArrayList<String> getCustomTargets() {
		return customTargets;
	}

	/**
	 * Sets the custom targets.
	 *
	 * @param customTargets the customTargets to set
	 */
	public void setCustomTargets(ArrayList<String> customTargets) {
		this.customTargets = customTargets;
	}

	/**
	 * @return the branch
	 */
	public String getBranch() {
		return branch;
	}

	/**
	 * @param branch the branch to set
	 */
	public void setBranch(String branch) {
		this.branch = branch;
	}

	public boolean isAbortIfNoChanges() {
		return abortIfNoChanges;
	}

	public void setAbortIfNoChanges(boolean abortIfNoChanges) {
		this.abortIfNoChanges = abortIfNoChanges;
	}

	/**
	 * @return the skipTests
	 */
	public boolean skipTests() {
		return skipTests;
	}

	/**
	 * @param skipTests the skipTests to set
	 */
	public void setSkipTests(boolean skipTests) {
		this.skipTests = skipTests;
	}

	public String getBuildArena() {
		return buildArena;
	}

	public void setBuildArena(String buildArena) {
		this.buildArena = buildArena;
	}

}
