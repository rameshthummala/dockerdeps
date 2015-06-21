package org.docker.hackathon.depbuilder;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.MailingList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.docker.hackathon.depbuilder.Project;
import org.docker.hackathon.util.Executor;
import org.docker.hackathon.util.FileUtils;
import org.docker.hackathon.util.PomUtils;
import org.docker.hackathon.util.EmailUtil;
import org.docker.hackathon.util.JarUtils;
import org.docker.hackathon.util.DownloadUtils;

/**
 * <code>DependencyFactory</code> class contains logic to calculate dependencies, dependees, changed projects, affected projects 
 * and order them based on the project dependency ordering.<p>
 * The broad functions that this module performs are as follows:
 * 		1. Aggregates the Project objects
 * 		2. Calculates the upstream and downstream dependencies.
 * 		3. Provides helper routines to print and build a passed set of projects.
 * <p>
 * Primary input to the DependencyFactory is the BuildConfig Object. 
 * All user configurable data is maintained in the BuildConfig object.
 * 
 *  There are two entry points into the DependencyFactory:
 *  	1. The standalone java builder (<code>org.docker.hackathon.builder.Main</code>)
 *  	2. The Maven plugin (<code>org.docker.hackathon.plugin.mvntools-maven-plugin</code>)
 */

public class DependencyFactory {
	static Logger logger = Logger.getLogger(DependencyFactory.class.getName());
	private BuildConfig bf = new BuildConfig();
	private HashMap<String, Project> hashMap = null;
	//private Project manifestProject = null;
	private Vector<Project> topLevelProjects = new Vector<Project>();
	private Vector<Project> topLevelProjectsSubset = new Vector<Project>();
	private HashSet<Project> topDownProjects = new HashSet<Project>();
	private HashSet<Project> changedProjects = new HashSet<Project>();
	private HashSet<Project> affectedProjects = new HashSet<Project>();
	private HashSet<Project> failedProjects = new HashSet<Project>();
	private HashSet<Project> skippedProjects = new HashSet<Project>();
	private Vector<Project> affectedOrderedProjects = new Vector<Project>();
	private String manifestXml = System.getenv("DDC_SRC_HOME")+ "/manifest/manifest.xml";
	private String fullBuildManifestXml = System.getenv("DDC_SRC_HOME")+ "/manifest/manifest.xml";
	private String manifestProperties = System.getenv("DDC_SRC_HOME")+ "/manifest/src/main/resources/manifest.properties";
	private String fullBuildManifestProperties = System.getenv("DDC_SRC_HOME")+ "/manifest/src/main/resources/manifest.properties";
	private String commonBranch = "trunk";
	private String commonSCMURL = "https://svnhost.re.jigsaw.com/repos/";
	private String majorRelease = "";
	
	private Date buildStartDate;
	private Date buildEndDate;

	/**
	 * Instantiates a new dependency factory.
	 * 
	 * @param bf - The BuildConfig object with name:value pairs passed command-line or from plugin.
	 */
	public DependencyFactory(BuildConfig bf) {
		super();
		this.bf = bf;
		System.out.println((Level)bf.getLogLevel());
		if(bf.getRunMode().equals("developer")) {
			setManifestXml(bf.getSourceHome()+"/manifest/manifest.xml");
			setFullBuildManifestXml(bf.getSourceHome()+"/manifest/manifest.xml");
		} else {
			setManifestXml(bf.getSourceHome()+"/manifest/manifest_qe.xml");
			setFullBuildManifestXml(bf.getSourceHome()+"/manifest/manifest_qe.xml");
			setManifestProperties(bf.getSourceHome()+"/manifest/src/main/resources/manifest_qe.properties");
			setFullBuildManifestProperties(bf.getSourceHome()+"/manifest/src/main/resources/manifest_qe.properties");
		}
	}

	public String getMajorRelease() {
		return majorRelease;
	}

	public void setMajorRelease(String majorRelease) {
		this.majorRelease = majorRelease;
	}

	public String getCommonBranch() {
		return commonBranch;
	}

	public void setCommonBranch(String passedCommonBranch) {
		commonBranch = passedCommonBranch;
	}

	public String getCommonSCMURL() {
		return commonSCMURL;
	}

	public void setCommonSCMURL(String passedCommonSCMURL) {
		commonSCMURL = passedCommonSCMURL;
	}

	/**
	 * Gets the filesystem path of the manifest.xml to be used for full build.
	 * 
	 * @return the filesystem path of the manifest.xml to be used for full build.
	 */
	public String getFullBuildManifestXml() {
		return fullBuildManifestXml;
	}

	/**
	 * Sets the filesystem path of the manifest.xml for full build
	 * 
	 * @param fullBuildManifestXml - The path of the manifest.xml for full build
	 */
	public void setFullBuildManifestXml(String fullBuildManifestXml) {
		this.fullBuildManifestXml = fullBuildManifestXml;
	}

        /**
         * Gets the filesystem path of the manifest.properties to be used for full build.
         *
         * @return the filesystem path of the manifest.properties to be used for full build.
         */
        public String getFullBuildManifestProperties() {
                return fullBuildManifestProperties;
        }

        /**
         * Sets the filesystem path of the manifest.properties for full build
         *
         * @param fullBuildManifestXml - The path of the manifest.properties for full build
         */
        public void setFullBuildManifestProperties(String fullBuildManifestProperties) {
                this.fullBuildManifestProperties = fullBuildManifestProperties;
        }

	/**
	 * Instantiates a new dependency factory.
	 */
	public DependencyFactory() {
	}

	/**
	 * Gets the builds the xml.
	 * 
	 * @return the builds the xml
	 */
	public String getManifestXml() {
		return manifestXml;
	}

	/**
	 * Sets the filesystem path of the manifest.xml for the build.
	 * In case of a full build, the manifestXml will be same as fullBuildManifestXml
	 * 
	 * @param passedManifestXml - The filesystem path of the manifest.xml to be used for build.
	 */
	public void setManifestXml(String passedManifestXml) {
		manifestXml = passedManifestXml;
	}

       /**
         * Gets the manifest properties file.
         *
         * @return the the manifest properties file location.
         */
        public String getManifestProperties() {
                return manifestProperties;
        }

        /**
         * Sets the filesystem path of the manifest.xml for the build.
         * In case of a full build, the manifestProperties will be same as fullBuildManifestProperties
         *
         * @param passedManifestProperties - The filesystem path of the manifest.properties to be used for build.
         */
        public void setManifestProperties(String passedManifestProperties) {
                manifestProperties = passedManifestProperties;
        }


	public HashSet<Project> getTopDownProjects() {
		return topDownProjects;
	}

	public void setTopDownProjects(HashSet<Project> topDownProjects) {
		this.topDownProjects = topDownProjects;
	}

	public Date getBuildStartDate() {
		return buildStartDate;
	}

	public void setBuildStartDate(Date buildStartDate) {
		this.buildStartDate = buildStartDate;
	}

	public Date getBuildEndDate() {
		return buildEndDate;
	}

	public void setBuildEndDate(Date buildEndDate) {
		this.buildEndDate = buildEndDate;
	}

	/**
	 * Load projects.
	 */
	public void loadProjects() {
		hashMap = new HashMap<String, Project>();
		PrintWriter out = null;
		OutputStream fout = null;
		OutputStream bout = null;
		try {
			File fullBuildManifestFD = new File(getFullBuildManifestXml());
			/*
			if(fullBuildManifestFD.exists()) {
				String updatecmd = "svn update " + bf.getSourceHome()+"/manifest";
				System.out.println("Updating manifest.xml from manifest project");
				System.out.println("Command used is "+updatecmd);
				Executor exec = new Executor(); 
				boolean retval = exec.run(updatecmd);
				if(retval == Constants.FAILURE) {
					System.out.println("Updating manifest code at "+bf.getSourceHome()+"/manifest directory : FAILURE");
					System.exit(1);
				} else {
					System.out.println("Updating manifest code at "+bf.getSourceHome()+"/manifest directory : SUCCESS");
				}
			} else {
				String manifestBranch = "trunk";
				if(! bf.getBranch().contains("trunk")) {
					manifestBranch = "branches/manifest-"+bf.getBranch()+"-BRANCH";
				} 
					String checkoutcmd = "svn co --depth infinity "
							+ SCMFactory.getSVNRepoURL() + "manifest/"
							+ manifestBranch + " " + bf.getSourceHome()+"/manifest";
					System.out.println("Checking out manifest.xml from manifest project into "+bf.getSourceHome()+"/manifest directory : ");
					System.out.println("Command used is "+checkoutcmd);
					Executor exec = new Executor(); 
					exec.setLogLevel(Level.toLevel("DEBUG"));
                        		exec.setShowOutput(true);
					boolean retval = exec.run(checkoutcmd);
					if(retval == Constants.FAILURE) {
						System.out.println("Check out manifest code into "+bf.getSourceHome()+"/manifest directory : FAILURE");
						System.exit(1);
					} else {
						System.out.println("Check out manifest code into "+bf.getSourceHome()+"/manifest directory : SUCCESS");
					}
			}
			*/
			//manifestProject = new Project("manifest", "trunk",bf, false, null);
			
			FileInputStream fis = new FileInputStream(getFullBuildManifestXml());
			Document XMLDoc = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().parse(new InputSource(fis));

			XPath projxp = XPathFactory.newInstance().newXPath();
			NodeList projnl = (NodeList) (projxp.evaluate(
					"/manifest/projects", XMLDoc, XPathConstants.NODESET));
			String commonbranch = projnl.item(0).getAttributes()
					.getNamedItem("commonbranch").getNodeValue();
			String majrel = projnl.item(0).getAttributes()
					.getNamedItem("majorversion").getNodeValue();
			String buildTool = projnl.item(0).getAttributes()
					.getNamedItem("buildtool").getNodeValue();
			String scmType = projnl.item(0).getAttributes()
					.getNamedItem("scmtype").getNodeValue();
			String commonSCMURL = projnl.item(0).getAttributes()
					.getNamedItem("commonscmurl").getNodeValue();
			setCommonBranch(commonbranch);
			setMajorRelease(majrel);
			setCommonSCMURL(commonSCMURL);
			System.out.println("loadProjects called with commonbranch: "+commonbranch+" major release: "+majrel+" buildTool: "+buildTool+" scmType: "+scmType+" commonSCMURL: "+commonSCMURL);

			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList deps = (NodeList) (xpath.evaluate(
					"/manifest/projects/project", XMLDoc,
					XPathConstants.NODESET));
			// Clean up the pomfiles directory
			String pomFilesDir = bf.getSourceHome() + "/pomfiles";
			for (int iter = 0; iter < deps.getLength(); iter++) {
				Node nd = deps.item(iter);

				String projName = nd.getAttributes().getNamedItem("name").getNodeValue();
				String projBranch = nd.getAttributes().getNamedItem("branch").getNodeValue();
				Node targetsnode = nd.getAttributes().getNamedItem("targets");
				String targets = null;
				if(targetsnode != null) {
					targets = targetsnode.getNodeValue();
				}
				if(projBranch == null) { projBranch = commonbranch; }
				String projScmBranch = "";
				if(projBranch.equals("trunk")|| projBranch.contains("-BRANCH")) {
					projScmBranch = projBranch;
				} else {
					projScmBranch = "branches/" + projName+"-"+projBranch+"-BRANCH";
				}
				System.out.println("loadProjects : projBranch is : "+projBranch+" projScmBranch is : "+projScmBranch);
				Project proj = new Project(projName,projScmBranch, bf,false,new String(""));
				
				// If custom targets are passed from command line, use them as the only targets
				if (bf.getCustomTargets().size() != 0) {
					System.out.println("loadProjects : bf.getCustomTargets() is not null. Adding the custom target to the list of targets to be run.\n");
					for (int piter=0; piter < bf.getCustomTargets().size(); piter++) {
						if(!proj.getTargetsVector().contains(bf.getCustomTargets().get(piter))) {
							System.out.println("loadProjects : bf.getCustomTargets() is not null. Adding "+bf.getCustomTargets().get(piter)+".\n");
							proj.addToTargetsVector(bf.getCustomTargets().get(piter));
						}
					}
				} else {
					// Add any other custom targets registered in the manifest.xml
					if(targets != null) {
					System.out.println("loadProjects : bf.getCustomTargets() is null. Adding the targets from manifest.xml to the list of targets to be run.\n");
						String [] targetsParts = targets.split(",");
						for (int piter=0; piter < targetsParts.length; piter++) {
							if(!proj.getTargetsVector().contains(targetsParts[piter])) {
								System.out.println("loadProjects : bf.getCustomTargets() is null. Adding the "+targetsParts[piter]+" to the list of targets to be run.\n");
								proj.addToTargetsVector(targetsParts[piter]);
							}
						}
					} else {
						// If no targets arguments is passed, just use the deploy/install as the target.
						if(bf.getRunMode().equals("qe")) {
							System.out.println("loadProjects : No targets are passed. Using deploy as the target.\n");
							proj.addToTargetsVector("deploy");
						} else {
							System.out.println("loadProjects : No targets are passed. Using install as the target.\n");
							proj.addToTargetsVector("install");
						}
					}
				}
				
				proj.setBranch(projScmBranch);
				proj.setLogLevel(bf.getLogLevel());

				Node projectBuildToolNode = nd.getAttributes().getNamedItem("buildtool");
				if ( (projectBuildToolNode != null) && (!projectBuildToolNode.getNodeValue().equals(buildTool)) ) {
					proj.setBuildTool(projectBuildToolNode.getNodeValue());
				} else {
					proj.setBuildTool(buildTool);
				}
				Node genPkgNode = nd.getAttributes().getNamedItem("genpkg");
				if (genPkgNode != null) {
					proj.setGenPkg(genPkgNode.getNodeValue());
				}
				Node tagNode = nd.getAttributes().getNamedItem("tag");
				if (tagNode != null) {
					proj.setCurrentTag(tagNode.getNodeValue());
					// We set to the New Tag to be same as current tag. 
					// It will be changed if a new tag is generated after successful build.
					proj.setNewTag(tagNode.getNodeValue());
				}
				Node revisionNode = nd.getAttributes().getNamedItem("revision");
				if (revisionNode != null) {
					proj.setCurrentRevision(revisionNode.getNodeValue());
					// We set to the New Revision to be same as current Revision. 
					// It will be changed if a new tag is generated after successful build.
					proj.setNewRevision(revisionNode.getNodeValue());
				}
				Node projectSCMTypeNode = nd.getAttributes().getNamedItem("scmtype");
				if( (projectSCMTypeNode != null ) && (!projectSCMTypeNode.getNodeValue().equals(scmType))) {
					proj.setScmType(projectSCMTypeNode.getNodeValue());
				} else {
					proj.setScmType(scmType);
				}

				Node projectSCMURLNode = nd.getAttributes().getNamedItem("scmurl");
				if ( (projectSCMURLNode != null) && (!projectSCMURLNode.getNodeValue().equals(commonSCMURL+"/branches/"+projBranch)) ) {
					proj.setScmURL(projectSCMURLNode.getNodeValue());
				} else {
					if(projBranch.contains("trunk")) {
						proj.setScmURL(commonSCMURL + projName+"/" + projBranch);
					} else {
						if(!projBranch.contains("-BRANCH")) {
							proj.setScmURL(commonSCMURL + projName+"/branches/" + projName+"-"+projBranch+"-BRANCH");
						} else {
							proj.setScmURL(commonSCMURL + projName+"/"+projBranch);
						}
					}
				}
				System.out.println("loadProjects : ScmURL is : "+proj.getScmURL());

				hashMap.put(projName, proj);
				topLevelProjects.add(proj);
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		} 
	}

	/**
	 * Locate project in the toplevelproject vector.
	 * Logic :
	 * 		1. Takes the passed project name
	 * 		2. Iterates through the entries in the toplevelproject vector
	 * 		3. Returns the projects whose name matches with the passed project name
	 * 
	 * @param passedProj - The passed project name
	 * @return the Project object whose name matches the passed project name.
	 */
	Project locateProject(String passedProj) {
		Enumeration<Project> outerenum = topLevelProjects.elements();
		while (outerenum.hasMoreElements()) {
			Project outerproj = (Project) outerenum.nextElement();
			System.out.println("locateProject : iterating through the toplevelprojects : current element : "+outerproj.getName()+"\n");
			if (outerproj.getName().equals(passedProj)) {
				System.out.println("locateProject : "+passedProj+" matches with "+outerproj.getName()+".\n");
				return outerproj;
			}
		}
		System.out.println("locateProject : Could not locate "+passedProj+" in the manifest.xml.\n");
		return null;
	}

	/**
	 * Gets the top level projects vector.
	 * 
	 * @return the top level projects vector
	 */
	Vector<Project> getTopLevelProjects() {
		return topLevelProjects;
	}

	/**
	 * Load Dependees for each of the projects mentioned in manifest file into the Dependees vector. 
	 * The dependency lists from the Project objects (populated after parsing pom.xml) are used for dependency lookups.
	 */
	void loadDependees() {
		Enumeration<Project> outerenum = topLevelProjects.elements();
		while (outerenum.hasMoreElements()) {
			Project outerproj = (Project) outerenum.nextElement();
			System.out.println("DependencyFactory::loadDependees : Checking for " + outerproj.getArtifactId() + " in all top level projects dependencies.\n");
			Enumeration<Project> innerenum = topLevelProjects.elements();
			while (innerenum.hasMoreElements()) {
				Project innerproj = (Project) innerenum.nextElement();
				if (outerproj.getArtifactId().equals(innerproj.getArtifactId())) {
					// A project cannot be dependent on itself. Skipping.
					System.out.println(innerproj.getArtifactId()+" seems to be dependent on itself. Please check and correct your pom.xml.");
					continue;
				}
				if (innerproj.isDependency(outerproj.getArtifactId())) {
					System.out.println("DependencyFactory::loadDependees : Looks like "
					    + outerproj.getArtifactId() + " is dependency for " + innerproj.getArtifactId());
					Dependency dep = new Dependency();
					dep.setArtifactId(innerproj.getArtifactId());
					dep.setGroupId(innerproj.getGroupId());
					dep.setVersion(innerproj.getVersion());
					System.out.println("DependencyFactory::loadDependees : Adding "
					    + innerproj.getArtifactId() + " as a dependee for " + outerproj.getArtifactId());
					outerproj.addDependee(dep);
				}
			}
		}
		if(bf.getDepTraversal().equals("topdown")) {
			//Populate the customProjects content to TopDownProjects
			for(int i=0; i<bf.customProjects.size();i++) {
				topDownProjects.add(locateProject(bf.customProjects.get(i)));
			}
			HashSet<Project> hsp = getAllDependencies(topDownProjects);
			Iterator<Project> projIter = hsp.iterator();
			while (projIter.hasNext()) {
				Project proj = (Project) projIter.next();
				topLevelProjectsSubset.add(proj);
			}
		} else {
			topLevelProjectsSubset = topLevelProjects;
		}
		System.out.println("loadDependees : Enumerating the topLevelProjectsSubset vector contents");
		Iterator<Project> projIter = topLevelProjectsSubset.iterator();
		while (projIter.hasNext()) {
			Project proj = (Project) projIter.next();
			System.out.println("loadDependees : topLevelProjectSubset entry : "+proj.getName());
		}
		
	}

	/**
	 * Prints the top level projects in the order read from the manifest file.
	 */
	void printTopLevelProjects() {
		System.out.println("Enumerating the manifest.xml vector contents");
		Enumeration<Project> e = topLevelProjects.elements();
		while (e.hasMoreElements()) {
			Project proj = (Project) e.nextElement();
			proj.printProjectDetails();
			proj.printDependencies();
			proj.printDependees();
			System.out.println("");
		}
	}

	/**
	 * Builds the top level projects one-by-one in the same order as mentioned in the manifest.
	 * 
	 * @return true, if all the project builds are successful
	 */
	boolean buildTopLevelProjects() {
		boolean result = Constants.SUCCESS;
		System.out.println("Building the manifest.xml vector contents");
		Enumeration<Project> e = topLevelProjects.elements();
		if(bf.getRunMode().equals("qe")) {
			if(topLevelProjects.size() > 0) {
				String m2SnapshotRoot = "/scratch/build/m2snapshots/LATEST";
				try {
					File snapshotFile = new File(m2SnapshotRoot);
					if (snapshotFile.exists()) {
						System.out.println("Copying the LATEST .m2 snapshot from " 
								+ m2SnapshotRoot + " to " + bf.getSourceHome()+ "/.m2");
						FileUtils.dirCopy(snapshotFile,
								new File(bf.getSourceHome() + "/.m2"));
						System.out.println("Finished the copy of the LATEST .m2 snapshot from "
								+ m2SnapshotRoot+ " to "+ bf.getSourceHome()+ "/.m2");
					}
				} catch (Exception ioexp) {
					ioexp.printStackTrace();
					System.out.println("Error while copying the LATEST .m2 snapshot from "+m2SnapshotRoot+" to "+bf.getSourceHome()+"/.m2");
					result = Constants.FAILURE;
					return result;
				}
			}
		}
		setBuildStartDate(new Date());
		while (e.hasMoreElements()) {
			Project proj = (Project) e.nextElement();
			boolean tempresult = proj.build();
			proj.setBuildSuccess(tempresult);
			if(tempresult != Constants.SUCCESS) {
				failedProjects.add(proj);
			}
			result = result || tempresult;
		}
		if(bf.getRunMode().equals("qe")) {
			// Refreshing manifest file so that we are current when we attempt to commit.
			refreshManifest(); 
			serializeProjectsToManifestXML();
			serializeProjectsToManifestProperties();
			copyPomFilesIntoDCManifest();
			commitManifest();
			buildManifestProject();
			//generateTagForManifestProject(manifestProject.getVersion());
		}
		setBuildEndDate(new Date());
		printBuildStatusOfTopLevelProjects();
		
		return result;
	}

	/**
	 * Prints the build status of top level projects.
	 * Status is taken from each of the project objects.
	 * So, the prerequisite is to have all the project builds done before invoking this method.
	 */
	void printBuildStatusOfTopLevelProjects() {
		System.out.println("#####################################################################");
		System.out.println("####      FINAL BUILD STATUS OF THE PROJECTS DURING FULL BUILD");
		System.out.println("#####################################################################");
		Enumeration<Project> e = topLevelProjects.elements();
		while (e.hasMoreElements()) {
			Project proj = (Project) e.nextElement();
			if(proj.isBuildSuccess()) {
			    System.out.println("##        "+proj.getArtifactId()+" : "+"SUCCESS");
			} else {
			    System.out.println("##        "+proj.getArtifactId()+" : "+"FAILURE");
			}
		}
		System.out.println("#####################################################################\n");
	}

	HashSet<Project> getChangedProjects() {
		return changedProjects;
	}

	void calculateChangedProjects() {
		Enumeration<Project> e = topLevelProjectsSubset.elements();
		
		while (e.hasMoreElements()) {
			Project proj = (Project) e.nextElement();
			if(proj.isChanged()) {
			    System.out.println("getChangedProjects : Code change detected in "+proj.getName()+" project. Adding it to the list of changed projects.");
			    changedProjects.add(proj);
			}
		}
	}

	/**
	 * Prints the affected projects list.
	 */
	void printChangedProjects() {
		System.out.println("    Changed Projects: \n");
		Iterator<Project> projIter = changedProjects.iterator();
		while (projIter.hasNext()) {
			Project tmpproj = (Project) projIter.next();
			System.out.println("        " + tmpproj.getName() + ",");
		}
	}

	/**
	 * Queries if the passed project is in the changed projects or not
	 */
	boolean isInChangedProjects(String passedProject) {
		System.out.println("    isInChangedProjects: Querying if the"+passedProject+" is in the list of changed projects \n");
		Iterator<Project> projIter = changedProjects.iterator();
		while (projIter.hasNext()) {
			Project tmpproj = (Project) projIter.next();
			if (tmpproj.getName().equals(passedProject)) {
			    return true;
			}
		}
		return false;
	}

	/**
	 * Builds the affected ordered projects.
	 * 
	 * @return true, if all the project builds are successful
	 */
	boolean buildAffectedOrderedProjects() {
		boolean result = Constants.SUCCESS;
		if(affectedOrderedProjects.size() <=0 ) {
			System.out.println("Number of affected ordered project is : 0. There is nothing to build. Exiting.");
			return Constants.SUCCESS;
		}
		System.out.println("Building the affected projects:\n");
		Enumeration<Project> e = affectedOrderedProjects.elements();
		if(bf.getRunMode().equals("qe")) {
			if(affectedOrderedProjects.size() > 0) {
				String m2SnapshotRoot = "/scratch/build/m2snapshots/LATEST";
				try {
					System.out.println("Copying the LATEST .m2 snapshot from "+m2SnapshotRoot+" to "+bf.getSourceHome()+"/.m2");
					FileUtils.dirCopy(new File(m2SnapshotRoot), new File(bf.getSourceHome()+"/.m2"));
					System.out.println("Finished the copy of the LATEST .m2 snapshot from "+m2SnapshotRoot+" to "+bf.getSourceHome()+"/.m2");
				} catch (Exception ioexp) {
					ioexp.printStackTrace();
					System.out.println("Error while copying the LATEST .m2 snapshot from "+m2SnapshotRoot+" to "+bf.getSourceHome()+"/.m2");
					result = Constants.FAILURE;
					return result;
				}
			}
		}
		setBuildStartDate(new Date());
		while (e.hasMoreElements()) {
			Project proj = (Project) e.nextElement();
			// If the code build type is developer and the project is NOT in the list of changed projects, dont run unit tests for it.
			if(bf.getRunMode().equals("developer") && (! getChangedProjects().contains(proj)) ) {
				System.out.println("Project "+proj.getName()+" is not part of the changed projects list. Marking NOT to do unit tests.");
				proj.setSkipTests();
			}

			proj.checkoutCode();
			//proj.clean(); // TODO - Add code to skip clean
			boolean tempresult = Constants.SUCCESS;
			tempresult = proj.build();
			if(tempresult != Constants.SUCCESS) {
				failedProjects.add(proj);
			}
			result = result || tempresult;
		}
		if ((bf.getRunMode().equals("qe")) && (affectedOrderedProjects.size() > 0) ) {
			serializeProjectsToManifestXML();
			serializeProjectsToManifestProperties();
			copyPomFilesIntoDCManifest();
			commitManifest();
			buildManifestProject();
			//generateTagForManifestProject(manifestProject.getVersion());
		}
		setBuildEndDate(new Date());
		printBuildStatusOfOrderedAffectedProjects();
		if (bf.getRunMode().equals("qe")) {
			sendMailWithStatusOfOrderedAffectedProjects();
		}
		return result;
	}

	/**
	 * Prints the build status of ordered affected projects.
	 */
	void printBuildStatusOfOrderedAffectedProjects() {
		System.out.println("#####################################################################");
		System.out.println("####      FINAL BUILD STATUS OF THE ORDERED AFFECTED PROJECTS");
		System.out.println("#####################################################################");
		Enumeration<Project> e = affectedOrderedProjects.elements();
		while (e.hasMoreElements()) {
			Project proj = (Project) e.nextElement();
			if(proj.isBuildSuccess()) {
			    System.out.println("##        "+proj.getArtifactId()+" : "+"SUCCESS");
			} else {
			    System.out.println("##        "+proj.getArtifactId()+" : "+"FAILURE");
			}
		}
		System.out.println("#####################################################################\n");
	}
	
	void sendMailWithStatusOfOrderedAffectedProjects() {
		String newManifestVersion = "";
		
		String changedProjectsStr = "";
		Iterator<Project> cpi = changedProjects.iterator();
		while (cpi.hasNext()) {
			if(!changedProjectsStr.equals("")) { changedProjectsStr += ","; }
			changedProjectsStr += ((Project) cpi.next()).getName();
		}
		
		String affectedProjectsStr = "";
		Iterator<Project> api = affectedProjects.iterator();
		while (api.hasNext()) {
			if(!affectedProjectsStr.equals("")) { affectedProjectsStr += ","; }
			affectedProjectsStr += ((Project) api.next()).getName();	
		}
		
		String orderedAffectedProjectsStr = "";
		Enumeration<Project> oape = affectedOrderedProjects.elements();
		while (oape.hasMoreElements()) {
			if(!orderedAffectedProjectsStr.equals("")) { orderedAffectedProjectsStr += ","; }
			orderedAffectedProjectsStr += ((Project) oape.nextElement()).getName();
		}
		
		String failedProjectsStr = "";
		Iterator<Project> fpi = failedProjects.iterator();
		while (fpi.hasNext()) {
			if(!failedProjectsStr.equals("")) { failedProjectsStr += ","; }
			failedProjectsStr += ((Project) fpi.next()).getName();
		}
		
	}
	
	public void sendMail(String subject, String body) {
		try {
		InternetAddress [] toAddresses = new InternetAddress [1];
		int i=0;
		// TODO - Send mail to all affected teams
		toAddresses[i] = new InternetAddress("ramesh.thummala@gmail.com");
		// Send the mail with the formed toAddresses.
		EmailUtil.sendMail(toAddresses,subject,body);
		} catch(AddressException e) {
			System.out.println("sendMail : Invalid Address received.");
		}
	}

	public boolean failedProjects() {
	    boolean failedProjects = Constants.SUCCESS;
		Enumeration<Project> e = affectedOrderedProjects.elements();
		while (e.hasMoreElements()) {
			Project proj = (Project) e.nextElement();
			if(! proj.isBuildSuccess()) {
			    failedProjects = Constants.FAILURE;
			}
		}
		return failedProjects;
	}
	
	HashSet<Project> getAffectedProjects() {
		return affectedProjects;
	}

	/**
	 * Calculates the affected projects for all the changed projects.
	 * If any project code is changed, all its dependees (recursively - up to the leaf nodes) 
	 * are looked up and added to the list of affected projects.
	 */
	void calculateAffectedProjects() {
		if (bf.getDepTraversal().equals("bottomup")) {
			calculateAffectedDependeeProjects(getChangedProjects());
		} 
		if (bf.getDepTraversal().equals("topdown")) {
			HashSet <Project> alldeps = getAllDependencies(getChangedProjects());
			Iterator<Project> projIter = alldeps.iterator();
			while (projIter.hasNext()) {
				Project proj = (Project) projIter.next();
				affectedProjects.add(proj);
			} 
		}
	}

	HashSet <Project> getAllDependencies(HashSet <Project> passedProjects) {
		HashSet <Project> allDependencies = new HashSet<Project> ();
		Iterator<Project> projIter = passedProjects.iterator();
		while (projIter.hasNext()) {
			Project proj = (Project) projIter.next();
		    System.out.println("getAllDependencies:Processing project : " + proj.getName());
			// Get Dependent projects
			Vector<Dependency> depprojects = proj.getJigsawDependencies();
			if(! allDependencies.contains(proj)) {
				allDependencies.add(proj);
			}
			Enumeration<Dependency> e = depprojects.elements();
			while (e.hasMoreElements()) {	
				Project depproj = (Project) locateProject(((Dependency) e
						.nextElement()).getArtifactId());
				System.out.println("getAllDependencies: Processing dependency "+depproj.getName()+" of "+proj.getName());
				if(! allDependencies.contains(depproj)) {
					allDependencies.add(depproj);
				}

				// Iterate through the dependees of these dependee projects
				HashSet<Project> dependeeProjectHash = new HashSet<Project>();
				dependeeProjectHash.add(depproj);
				HashSet <Project> dependeesDependees = getAllDependencies(dependeeProjectHash);
				Iterator<Project> edd = dependeesDependees.iterator();
				while (edd.hasNext()) {
					Project depdepproj = (Project) locateProject(((Project) edd.next()).getArtifactId());
					if(! allDependencies.contains(depdepproj)) {
						allDependencies.add(depdepproj);
					}
				}
			}
		}
		return allDependencies;
	}
	
	void calculateAffectedDependeeProjects(HashSet<Project> chgProjects) {
		System.out.println("getAffectedProjects: Incoming changedProjects HashSet Count is : "
				+ changedProjects.size() + "\n");
		Iterator<Project> projIter = chgProjects.iterator();
		while (projIter.hasNext()) {
			Project proj = (Project) projIter.next();
			System.out.println("getAffectedProjects:Processing project : "
					+ proj.getName());
			// Get Dependent projects
			Vector<Dependency> depprojects = proj.getDependees();

			if (topLevelProjectsSubset.contains(proj)) {
				affectedProjects.add(proj);
			}

			if ((!bf.getRunMode().equals("qe"))
					&& proj.getName().equals("dc_config")) {
				System.out.println("getAllDependencies: This a non-QE build and the project name is dc_config.");
				System.out.println("Skipping the dependee calculation.");
			} else {
				// If skipDependees option is set to true, do not traverse the
				// dependee chain.
				if (!bf.isSkipDependees()) {
					Enumeration<Dependency> e = depprojects.elements();
					while (e.hasMoreElements()) {
						Project depproj = (Project) locateProject(((Dependency) e
								.nextElement()).getArtifactId());
						if (topLevelProjectsSubset.contains(depproj)) {
							affectedProjects.add(depproj);
							// Iterate through the dependees of these dependee
							// projects
							HashSet<Project> dependeeProjectHash = new HashSet<Project>();
							dependeeProjectHash.add(depproj);
							calculateAffectedDependeeProjects(dependeeProjectHash);
						}
					}
				}
			}
		}
	}

	/**
	 * Order affected projects. 
	 * The ordering is based on the manifest ordering.
	 */
	void orderAffectedProjects() {
		// Order it according to the manifest.xml order
		Enumeration<Project> e = topLevelProjects.elements();
		while (e.hasMoreElements()) {
			Project topproj = (Project) e.nextElement();
			if (getAffectedProjects().contains(topproj)) {
				affectedOrderedProjects.add(topproj);
			}
		}
	}

	/**
	 * Prints the affected projects list.
	 */
	void printAffectedProjects() {
		System.out.println("    Affected Projects: \n");
		Iterator<Project> projIter = affectedProjects.iterator();
		while (projIter.hasNext()) {
			Project tmpproj = (Project) projIter.next();
			System.out.println("        " + tmpproj.getName() + ",");
		}
	}

	void printOrderedAffectedProjects() {
		System.out.println("\n");
		System.out.println("    Ordered Affected Projects: \n");
		Iterator<Project> projIter = affectedOrderedProjects.iterator();
		while (projIter.hasNext()) {
			Project tmpproj = (Project) projIter.next();
			System.out.println("        " + tmpproj.getName() + ",");
		}
	}

	/**
	 * Serialize ordered affected projects to xml so that it can be referred to later.
	 * For doing the actual builds, still we use the in-memory orderedAffectedProjects vector
	 * TODO - Move to using the contents of build_projects.xml for doing the project builds for:
	 * 		- That would remove the dependency on the in-memory objects.
	 * 		- Would allow us to implement the resume operation in the future.  
	 *
	 * @param xmlFile - The xml file to which the ordered affected project list should be written to.
	 */
	boolean serializeOrderedAffectedProjectsToXML(String xmlFile) {
		PrintWriter out = null;
		OutputStream fout = null;
		OutputStream bout = null;
		try {
			String charsetName = "UTF-8";

			fout = new FileOutputStream(xmlFile);
			bout = new BufferedOutputStream(fout);
			out = new PrintWriter(new OutputStreamWriter(bout, charsetName),
					true);

			Iterator<Project> projIter = affectedOrderedProjects.iterator();
			out.println("<build>");
			out.println("    <projects>");
			while (projIter.hasNext()) {
				Project proj = (Project) (projIter.next());
				out.println("        <project>" + proj.getName() + "</project>");
			}
			out.println("    </projects>");
			out.println("</build>");
			if (out != null) {
				out.close();
			}
			if (fout != null) {
				fout.close();
			}
			if (bout != null) {
				bout.close();
			}
			return Constants.SUCCESS;
		} catch (Exception e) {
			System.out.println(e);
			return Constants.FAILURE;
		}
	}

	/**
	 * Serialize updated information of all projects to manifest.xml. Commit it once the update is complete.
	 **/
	boolean serializeProjectsToManifestXML() {
		PrintWriter out = null;
		OutputStream fout = null;
		OutputStream bout = null;
		try {
			String charsetName = "UTF-8";

			fout = new FileOutputStream(getManifestXml());
			bout = new BufferedOutputStream(fout);
			out = new PrintWriter(new OutputStreamWriter(bout, charsetName), true);

			Iterator<Project> projIter = topLevelProjects.iterator();
			out.println("<manifest>");
			out.println("    <projects majorversion=\""+getMajorRelease()+"\" commonbranch=\""+getCommonBranch()+"\" buildtool=\"mvn\" scmtype=\"svn\" commonscmurl=\""+getCommonSCMURL()+"\">");

			while (projIter.hasNext()) {
				Project proj = (Project) (projIter.next());
				String project_str = "        <project name=\"" + proj.getName() + "\" branch=\""+proj.getBranch()+"\" ";
				project_str = project_str + " tag=\""+proj.getNewTag()+"\" revision=\""+proj.getNewRevision()+"\" genpkg=\""+proj.getGenPkg()+"\" ";
				if(proj.getTargetsVector().size() > 0) {
					project_str = project_str + " targets=\"";
					Enumeration<String> tgtenum = proj.getTargetsVector().elements();
					while (tgtenum.hasMoreElements()) {
						String tgt  = (String) tgtenum.nextElement();
						project_str = project_str + tgt+",";
					}
					// Remove the last trailing comma.
					project_str = project_str.substring(0, project_str.length() - 1);
					project_str = project_str + "\" ";
				}
				project_str = project_str + "/>";
				out.println(project_str);
			}
			out.println("    </projects>");
			out.println("</manifest>");
			if (out != null) {
				out.close();
			}
			if (fout != null) {
				fout.close();
			}
			if (bout != null) {
				bout.close();
			}
			return Constants.SUCCESS;
		} catch (Exception e) {
			System.out.println(e);
			return Constants.FAILURE;
		}
	}
	
	boolean copyPomFilesIntoDCManifest() {
		boolean retval = Constants.SUCCESS;
		String srcPomFilesDir = bf.getSourceHome()+"/pomfiles";
		String destPomFilesDir = bf.getSourceHome()+"/dcmanifest/src/main/resources";
		try {
			System.out.println("copyPomFilesIntoDCManifest: Copying pom.xml from "+srcPomFilesDir+" to "+destPomFilesDir);
			
			Executor exec = new Executor();
			exec.setLogLevel(Level.toLevel("DEBUG"));
			exec.setShowOutput(true);
			
			String svncmd = "cp -Rfp "+srcPomFilesDir+" "+destPomFilesDir;
			retval = exec.run(svncmd);
			svncmd = " rm -f "+destPomFilesDir+"/pomfiles/*/*.txt";
			boolean retval1 = exec.run(svncmd);
			svncmd = " rm -f "+destPomFilesDir+"/pomfiles/*/grid*.xml";
			retval1 = exec.run(svncmd);
			svncmd = " rm -f "+destPomFilesDir+"/pomfiles/*/build*.xml";
			retval1 = exec.run(svncmd);
			svncmd = " rm -rf "+destPomFilesDir+"/pomfiles/*/.svn";
			retval1 = exec.run(svncmd);
		} catch(Exception e) {
			e.printStackTrace();
			return Constants.FAILURE;
		}
		
		return retval;
	}
        boolean serializeProjectsToManifestProperties() {
                PrintWriter out = null;
                OutputStream fout = null;
                OutputStream bout = null;
                try {  
                        String charsetName = "UTF-8";

                        fout = new FileOutputStream(getManifestProperties());
                        bout = new BufferedOutputStream(fout);
                        out = new PrintWriter(new OutputStreamWriter(bout, charsetName), true);

                        Iterator<Project> projIter = topLevelProjects.iterator();

                        while (projIter.hasNext()) {
                                Project proj = (Project) (projIter.next());
                                String project_str = proj.getGroupId() + "." + proj.getName() + ".version="+proj.getVersion();
				out.println(project_str);
                        }
                        if (out != null) {
                                out.close();
                        }
                        if (fout != null) {
                                fout.close();
                        }
                        if (bout != null) {
                                bout.close();
                        }
                        return Constants.SUCCESS;
                } catch (Exception e) {
                        System.out.println(e);
                        return Constants.FAILURE;
                }
        }

	boolean refreshManifest() {
		boolean retval = true;
		try {
			System.out.println("Refreshing the manifest at "+getManifestXml());
			String svncmd = "svn update "+bf.getSourceHome()+"/dcmanifest";
			Executor exec = new Executor();
			exec.setLogLevel(Level.toLevel("DEBUG"));
			exec.setShowOutput(true);
			retval = exec.run(svncmd);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return retval;
	}
	
	boolean commitManifest() {
		boolean retval = true;
		try {
			File manifestCommitCommentFile = new File("manifestcommitcomments.txt");
			if (!manifestCommitCommentFile.exists()) {
				manifestCommitCommentFile.createNewFile();
			}
			FileWriter commentWriter = new FileWriter(manifestCommitCommentFile.getAbsoluteFile());
			BufferedWriter commentBufferWriter = new BufferedWriter(commentWriter);
			commentBufferWriter.write("Automated Manifest update");
			commentBufferWriter.close();

			System.out.println("Committing the updated "+getManifestXml()+" and "+getManifestProperties());
			String svncmd = "svn commit --file "+manifestCommitCommentFile+" "+getManifestXml()+" "+getManifestProperties();
			Executor exec = new Executor();
			exec.setLogLevel(Level.toLevel("DEBUG"));
			exec.setShowOutput(true);
			retval = exec.run(svncmd);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return retval;
	}

	boolean buildManifestProject() {
		boolean retval = true;
		try {
			System.out.println("Building the dcmanifest project.");
			String buildcmd = "";
                        if(bf.getRunMode().equals("qe")) {
                            String localRepoLoc = System.getProperty("maven.repo.local");
                            if(localRepoLoc == null) {
                                localRepoLoc = bf.getSourceHome()+"/.m2";
                            }
                            buildcmd = System.getenv("MAVEN_HOME") + "/bin/mvn -B -U -Dmaven.repo.local="+localRepoLoc+" -f " + bf.getSourceHome() + "/dcmanifest/pom.xml deploy -DskipTests";
                        }
			Executor exec = new Executor();
			exec.setLogLevel(Level.toLevel("DEBUG"));
			exec.setShowOutput(true);
			retval = exec.run(buildcmd);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return retval;
	}

}
