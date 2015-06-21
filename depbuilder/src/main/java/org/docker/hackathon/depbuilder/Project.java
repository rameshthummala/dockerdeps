package org.docker.hackathon.depbuilder;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.text.*;
import java.net.*;
import java.security.MessageDigest;

import javax.mail.internet.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.MailingList;

import org.docker.hackathon.util.Executor;
import org.docker.hackathon.util.FileUtils;
import org.docker.hackathon.util.PomUtils;
import org.docker.hackathon.util.NexusUtils;
import org.docker.hackathon.util.DigestUtils;
import org.docker.hackathon.util.EmailUtil;
import org.docker.hackathon.util.DBOps;
import org.docker.hackathon.depbuilder.Constants;

/**
 * Project object extends the <code>org.apache.maven.project.MavenProject</code> and adds the Data.com specific logic to it:<p>
 *     1. Dependencies and Dependee vectors for upstream and downstream dependency calculations<br>
 *     2. Methods to build a project using maven<br>
 *     3. Methods to print the dependency and dependee vectors.<br>
 *     <p>
 *     Uses log4j for logging.
 */
public class Project extends MavenProject {

	/** The name of the project. */
	private String name;

	/** The branch from which the project code is checked out. 
	 *  Maintained per project in the manifest in the dcmanifest project.
	 *  Loaded by DependencyFactory and passed onto Project object constructor. */
	private String branch;

	private String scmBranch;

	private String tag;

	private String revision;

	private String genPkg = "no";

	/** The build tool used for doing the project build. 
	 *  Allowed values : ant, mvn or "ant,mvn"
	 *  Temporary and will be deprecated once all projects move to using Maven. */
	private String buildTool;

	/** The Source Code Management system type used for the project.
	 *  Allowed Values : svn, git */
	private String scmType;

	/** The SCM URL for the project. 
	 */
	private String scmURL;

	/** The source directory where project source is checked out and built. 
	 *  Used only when buildLocation is set to local (the default). 
	 *  In case of Jenkins builds, this variable is not used. 
	 */
	private String localSourceHome;

        /** The source directory where project source is checked out and built.
         *  Used only when buildLocation is set to localremote.
         */
        private String localRemoteSourceHome;

	/** The build file which contains Maven Project Object Model metadata. */
	private String buildFile = "pom.xml";

	private String buildStartTime = null;
	private String buildEndTime = null;
	
	private String currentTag = "";
	private String currentRevision = "";
	
	private String newTag = "";
	private String newRevision = "";
	
	private String lastTag = "";
	private String lastRevision = "";
	
	private ChangeList changeList;

	private boolean codeChanged = false;

	private boolean skipTests = false;
	
	public boolean shouldSkipTests() {
		return skipTests;
	}

	public void setSkipTests() {
		this.skipTests = true;
	}

	public String getCurrentTag() {
		return currentTag;
	}

	public void setCurrentTag(String currentTag) {
		this.currentTag = currentTag;
	}

	public String getCurrentRevision() {
		return currentRevision;
	}

	public void setCurrentRevision(String currentRevision) {
		this.currentRevision = currentRevision;
	}

	public String getNewTag() {
		return newTag;
	}

	public void setNewTag(String newTag) {
		this.newTag = newTag;
	}

	public String getNewRevision() {
		return newRevision;
	}

	public void setNewRevision(String newRevision) {
		this.newRevision = newRevision;
	}
	
	public String getRevision() {
		return revision;
	}

	public void setRevision(String passedRevision) {
		this.revision = passedRevision;
	}
	
	public String getBuildEndTime() {
		return buildEndTime;
	}

	public void setBuildEndTime(String buildEndTime) {
		this.buildEndTime = buildEndTime;
	}

	public String getBuildStartTime() {
		return buildStartTime;
	}

	public void setBuildStartTime(String buildStartTime) {
		this.buildStartTime = buildStartTime;
	}

	public String getBuildFile() {
		return buildFile;
	}

	public void setBuildFile(String buildFile) {
		this.buildFile = buildFile;
	}

	public String getGenPkg() {
		return genPkg;
	}

	public void setGenPkg(String genPkg) {
		this.genPkg = genPkg;
	}

	public ChangeList getChangeList() {
		return changeList;
	}

	public void setChangeList(ChangeList changeList) {
		this.changeList = changeList;
	}

	/** The temporary Maven POM file used for loading project metadata for all projects
	 *  Used only for project upstream and downstream dependency calculation
	 *  Not used during the real project build. 
	 */
	private String tempbuildfile = "pom.xml";

	/** Flag to switch the SVN Provider implementation to use SVNKit. */
	private boolean useJavaHL = false;

	/** The temporary model variable to generate and set the model in Project object. */
	Model model = null;

	/** Vector where all the downstream dependencies are maintained. 
	 *  This is populated when the Project object (which in turn extends the MavenProject object) is loaded.*/
	Vector<Dependency> jigsawDependencies;

	/** Vector where all the upstream dependees for each project are maintained. 
	 *  This is populated using the loadDependees method of DependencyFactory.*/
	Vector<Dependency> jigsawDependees;
	
	Vector<Project> reactorProjects;

	/** The build success. */
	private boolean buildSuccess = Constants.SUCCESS;

	/** The build config. */
	private BuildConfig buildConfig ;
	
	Vector <String> targetsVector = new Vector <String> ();

	Level logLevel = Level.toLevel("DEBUG");
	static Logger logger = Logger.getLogger(Project.class.getName());

	public void setLogLevel(Level passedLogLevel) {
		logLevel = passedLogLevel;
		logger.setLevel(logLevel);
	}

	public Level getLogLevel() {
		return logLevel;
	}

	/**
	 * Gets the local source home.
	 *
	 * @return the local source home
	 */
	public String getLocalSourceHome() {
		return localSourceHome;
	}

	/**
	 * Sets the local source home.
	 *
	 * @param localSourceHome the new local source home
	 */
	public void setLocalSourceHome(String localSourceHome) {
		this.localSourceHome = localSourceHome;
	}

	/**
	 * Gets the build config
	 *
	 * @return the build config
	 */
	public BuildConfig getBuildConfig() {
		return buildConfig;
	}

	/**
	 * Sets the local build config
	 *
	 * @param buildConfig the new build config
	 */
	public void setBuildConfig(BuildConfig buildConfig) {
		this.buildConfig = buildConfig;
	}

	/**
	 * Checks if this project build is successful.
	 *
	 * @return true, if is builds the success
	 */
	public boolean isBuildSuccess() {
		return buildSuccess;
	}

	/**
	 * Takes the outcome of the build and sets it in the Project's buildSuccess variable.
	 *
	 * @param buildSuccess - The passed varible which denotes whether a project build is successful or not.
	 */
	public void setBuildSuccess(boolean buildSuccess) {
		this.buildSuccess = buildSuccess;
	}

	public boolean isCodeChanged() {
		return codeChanged;
	}

	public void setCodeChanged(boolean codeChanged) {
		this.codeChanged = codeChanged;
	}

	/**
	 * @return the reactorProjects
	 */
	public Vector<Project> getReactorProjects() {
		return reactorProjects;
	}

	/**
	 * @param reactorProjects the reactorProjects to set
	 */
	public void addToReactorProjects(Project reactorProject) {
		reactorProjects.add(reactorProject);
	}

	public Vector<String> getTargetsVector() {
		return targetsVector;
	}

	/**
	 * @param reactorProjects the reactorProjects to set
	 */
	public void addToTargetsVector(String target) {
		targetsVector.add(target);
	}

	
	public static enum BuildResult {
		SUCCESS, FAILURE
	}

	public static boolean checkoutTempPOM(String projName, String projBranch, String sourceHome) {
		return checkoutTempPOM(projName, projBranch,sourceHome,false,new String(""));
	}

	public static boolean checkoutTempPOM(String projName, String projBranch,String sourceHome, boolean isModule, String moduleParent) {
        	String checkoutcmd = "";
                String projScmBranch = "";
                if(projBranch.equals("trunk") || projBranch.contains("-BRANCH")) {
                	projScmBranch = projBranch;
                } else {
                	if(isModule) {
                		projScmBranch = "branches/" + moduleParent+"-"+projBranch+"-BRANCH";
                	} else {
                		projScmBranch = "branches/" + projName+"-"+projBranch+"-BRANCH";
                	}
                }
		String pomFilePath = "";
		if(isModule) {
			pomFilePath = SCMFactory.getSVNRepoURL() + moduleParent + "/" + projScmBranch + "/" + projName + " " + sourceHome + "/pomfiles/" + moduleParent + "/" + projName;
		} else {
			pomFilePath = SCMFactory.getSVNRepoURL() + projName + "/" + projScmBranch + " " + sourceHome + "/pomfiles/" + projName;
		}
		boolean retval = Constants.SUCCESS;
		checkoutcmd = "svn co --depth files " + pomFilePath;
		System.out.println("Check out " + projName + " pom.xml :" + pomFilePath
				+ " into pomfiles directory : ");
		System.out.println("Command used is " + checkoutcmd);
		Executor exec = new Executor();
		// exec.setLogLevel(Level.toLevel("DEBUG"));
		// exec.setShowOutput(true);
		retval = exec.run(checkoutcmd);
		if (retval) {
			System.out.println("Check out " + pomFilePath + " into pomfiles directory : SUCCESS");
		} else {
			System.out.println("Check out " + pomFilePath + " into pomfiles directory : FAILURE");
		}
		
		return retval;
	}

	/**
	 * Instantiates a new Project object.
	 *
	 * @param passedName - The passed project name
	 */
	public Project(String passedName,String passedBranch,BuildConfig passedBuildConfig, boolean isModule, String moduleParent) {
		System.out.println("Project: Constructor called with Name :"+passedName+" Branch: "+passedBranch+" Parent: "+moduleParent);
		setName(passedName);
		setBranch(passedBranch);
		// Has to be called after setBranch since setScmBranch uses the branch variable for its calculation.
		setScmBranch(); 
		setBuildConfig(passedBuildConfig);
		setScmURL(SCMFactory.getSVNRepoURL() + getName() + "/"+ getScmBranch());
		setLocalSourceHome(getBuildConfig().getSourceHome() + "/" + getName());
		String pomFile = "";
		String tempPomFile = "";
		if (isModule) {
			pomFile = passedBuildConfig.getSourceHome()+"/"+moduleParent+"/"+passedName+"/pom.xml";
			tempPomFile = passedBuildConfig.getSourceHome()+"/pomfiles/"+moduleParent+"/"+passedName+"/pom.xml";
		} else {
			pomFile = passedBuildConfig.getSourceHome()+"/"+passedName+"/pom.xml";
			tempPomFile = passedBuildConfig.getSourceHome()+"/pomfiles/"+passedName+"/pom.xml";
		}
		FileReader reader = null;
		setBuildFile(pomFile);
		if (getBuildConfig().getRunMode().equals("qe") || passedName.equals("dcmanifest")) {
			// We checkout the temporary pom.xml into the pomfiles only if the runmode is QE.
			// For Dev build, the cached pom.xmls are downloaded and used.
			checkoutTempPOM(passedName,passedBranch,passedBuildConfig.getSourceHome(),isModule,moduleParent);
		}
		System.out.println("Determined build file is : " + getBuildFile());
		MavenXpp3Reader mavenreader = new MavenXpp3Reader();
		try {
			reader = new FileReader(tempPomFile);
			model = mavenreader.read(reader);
			model.setPomFile(new File(pomFile));
		} catch (Exception ex) {
			System.out.println("Unable to read and parse the "+pomFile+" for project initialization.");
		}
		setModel(model);

		System.out.println("Added a new Project element with Name: "+getName()+" Branch: "+getBranch()+" BuildFile: "+getBuildFile()+"\n");

		jigsawDependencies = new Vector<Dependency>();
		jigsawDependees = new Vector<Dependency>();
		reactorProjects = new Vector<Project>();

		// Add the parent as a dependency for the build purposes.
		Parent par = getModel().getParent();
		if(par != null) {
			Dependency parent = new Dependency();
			parent.setGroupId(par.getGroupId());
			parent.setArtifactId(par.getArtifactId());
			parent.setVersion(par.getVersion());
			jigsawDependencies.add(parent);
		}

		final List<Dependency> deps = getModel().getDependencies();
		for (int i = 0; i < deps.size(); i++) {
			final Dependency dependency = deps.get(i);
			if (dependency.getGroupId().equals("org.docker.hackathon")) {
				jigsawDependencies.add(dependency);
			}
		}
		// Load reactor projects into their own Project objects
		final List<String> modules = getModel().getModules();
		for (int i = 0; i < modules.size(); i++) {
			final String module = modules.get(i);
			Project moduleProject = new Project(module,passedBranch,passedBuildConfig,true,getName());
			reactorProjects.add(moduleProject);
		}
		
		
	}
	
	public void calculateChangeList() {
		
		if (getBuildConfig().getRunMode().equals("qe")) {
			String latestTag = SCMFactory.getLatestTag(getName(), getVersion());
			if (latestTag.equals("")) {
				// No tags for this version series. Probably the first build
				// after FF branch is created. Force a new build.
				setCodeChanged(true);
			} else {
				System.out.println("build : Latest tag detected for " + getName()
						+ " is : " + latestTag);
				String latestURL = SCMFactory.getSVNRepoURL() + getName() + "/"
						+ getScmBranch();
				String lastTagURL = SCMFactory.getSVNRepoURL() + getName()
						+ "/tags/" + getName() + "-" + latestTag;
				// long latestRevision = SCMFactory.getRevision(latestURL);
				// System.out.println("build : Latest revision detected for "+getName()+" is : "+latestRevision);
				try {
					setChangeList(SCMFactory.getChangeList(getName(),
							getLocalSourceHome() + "/" + getName(), lastTagURL,
							latestURL));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if (getBuildConfig().getRunMode().equals("developer")) {
			try {
				setChangeList(SCMFactory.getChangeList(getName(),
						getLocalSourceHome() + "/" + getName()));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Prints the Project details like GAV(GroupID, ArtifactID and Version).
	 * Uses logger to print this information.
	 */
	void printProjectDetails() {
		System.out.println(getArtifactId() + " Details :");
		System.out.println("\tGroupId : " + getGroupId());
		System.out.println("\tArtifactId : " + getArtifactId());
		System.out.println("\tVersion : " + getVersion());
		//System.out.println("\tSCM : "+getScm().getConnection());
	}

	/**
	 * Prints the artifacts for the current project object.
	 */
	void printArtifacts() {
		System.out.println("Entering printArtifacts.\n");
		Set<Artifact> s = getArtifacts();
		Iterator<Artifact> it = s.iterator();
		System.out.println("printArtifacts: Size of the Artifact set is :" + s.size() + "\n");
		while (it.hasNext()) {
			final DefaultArtifact da = (DefaultArtifact) it.next();
			System.out.println("GroupId: " + da.getGroupId() + " ArtifactId: "
					+ da.getArtifactId() + " Version: " + da.getVersion() + "\n");
		}
		System.out.println("Exiting printArtifacts.\n");
	}

	/**
	 * Gets the jigsaw Dependencies vector for this project.
	 * Dependencies are not ordered in the dependency order.
	 *
	 * @return The jigsaw dependencies vector
	 */
	Vector<Dependency> getJigsawDependencies() {
		// Return the dependencies in a vector
		return jigsawDependencies;
	}

	/**
	 * Gets the Dependee object vector for this project.
	 * Dependees are not ordered in the dependency order.
	 *
	 * @return the dependee object vector
	 */
	Vector<Dependency> getDependees() {
		// Return the dependees in a List
		return jigsawDependees;
	}

	/**
	 * Adds the passed dependency object into the dependee vector.
	 *
	 * @param dep - The passed dependency object
	 */
	void addDependee(Dependency dep) {
		jigsawDependees.add(dep);
		System.out.println("Project::addDependee : The size of jigsawDependees is :" + jigsawDependees.size() + "\n");
	}

	/**
	 * Checks if a passed project is listed as a dependency for the project object.
	 *
	 * @param passedproj - The passed project name
	 * @return true, if the passed project is a dependency
	 * @return false, if the passed project is not a dependency
	 */
	boolean isDependency(String passedproj) {
		// Traverse the jigsaw dependencies to see if a passed project is a dependency
		for (int i = 0; i < jigsawDependencies.size(); i++) {
			final Dependency dependency = jigsawDependencies.get(i);
			if (dependency.getArtifactId().equals(passedproj)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Prints the dependencies.
	 */
	void printDependencies() {
		// Print the dependencies on the console
		System.out.println("DEPENDENCIES for " + getName() + " are :");
		for (int i = 0; i < jigsawDependencies.size(); i++) {
			final Dependency dependency = jigsawDependencies.get(i);
			System.out.println(dependency.getGroupId() + " / "
					+ dependency.getArtifactId() + " / "
					+ dependency.getVersion() + " / " + dependency.getScope());
		}
	}

	/**
	 * Prints the all dependencies.
	 */
	void printAllDependencies() {
		// Print the dependencies on the console
		final List<Dependency> dependencies = getModel().getDependencies();
		for (int i = 0; i < dependencies.size(); i++) {
			final Dependency dependency = dependencies.get(i);
			System.out.println(dependency.getGroupId() + " / "
					+ dependency.getArtifactId() + " / "
					+ dependency.getVersion() + " / " + dependency.getScope());
		}

	}

	/**
	 * Prints the dependees.
	 */
	void printDependees() {
		// Print the dependees on the console
		System.out.println("");
		System.out.println("DEPENDEES for " + getName() + " are :");
		for (int i = 0; i < jigsawDependees.size(); i++) {
			final Dependency dependency = jigsawDependees.get(i);
			System.out.println(dependency.getGroupId() + " / "
					+ dependency.getArtifactId() + " / "
					+ dependency.getVersion() + " / " + dependency.getScope());
		}
		System.out.println("");
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.project.MavenProject#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.project.MavenProject#setName(java.lang.String)
	 */
	public void setName(String passedName) {
		name = passedName;
	}

	/**
	 * Gets the branch.
	 *
	 * @return the branch
	 */
	public String getBranch() {
		return branch;
	}

	/**
	 * Sets the branch.
	 *
	 * @param passedBranch the new branch
	 */
	public void setBranch(String passedBranch) {
		branch = passedBranch;
	}

	public String getScmBranch() {
		return scmBranch;
	}

	public void setScmBranch() {
			scmBranch = branch;
	}

	/**
	/**
	 * Gets the builds the tool.
	 *
	 * @return the builds the tool
	 */
	public String getBuildTool() {
		return buildTool;
	}

	/**
	 * Sets the builds the tool.
	 *
	 * @param passedBuildTool the new builds the tool
	 */
	public void setBuildTool(String passedBuildTool) {
		buildTool = passedBuildTool;
	}

	/**
	 * Gets the SCM Type.
	 *
	 * @return the scm type
	 */
	public String getScmType() {
		return scmType;
	}

	/**
	 * Sets the SCM Type.
	 *
	 * @param passedScmType the new scm type
	 */
	public void setScmType(String passedScmType) {
		scmType = passedScmType;
	}

	/**
	 * Gets the SCM (SVN) URL for this project
	 *
	 * @return the scm url
	 */
	public String getScmURL() {
		return scmURL;
	}

	/**
	 * Sets the SCM (SVN) URL for this project.
	 *
	 * @param passedScmURL the new scm url
	 */
	public void setScmURL(String passedScmURL) {
		scmURL = passedScmURL;
	}

	public boolean commitPOM(String pomFile, String commitCommentFile) {
		System.out.println("Committing the updated "+getBuildFile());
		String svncmd = "svn commit --file "+commitCommentFile+" "+pomFile;
		Executor exec = new Executor();
		exec.setLogLevel(Level.toLevel("DEBUG"));
		exec.setShowOutput(true);
		boolean retval = exec.run(svncmd);
		return retval;
	}

	public boolean commitPOM(String [] pomList, String commitCommentFile) {
		System.out.println("Committing the updated "+getBuildFile());
		
		String svncmd = "svn commit --file "+commitCommentFile+" ";
		for (int iter=0;iter<pomList.length;iter++) {
			svncmd = svncmd + " "+pomList[iter];
		}
		Executor exec = new Executor();
		exec.setLogLevel(Level.toLevel("DEBUG"));
		exec.setShowOutput(true);
		boolean retval = exec.run(svncmd);
		return retval;
	}
	
	/**
	 * Checkout code for this project.
	 * Uses the SCM URL for checking out the code.
	 * Uses the svn executable present in the PATH to do the checkout.
	 * TODO - Change the implementation to use JavaHL client or the SVNKit.
	 *
	 * @return true, if the project source code checkout is successful
	 */
	public boolean checkoutCode() {
		boolean retval = true;
		File projectSourceRoot = new File(getLocalSourceHome());
		long srcAreaRevision = 0;
		if(projectSourceRoot.exists()) {
			// See if the checked out source has any changes. If there are NO changes, we need to update it to the latest SVN revision level.
			srcAreaRevision = SCMFactory.getSourceAreaRevision(getLocalSourceHome());
			long latestRevisionInBackend = SCMFactory.getRevision(getScmURL());
			if (latestRevisionInBackend > srcAreaRevision) {
				System.out.println("The "+getLocalSourceHome()+" path already exists on the file system but is at lower revision level"+srcAreaRevision+". Updating it to "+latestRevisionInBackend);
				// Update the source area to latest revision
				System.out.println(getName() + " svn update: Start time : "+ getCurrentTime());
				String checkoutcmd = "svn update "+ getLocalSourceHome();
				Executor exec = new Executor();
				exec.setLogLevel(Level.toLevel("DEBUG"));
				exec.setShowOutput(true);
				retval = exec.run(checkoutcmd);
				System.out.println(getName() + " svn Update: End time : " + getCurrentTime());
				setRevision(String.valueOf(latestRevisionInBackend));
			} else {
				System.out.println("The "+getLocalSourceHome()+" path already exists on the file system.");
			}
			return retval;
		} else {
			System.out.println(getName() + " Checkout: Start time : "+ getCurrentTime());
			String checkoutcmd = "svn co --depth infinity " + getScmURL() + " "+ getLocalSourceHome();
			Executor exec = new Executor();
			//exec.setLogLevel(Level.toLevel("DEBUG"));
			exec.setShowOutput(true);
			retval = exec.run(checkoutcmd);
			System.out.println(getName() + " Checkout: End time : " + getCurrentTime());
			srcAreaRevision = SCMFactory
					.getSourceAreaRevision(getLocalSourceHome());
			System.out.println(getName() + " Checkout: Source Area Revision : "
					+ srcAreaRevision);
			setRevision(String.valueOf(srcAreaRevision));
		}
		return retval;
	}

	/**
	 * Clean the artifacts generated by the project build.
	 * For Maven, the target directory is cleaned up.
	 *
	 * @return true, if successful
	 */
	public boolean clean() {
		String mvncmd = System.getenv("MAVEN_HOME") + "/bin/mvn -B -U -f " + getBuildFile() + " clean -DskipTests";
		Executor exec = new Executor();
		boolean retval = exec.run(mvncmd);
		exec.setLogLevel(Level.toLevel("DEBUG"));
		exec.setShowOutput(true);
		return retval;
	}

	public boolean isChanged() {
		if(getBuildConfig().isAbortIfNoChanges()) {
			String latestTag = SCMFactory.getLatestTag(getName(),getVersion());
			if(latestTag.equals("")) {
				// No tags for this version series. Probably the first build after FF branch is created. Force a new build.
				setCodeChanged(true);
			} else {
				System.out.println("build : Latest tag detected for "+getName()+" is : "+latestTag);
				String latestURL =  SCMFactory.getSVNRepoURL() + getName() + "/"+getScmBranch();
				String lastTagURL =  SCMFactory.getSVNRepoURL() + getName() + "/tags/"+getName()+"-"+latestTag;
				//long latestRevision = SCMFactory.getRevision(latestURL);
				//System.out.println("build : Latest revision detected for "+getName()+" is : "+latestRevision);
				setCodeChanged(SCMFactory.isChanged(lastTagURL,latestURL));
			}
		} else {
			setCodeChanged(true);
		}
		return isCodeChanged();
	}

	/**
	 * Builds the project using the build tool mentioned in buildTool variable.
	 *
	 * @return true, if the project build is successful
	 */
	public boolean build() {
		boolean retval = Constants.SUCCESS;
		retval = checkoutCode();
		if(!retval) {
			System.out.println("Error while checking out "+getName()+" project code.");
			return retval;
		}

		PomUtils pomu = new PomUtils();
		try {
			if(!getBuildConfig().isSkipDependees()) {
				retval = pomu.updateDependencyVersions(new File(getBuildFile()),getJigsawDependencies(), getVersion());
				// Special case for projects who have reactor modules.
				if(getReactorProjects().size() > 0) {
					for(Project reactorProject : getReactorProjects()) {
							String modulePOMFile = getLocalSourceHome()+"/"+reactorProject.getName()+"/pom.xml";
							System.out.println("build : Updating dependency versions in "+modulePOMFile+" to pickup latest available versions.");
							retval = pomu.updateDependencyVersions(new File(modulePOMFile),reactorProject.getJigsawDependencies(), getVersion());
					}
				}
			if(!retval) {
				System.out.println("Error while changing dependency versions of "+getName()+" project.");
				String dependencyVersionUpdateMailBody = "[ALERT DEPENDENCY VERSION UPDATE FAILED] for : ";
				dependencyVersionUpdateMailBody = dependencyVersionUpdateMailBody + "\t Machine Name : "+InetAddress.getLocalHost().getHostName()+"\n";
				dependencyVersionUpdateMailBody = dependencyVersionUpdateMailBody + "\t Source Home : "+getLocalSourceHome()+"\n";
				dependencyVersionUpdateMailBody = dependencyVersionUpdateMailBody + "\t Project : "+getName()+"\n";
				dependencyVersionUpdateMailBody = dependencyVersionUpdateMailBody + "\t Version : "+getVersion()+"\n";
				String dependencyVersionUpdateMailSubject = "[ALERT DEPENDENCY VERSION UPDATE FAILED] "+getName()+" failed";
				sendMail(dependencyVersionUpdateMailSubject, dependencyVersionUpdateMailBody); 
				return retval;
			} else {
				// During QE build, We need to commit the POM after doing dependency updates.
				if(getBuildConfig().getRunMode().equals("qe")) {
					System.out.println("Committing the pom.xml with dependency updates for "+getName());

					String commitLog = "Automated dependency version update for pom.xml: "+getName()+"\nReviewer: Self ";
					File commentFile = new File("commitcomments.txt");
					if (!commentFile.exists()) {
						commentFile.createNewFile();
					}
					FileWriter commentWriter = new FileWriter(commentFile.getAbsoluteFile());
					BufferedWriter commentBufferWriter = new BufferedWriter(commentWriter);
					commentBufferWriter.write(commitLog);
					commentBufferWriter.close();				

					List<String> modules = getModules();
					String [] pomList = new String [modules.size()+1];
					pomList[0] = getBuildFile();
					int itercount = 1;
					
					if(modules.size() > 0) {
						for(String module : modules) {
								String modulePOMFile = getLocalSourceHome()+"/"+module+"/pom.xml";
								pomList[itercount] = modulePOMFile;
								itercount++;
						}
					}
					
					retval = commitPOM(pomList,"commitcomments.txt"); // Commit the POM so that the version update is picked up.
					if(!retval) {
						System.out.println("Error while committing the pom.xml of "+getName()+" project.");
						// TODO - Temporarily commenting out the return on error until we implement the pom.xml comparison.
						//return retval;
					}
				}
			}
		  }
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		String incrementedProjectVersion = getVersion();
		if(getBuildConfig().getRunMode().equals("qe")) {
			// Increment the project version in the pom.xml so that the new build will pick it up.
			// IMPORTANT : This is a local code change that is not committed. 
			// IMPORTANT : It will be committed only after QE build is successful.
			System.out.println("build : Incrementing the "+getName()+" project version");
			String [] versionParts = Pattern.compile(".", Pattern.LITERAL).split(getVersion().replaceAll("-","."));
			String incrementedRollingVersion = String.valueOf(Integer.parseInt(versionParts[2]) + 1);
			incrementedProjectVersion = versionParts[0]+"."+versionParts[1]+"."+incrementedRollingVersion+"-"+versionParts[3];
			if(getBuildConfig().getBuildType().equals("erelease")) {
				incrementedRollingVersion = String.valueOf(Integer.parseInt(versionParts[3]) + 1);
				incrementedProjectVersion = versionParts[0]+"."+versionParts[1]+"."+versionParts[2]+"-"+incrementedRollingVersion;	
			}
			System.out.println("build : incrementProjectVersion: Current version is : "+getVersion()+" . Incremented version is : "+incrementedProjectVersion);
			retval = pomu.setProjectVersion(new File(getBuildFile()), incrementedProjectVersion);
			
			// Special case for projects who have reactor modules.
			if(getReactorProjects().size() > 0) {
				for(Project reactorProject : getReactorProjects()) {
					try {
						String modulePOMFile = getLocalSourceHome()+"/"+reactorProject.getName()+"/pom.xml";
						// Determine the latest version of the parent.
						String reactorProjectParent = reactorProject.getModel().getParent().getArtifactId();
						String reactorProjectParentGroupId = reactorProject.getModel().getParent().getGroupId();
						if (getArtifactId().equals(reactorProjectParent)) {
							// I am the parent of my reactor project. Update the parent version in reactor project pom.xml to my latest bumped up version.
							System.out.println("build : Updating parent version in "+ modulePOMFile + " to "+ incrementedProjectVersion);
							pomu.setProjectParentVersion(new File(modulePOMFile),incrementedProjectVersion);
						} else {
							// Some other project is the parent of my reactor project. Determine the latest version and update.
							System.out.println("Looking for newer versions of "+ reactorProjectParent);
							String parentVersionParts [] = incrementedProjectVersion.split(".");
							String parentVersionMatchStr = parentVersionParts[0]+"."+parentVersionParts[1];
							String latestReactorProjectParentVersion = NexusUtils.getLatestArtifactVersion(reactorProjectParentGroupId,reactorProjectParent,parentVersionMatchStr);
							System.out.println("build : Updating parent version in "+ modulePOMFile + " to "+ latestReactorProjectParentVersion);
							pomu.setProjectParentVersion(new File(modulePOMFile),latestReactorProjectParentVersion);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			setVersion(incrementedProjectVersion);
		}
		
		if(getBuildConfig().getRunMode().equals("developer") || ( getBuildConfig().getRunMode().equals("qe") && (isChanged())) ) {
			if(getBuildConfig().getRunMode().equals("qe")) {
				setBuildStartTime(getCurrentTime());
			}
			retval = codeBuild();
			if(retval != Constants.SUCCESS) {
				System.out.println("build : Error while building "+getName()+" project code.");
				if(getBuildConfig().getRunMode().equals("qe")) {
					String buildFailureMailBody = formBuildFailureMsg();
					String buildFailureMailSubject = "[ALERT BUILD FAILED] "+getName()+" "+getBranch()+" Build failed";
					sendMail(buildFailureMailSubject, buildFailureMailBody); 
				}
				return retval;
			}
		} else {
			System.out.println("There is NO code change in "+getName()+" project from last tag. Skipping the build.");
		}
		return retval;
	}

	public boolean codeBuild() {
		System.out.println("#################################################################");
		System.out.println("##  Project::build : Starting build of " + getName());
		System.out.println("#################################################################");
		String buildcmd = "";
		boolean retval = Constants.SUCCESS;
		
			if (getBuildTool().equals("mvn")) {
					//clean();
					String targets = "clean";
					String skipTestsOption = "";
					if ( ( getBuildConfig().skipTests() || ( shouldSkipTests() ) ) || 
						 ( (!getBuildConfig().getRunMode().equals("qe")) && getName().equals("bluemaster")) ) {
						// We are skipping the tests if the repo name is bluemaster.
						// TODO - Remove this once the bluetail adds a new test bucket for precheckin
						skipTestsOption = " -DskipTests ";
					}
					if(getBuildConfig().getRunMode().equals("qe")) {

						String localRepoLoc = System.getProperty("maven.repo.local");
						if(localRepoLoc == null) {
							localRepoLoc = getBuildConfig().getSourceHome()+"/.m2";
						}
						buildcmd = System.getenv("MAVEN_HOME") + "/bin/mvn -B -U -Prpmbuild -Dmaven.repo.local="+localRepoLoc+" -f " + getBuildFile() + " " + skipTestsOption + " ";
					} else {

						buildcmd = System.getenv("MAVEN_HOME") + "/bin/mvn -B -U -f " + getBuildFile() + " " + skipTestsOption + " " ;
					}

					Enumeration<String> tgtenum = getTargetsVector().elements();
					while (tgtenum.hasMoreElements()) {
						String tgt  = (String) tgtenum.nextElement();
						String curbuildcmd = buildcmd + tgt;
						System.out.println("codeBuild : Invoking build command : "+curbuildcmd);
						Executor exec = new Executor();
						exec.setLogLevel(Level.toLevel("DEBUG"));
						exec.setShowOutput(true);
						exec.setLogOutputEnabled(true);
						exec.setOutputLog(new File(getLocalSourceHome()+"/"+getName()+"-"+getVersion()+".BUILDLOG.2.6.txt"));
						boolean localretval = exec.run(curbuildcmd);
						if(localretval != Constants.SUCCESS) {
							retval = localretval;
							// We break out whenever one of the targets fail.
							break;
						}
					}
			}
	
		System.out.println("Project::build : Completed build of " + getName()+ " with return code : " + retval);
		setBuildSuccess(retval);
		return retval;
	}

	public boolean generateChangeLogFromTags() {
		String oldTagURL = SCMFactory.getSVNRepoURL() + getName() + "/tags/"+getCurrentTag();
		String newTagURL = SCMFactory.getSVNRepoURL() + getName() + "/tags/"+getNewTag();
		String diffcmd = "svn diff "+newTagURL+" "+oldTagURL;

		System.out.println("generateChangeLog : Invoking diff command : "+diffcmd);
		Executor exec = new Executor();
		exec.setLogLevel(Level.toLevel("DEBUG"));
		exec.setShowOutput(true);
		exec.setLogOutputEnabled(true);
		exec.setOutputLog(new File(getLocalSourceHome()+"/"+getName()+"-"+getVersion()+".DIFFLOG.2.6.txt"));
		boolean retval = exec.run(diffcmd);
		return retval;
	}
	
	/**
	 * Publishes the built artifacts to the artifact repository (Nexus).
	 * The Nexus repo name is controlled by the settings.xml and pom.xml.
	 * Artifact publishing happens only during QE build.
	 *
	 * @return true, if successful
	 */
	public boolean publishArtifacts() {
		System.out.println("#################################################################");
		System.out.println("##  Project::build : Publishing " + getName() + " project build artifacts to Nexus.");
		System.out.println("#################################################################");
		String buildcmd = "";
		boolean retval = true;
		if (getBuildTool().equals("mvn")) {
				clean();
				buildcmd = System.getenv("MAVEN_HOME") + "/bin/mvn -B -U -f " + getBuildFile() + " deploy -DskipTests";
				Executor exec = new Executor();
				exec.setLogLevel(Level.toLevel("DEBUG"));
				exec.setShowOutput(true);
				retval = exec.run(buildcmd);
		}
		if(retval == Constants.FAILURE) {
			System.out.println("Project::build : Artifact publishing for " + getName()+ " successful with return code : " + retval);
		} else {
			System.out.println("Project::build : Artifact publishing for " + getName()+ " failed with return code : " + retval);
		}
		setBuildSuccess(retval);
		return retval;
	}

	public boolean generateTag(String version) {
		boolean retval = true;
		try {
			String tagRepoPath = SCMFactory.getSVNRepoURL() + getName() + "/tags/"+getName()+"-"+version;
			String repoURLPath = SCMFactory.getSVNRepoURL() + getName() + "/"+getBranch();
			//if(!SCMFactory.exists(tagRepoPath)) {
			//	System.out.println(tagRepoPath+" already exists in the SVN backend for "+getName()+" project. returning.");
			//	return retval;
			//}
			String commitLog = "Automated "+getName()+"-"+version+" tag generation for : "+getName()+"\nReviewer: Self ";
			File commentFile = new File("commitcomments.txt");
			if (!commentFile.exists()) {
				commentFile.createNewFile();
			}
			FileWriter commentWriter = new FileWriter(commentFile.getAbsoluteFile());
			BufferedWriter commentBufferWriter = new BufferedWriter(commentWriter);
			commentBufferWriter.write(commitLog);
			commentBufferWriter.close();

			System.out.println("Generating tag for "+getName());
			String svntagcmd = "svn copy "+getLocalSourceHome()+" "+ tagRepoPath+" --file commitcomments.txt";
			Executor exec = new Executor();
			exec.setLogLevel(Level.toLevel("DEBUG"));
			exec.setShowOutput(true);
			retval = exec.run(svntagcmd);
			if(retval == Constants.FAILURE) {
				System.out.println("Error "+retval+" while generating the tag generation for "+getName()+"-"+getVersion());
			} else {
				System.out.println("Completed the tag generation for "+getName()+"-"+getVersion());
				// Add the Revision vs Tag mapping into the revisiontag table.
				setRevision(String.valueOf(SCMFactory.getSourceAreaRevision(getLocalSourceHome())));
			}
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}

		return retval ;
	}

	public void sendMail(String subject, String body) {
		try {
		InternetAddress [] toAddresses = new InternetAddress [getMailingLists().size()+1];
		int i=0;

		for(MailingList ml : getMailingLists()) {
			String list = ml.getPost();
			System.out.println("sendMail: Adding "+list+" to the list of recepients.");
			toAddresses[i] = new InternetAddress(list);
			i++;
		}
		toAddresses[i] = new InternetAddress("ramesh.thummala@gmail.com");
		// Send the mail with the formed toAddresses.
		EmailUtil.sendMail(toAddresses,subject,body);
		} catch(AddressException e) {
			System.out.println("sendMail : Invalid Address received.");
		}
	}

	public String getLastFewLines(String logFile) {
		String lastFewLines = new String();
		try {
			File logFileFD = new File(logFile);
			if(logFileFD.exists()) {
				int numLines = countLines(logFile);
				BufferedReader in = new BufferedReader(new FileReader(logFile));
				String line = null;
				int currline = 1;
				boolean inErrorZone = false;
				while( ( line = in.readLine() ) != null ) {
					if(currline > numLines - 100) {
						if(line.contains("ERROR")) {
							if (inErrorZone == false) {
								// We are entering the error zone. Start red color font.
								lastFewLines = lastFewLines+"<font color=\"red\">";
								inErrorZone = true;
							}
						} else {
							if(inErrorZone) {
								// If we are already in error message, close out the red font.
								lastFewLines = lastFewLines+"</font>";
								inErrorZone=false;
							}
						}
						lastFewLines = lastFewLines+line+"<br>";
					}
					currline++;
				}
				in.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lastFewLines;
	}
	
	public int countLines(String filename) throws IOException {
		LineNumberReader reader  = new LineNumberReader(new FileReader(filename));
		int cnt = 0;
		while (reader.readLine() != null) {}
		cnt = reader.getLineNumber(); 
		reader.close();
		return cnt;
	}

	/**
	 * Gets the current time.
	 *
	 * @return the current time
	 */
	public String getCurrentTime() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
	}

	//public static void main(String[] args) {
	//	Project proj = new Project(args[0]);
	//  proj.printArtifacts();
	//	proj.printDependencies();
	//	proj.build();
	//}
}
