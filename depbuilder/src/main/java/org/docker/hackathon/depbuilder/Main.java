package org.docker.hackathon.depbuilder;

import java.io.*;
import java.util.*;

import org.docker.hackathon.depbuilder.BuildConfig;
import org.docker.hackathon.depbuilder.Project;
import org.docker.hackathon.depbuilder.DependencyFactory;
import org.docker.hackathon.util.FileUtils;

/**
 * The Main class is a wrapper into the Builder. It just formulates the BuildConfig object based on the arguments passed by the user and invokes the Builder on that BuildConfig.
 */
public class Main {
	
	/**
	 * The main method (entry point) for the standalone java builder.
	 *
	 * @param args - The arguments passed for standalone invocation.
	 */
	public static void main(String[] args) {
		BuildConfig bconf = new BuildConfig();
		bconf.parseArgs(args);
		Builder builder = new Builder(bconf);
		builder.doBuild(bconf);
	}
}
