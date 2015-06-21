package org.docker.hackathon.depbuilder;

import java.io.*;
import java.util.*;

import org.docker.hackathon.depbuilder.Constants;
import org.docker.hackathon.depbuilder.BuildConfig;
import org.docker.hackathon.depbuilder.Project;
import org.docker.hackathon.depbuilder.DependencyFactory;

/**
 * The Builder class is the main class that invokes the logic for determining the projects to be built and invokes builds for each of them.
 * The actual implementation methods are implemented in <code>DependencyFactory</code> class.
 */
public class Builder {
	
	DependencyFactory depFactory;
	
	/**
	 * Do build of all affected projects for a given BuildConfig object.
	 *
	 * @param passedBConf - The passed BuildConfig Object
	 * @return true, if successful
	 */
	public Builder (BuildConfig passedBConf) {
		// Logic to calculate changed and affected projects
		depFactory = new DependencyFactory(passedBConf);
		depFactory.loadProjects();
		depFactory.loadDependees();
		depFactory.printTopLevelProjects();

		 if(passedBConf.buildMode.equals("incr")) {
			if(passedBConf.getRunMode().equals("developer")) {
				// See if there are any projects to be built.
				if(passedBConf.customProjects.isEmpty()) { 
					System.out.println("You have chosen incremental build type but no project arguments are passed to be built. Exiting."); 
				 	System.exit(0); 
		     		} 
			 	// If the build mode is incremental build, set the projects xml to be build_projects.xml 
			 	// Otherwise, it will be the default i.e. full build.
				for (int i = 0; i < passedBConf.customProjects.size(); i++) {
					String tempproj = passedBConf.customProjects.get(i);
					System.out.println("Processing passedProject :    "+ tempproj + "\n");
					depFactory.getChangedProjects().add(depFactory.locateProject(tempproj));
				}
			}

			// For QE builds, we always calculate what has changed in the repos from last generated tags.
			if(passedBConf.getRunMode().equals("qe")) {
				if(passedBConf.getDepTraversal().equals("topdown")) {
					// For topdown mode, we need to populate the topDownProjects HashSet in Dependency Factory
					for (int i = 0; i < passedBConf.customProjects.size(); i++) {
						String tempproj = passedBConf.customProjects.get(i);
						System.out.println("Processing passedProject :    "+ tempproj + "\n");
						depFactory.getTopDownProjects().add(depFactory.locateProject(tempproj));
					}
				}
				depFactory.calculateChangedProjects();
				depFactory.printChangedProjects();
			}

			depFactory.calculateAffectedProjects();
			depFactory.printAffectedProjects();

			depFactory.orderAffectedProjects();
			depFactory.printOrderedAffectedProjects();

			String buildProjectsXML = passedBConf.getSourceHome()+ "/build_projects.xml";
			depFactory.serializeOrderedAffectedProjectsToXML(buildProjectsXML);
			passedBConf.projectsXML=buildProjectsXML;
		 }
	}
	
	public Project getProject(String projectName) {
		return depFactory.locateProject(projectName);
	}
		 
	public boolean doBuild(BuildConfig passedBConf) {
		boolean result = Constants.SUCCESS;
		 try { 
			 // Create <SRCHOME>/buildlogs directory if it does not exist. 
			 File buildLogs = new File(passedBConf.getSourceHome()+"/buildlogs");
			 buildLogs.mkdirs();
		 
			 if(passedBConf.buildFlow.equals("parallel")) { 
				 // TODO - Add code for parallel builds - Right now, by default, the builds are serial
				System.out.println("The parallel buildFlow is not yet implemented. Please use serial mode for timebeing.");
				 return result;
			 }
			 passedBConf.printConfig();
			 if(passedBConf.buildFlow.equals("serial")) {
				if(passedBConf.buildMode.equals("incr")) {
					// TODO - Read from bconf.m_projectsXML and build the projects mentioned in there.
					result = depFactory.buildAffectedOrderedProjects();
				} else {
					 // Do a full build.
					 result = depFactory.buildTopLevelProjects();
				}
				return result;
				// TODO - Right now, we use separate mvn command invocations to do the builds.
				// Use one JVM to do all the project maven builds.
			 }
		 } catch(Exception e) { 
			 e.printStackTrace(); 
		 }
		 return Constants.SUCCESS;
	}
	
	public boolean failedProjects() {
		return depFactory.failedProjects();
	}
}
