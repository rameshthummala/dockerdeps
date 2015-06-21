package org.docker.hackathon.util;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.stax2.XMLInputFactory2;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.docker.hackathon.util.ModifiedPomXMLEventReader;
import org.docker.hackathon.util.NexusUtils;
import org.docker.hackathon.depbuilder.Constants;

/**
 * @author rthummalapenta
 *
 */
public class PomUtils {

	static Logger logger = Logger.getLogger(PomUtils.class.getName());
	/**
	 * Gets the raw model before any interpolation what-so-ever.
	 *
	 * @param project The project to get the raw model for.
	 * @return The raw model.
	 * @throws IOException if the file is not found or if the file does not parse.
	 */
	public static Model getRawModel( MavenProject project )
			throws IOException
			{
		return getRawModel( project.getFile() );
			}

	/**
	 * Gets the raw model before any interpolation what-so-ever.
	 *
	 * @param moduleProjectFile The project file to get the raw model for.
	 * @return The raw model.
	 * @throws IOException if the file is not found or if the file does not parse.
	 */
	public static Model getRawModel( File moduleProjectFile )
			throws IOException
			{
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try
		{
			fileReader = new FileReader( moduleProjectFile );
			bufferedReader = new BufferedReader( fileReader );
			MavenXpp3Reader reader = new MavenXpp3Reader();
			return reader.read( bufferedReader );
		}
		catch ( XmlPullParserException e )
		{
			IOException ioe = new IOException( e.getMessage() );
			ioe.initCause( e );
			throw ioe;
		}
		finally
		{
			if ( bufferedReader != null )
			{
				bufferedReader.close();
			}
			if ( fileReader != null )
			{
				fileReader.close();
			}
		}
			}

	/**
	 * Gets the current raw model before any interpolation what-so-ever.
	 *
	 * @param modifiedPomXMLEventReader The {@link ModifiedPomXMLEventReader} to get the raw model for.
	 * @return The raw model.
	 * @throws IOException if the file is not found or if the file does not parse.
	 */
	public static Model getRawModel( ModifiedPomXMLEventReader modifiedPomXMLEventReader )
			throws IOException
			{
		StringReader stringReader = null;
		try
		{
			stringReader = new StringReader( modifiedPomXMLEventReader.asStringBuilder().toString() );
			MavenXpp3Reader reader = new MavenXpp3Reader();
			return reader.read( stringReader );
		}
		catch ( XmlPullParserException e )
		{
			IOException ioe = new IOException( e.getMessage() );
			ioe.initCause( e );
			throw ioe;
		}
		finally
		{
			if ( stringReader != null )
			{
				stringReader.close();
			}
		}
			}

	/**
	 * Searches the pom re-defining the project version to the specified version.
	 *
	 * @param pom   The pom to modify.
	 * @param value The new value of the property.
	 * @return <code>true</code> if a replacement was made.
	 * @throws XMLStreamException if something went wrong.
	 */
	public static boolean setProjectVersion( final ModifiedPomXMLEventReader pom, final String value )
			throws XMLStreamException
			{
		Stack<String> stack = new Stack<String>();
		String path = "";
		final Pattern matchScopeRegex;
		boolean madeReplacement = false;
		matchScopeRegex = Pattern.compile( "/project/version" );

		pom.rewind();

		while ( pom.hasNext() )
		{
			XMLEvent event = pom.nextEvent();
			if ( event.isStartElement() )
			{
				stack.push( path );
				path = path + "/" + event.asStartElement().getName().getLocalPart();

				if ( matchScopeRegex.matcher( path ).matches() )
				{
					pom.mark( 0 );
				}
			}
			if ( event.isEndElement() )
			{
				if ( matchScopeRegex.matcher( path ).matches() )
				{
					pom.mark( 1 );
					if ( pom.hasMark( 0 ) && pom.hasMark( 1 ) )
					{
						pom.replaceBetween( 0, 1, value );
						madeReplacement = true;
					}
					pom.clearMark( 0 );
					pom.clearMark( 1 );
				}
				path = stack.pop();
			}
		}
		return madeReplacement;
			}

	public boolean setProjectVersion(File pomFile, String version) {
		boolean retval = true;
		try {  
			StringBuilder input = readXmlFile(pomFile);
			ModifiedPomXMLEventReader newPom = newModifiedPomXER( input );
			retval = setProjectVersion(newPom,version);
			if ( newPom.isModified() )
			{
				writeFile( pomFile, input );
			}
			return retval ;
		} catch (Exception e) {
			System.out.println(e);
		}
		return retval;
	}


	/**
	 * Retrieves the project version from the pom.
	 *
	 * @param pom The pom.
	 * @return the project version or <code>null</code> if the project version is not defined (i.e. inherited from parent version).
	 * @throws XMLStreamException if something went wrong.
	 */
	public static String getProjectVersion( final ModifiedPomXMLEventReader pom )
			throws XMLStreamException
			{
		Stack<String> stack = new Stack<String>();
		String path = "";
		final Pattern matchScopeRegex = Pattern.compile( "/project/version" );

		pom.rewind();

		while ( pom.hasNext() )
		{
			XMLEvent event = pom.nextEvent();
			if ( event.isStartElement() )
			{
				stack.push( path );
				path = path + "/" + event.asStartElement().getName().getLocalPart();

				if ( matchScopeRegex.matcher( path ).matches() )
				{
					pom.mark( 0 );
				}
			}
			if ( event.isEndElement() )
			{
				if ( matchScopeRegex.matcher( path ).matches() )
				{
					pom.mark( 1 );
					if ( pom.hasMark( 0 ) && pom.hasMark( 1 ) )
					{
						return pom.getBetween( 0, 1 ).trim();
					}
					pom.clearMark( 0 );
					pom.clearMark( 1 );
				}
				path = stack.pop();
			}
		}
		return null;
			}

	public boolean setProjectParentVersion( File pomFile, final String version )
			throws XMLStreamException
			{
		boolean retval = true;
		try {  
			StringBuilder input = readXmlFile(pomFile);
			ModifiedPomXMLEventReader newPom = newModifiedPomXER( input );
			retval = setProjectParentVersion(newPom,version);
			if ( newPom.isModified() )
			{
				writeFile( pomFile, input );
			}
			return retval ;
		} catch (Exception e) {
			System.out.println(e);
		}
		return retval;
			}
	/**
	 * Searches the pom re-defining the project version to the specified version.
	 *
	 * @param pom   The pom to modify.
	 * @param value The new value of the property.
	 * @return <code>true</code> if a replacement was made.
	 * @throws XMLStreamException if somethinh went wrong.
	 */
	public static boolean setProjectParentVersion( final ModifiedPomXMLEventReader pom, final String value )
			throws XMLStreamException
			{
		Stack<String> stack = new Stack<String>();
		String path = "";
		final Pattern matchScopeRegex;
		boolean madeReplacement = false;
		matchScopeRegex = Pattern.compile( "/project/parent/version" );

		pom.rewind();

		while ( pom.hasNext() )
		{
			XMLEvent event = pom.nextEvent();
			if ( event.isStartElement() )
			{
				stack.push( path );
				path = path + "/" + event.asStartElement().getName().getLocalPart();

				if ( matchScopeRegex.matcher( path ).matches() )
				{
					pom.mark( 0 );
				}
			}
			if ( event.isEndElement() )
			{
				if ( matchScopeRegex.matcher( path ).matches() )
				{
					pom.mark( 1 );
					if ( pom.hasMark( 0 ) && pom.hasMark( 1 ) )
					{
						pom.replaceBetween( 0, 1, value );
						madeReplacement = true;
					}
					pom.clearMark( 0 );
					pom.clearMark( 1 );
				}
				path = stack.pop();
			}
		}
		return madeReplacement;
			}

	/**
	 * Searches the pom re-defining the specified dependency to the specified version.
	 *
	 * @param pom        The pom to modify.
	 * @param groupId    The groupId of the dependency.
	 * @param artifactId The artifactId of the dependency.
	 * @param oldVersion The old version of the dependency.
	 * @param newVersion The new version of the dependency.
	 * @return <code>true</code> if a replacement was made.
	 * @throws XMLStreamException if somethinh went wrong.
	 */
	public static boolean setDependencyVersion( final ModifiedPomXMLEventReader pom, final String groupId,
			final String artifactId, final String oldVersion,
			final String newVersion )
					throws XMLStreamException
					{
		String path = "";
		Stack<String> stack = new Stack<String>();
		path = "";
		boolean inMatchScope = false;
		boolean madeReplacement = false;
		boolean haveGroupId = false;
		boolean haveArtifactId = false;
		boolean haveOldVersion = false;

		if (artifactId.equals("dcinteg")) { 
			// Return back with success since the dcinteg update is handled with a separate updateParentVersion call.
			madeReplacement = true;
			return madeReplacement;
		}

		final Pattern matchScopeRegex = Pattern.compile( "/project" + "(/profiles/profile)?" +
				"((/dependencyManagement)|(/build(/pluginManagement)?/plugins/plugin))?"
				+ "/dependencies/dependency" );

		final Pattern matchTargetRegex = Pattern.compile( "/project" + "(/profiles/profile)?" +
				"((/dependencyManagement)|(/build(/pluginManagement)?/plugins/plugin))?"
				+ "/dependencies/dependency" +
				"((/groupId)|(/artifactId)|(/version))" );

		pom.rewind();

		logger.trace("updateDependencyVersion : Iterating through the pom elements.");

		while ( pom.hasNext() )
		{
			XMLEvent event = pom.nextEvent();
			if ( event.isStartElement() )
			{
				stack.push( path );
				final String elementName = event.asStartElement().getName().getLocalPart();
				path = path + "/" + elementName;

				logger.trace("getDependencyVersion : current element path is "+path);

				if ( matchScopeRegex.matcher( path ).matches() )
				{
					// we're in a new match scope
					// reset any previous partial matches
					inMatchScope = true;
					pom.clearMark( 0 );
					pom.clearMark( 1 );

					haveGroupId = false;
					haveArtifactId = false;
					haveOldVersion = false;
					logger.trace("updateDependencyVersion : current element seems to be a dependency node.");
				}
				else if ( inMatchScope && matchTargetRegex.matcher( path ).matches() )
				{
					if ( "groupId".equals( elementName ) )
					{
						logger.trace("updateDependencyVersion : Found  a groupId element.");
						haveGroupId = groupId.equals(pom.getElementText().trim());
						path = stack.pop();
					}
					else if ( "artifactId".equals( elementName ) )
					{
						logger.trace("updateDependencyVersion : Found an artifactId element.");
						haveArtifactId = artifactId.equals(pom.getElementText().trim());
						path = stack.pop();
					}
					else if ( "version".equals( elementName ) )
					{
						logger.trace("updateDependencyVersion : Found a version element.");
						pom.mark( 0 );
					}
				}
			}
			if ( event.isEndElement() )
			{
				if ( matchTargetRegex.matcher( path ).matches() && "version".equals(
						event.asEndElement().getName().getLocalPart() ) )
				{
					pom.mark( 1 );
					String compressedPomVersion = StringUtils.deleteWhitespace( pom.getBetween( 0, 1 ).trim() );
					String compressedOldVersion = StringUtils.deleteWhitespace( oldVersion );
					haveOldVersion = compressedOldVersion.equals( compressedPomVersion );
				}
				else if ( matchScopeRegex.matcher( path ).matches() )
				{

					if ( inMatchScope && pom.hasMark( 0 ) && pom.hasMark( 1 ) &&
							haveGroupId && haveArtifactId /* && haveOldVersion */)
					{
						logger.trace("updateDependencyVersion : Found the correct version element text to replace. Replacing it.");
						pom.replaceBetween( 0, 1, newVersion );
						madeReplacement = true;
					}
					pom.clearMark( 0 );
					pom.clearMark( 1 );
					haveArtifactId = false;
					haveGroupId = false;
					haveOldVersion = false;
					inMatchScope = false;
				}
				path = stack.pop();
			}
		}
		logger.trace("updateDependencyVersion : returning back after update.");
		return madeReplacement;
					}

	/**
	 * Reads a file into a String.
	 *
	 * @param outFile The file to read.
	 * @return String The content of the file.
	 * @throws java.io.IOException when things go wrong.
	 */
	public static StringBuilder readXmlFile( File outFile )
			throws IOException
			{
		Reader reader = ReaderFactory.newXmlReader( outFile );

		try
		{
			return new StringBuilder( IOUtil.toString( reader ) );
		}
		finally
		{
			IOUtil.close( reader );
		}
			}

	/**
	 * Processes the specified file. This is an extension point to allow updating a file external to the reactor.
	 *
	 * @param outFile The file to process.
	 * @throws MojoExecutionException If things go wrong.
	 * @throws MojoFailureException   If things go wrong.
	 */
	public boolean updateDependencyVersions( File pomFile, Vector<Dependency> dependencies , String projVersion) throws Exception
	{
		boolean generateBackupPoms = false;
		boolean retval = true;
		try
		{
			StringBuilder input = readXmlFile( pomFile );
			ModifiedPomXMLEventReader newPom = newModifiedPomXER( input );

			Iterator<Dependency> i = dependencies.iterator();
			
			String [] projVersionParts = Pattern.compile(".", Pattern.LITERAL).split(projVersion.replaceAll("-","."));
			String projMajorVersion = projVersionParts[0]+"."+projVersionParts[1];

			while ( i.hasNext() )
			{
				Dependency dep = (Dependency) i.next();
				String version = dep.getVersion();
				// If the version string does not contain a $ character i.e. not a maven variable, then update with latest dep version
				if ( ! version.contains("$") ) {
					String [] depVersionParts = Pattern.compile(".", Pattern.LITERAL).split(dep.getVersion().replaceAll("-","."));
					String depMajorVersion = depVersionParts[0]+"."+depVersionParts[1];
					if(!depMajorVersion.equals(projMajorVersion)) {
						// If the project major version is different from dependency major version, query for latest Tag in the projMajorVersion series.
						logger.debug("updateDependencyVersions : "+pomFile+" : Project major version "+projMajorVersion+" is different from dependency version : "+depMajorVersion+". Querying for the latest version in "+depMajorVersion+" series.");
						logger.debug("If you think latest version in "+projMajorVersion+" needs to be picked up, please adjust the pom.xml to use the "+projMajorVersion+" dependencies.");
					}
					String artifactId = dep.getArtifactId();
					String groupId = dep.getGroupId();
					logger.trace( "Looking for newer versions of " + artifactId);
					String newVersion = NexusUtils.getLatestArtifactVersion(groupId,artifactId, depMajorVersion);
					logger.debug("Latest "+artifactId+" version in Nexus : "+newVersion+" . Present version in pom.xml is : "+version);
					VersionComparator vc = new VersionComparator();
					//int compResult = newVersion.compareTo(version);
					int compResult = vc.compare(newVersion, version);
					logger.trace("Comparison result between "+newVersion+" and "+version+" is :"+compResult);
					if ( newVersion.length() > 0 && (compResult > 0) ) 
					{
						if (dep.getArtifactId().equals("dcinteg") || dep.getArtifactId().equals("bluemaster") || dep.getArtifactId().equals("tparty") ||
					    	dep.getArtifactId().equals("dc_newton") || dep.getArtifactId().equals("dc_testutils") || dep.getArtifactId().equals("dc_mycontacts") ) {
							// Update parent version
							setProjectParentVersion(newPom,newVersion);
						} else {
							retval = setDependencyVersion( newPom, dep.getGroupId(), dep.getArtifactId(), version, newVersion );
							if(retval == Constants.SUCCESS)
							{
								logger.debug( "updateDependencyVersions : "+pomFile+" : Updated " + artifactId + " version from "+version+" to " + newVersion );
							} else {
								logger.debug( "updateDependencyVersions : "+pomFile+" : Error while updating " + artifactId + " version from "+version+" to " + newVersion );
								return retval;
							}
						}
					} else {
						logger.debug( "Skipping " + artifactId + " version update since version in pom.xml("+version+") and latest Nexus version("+newVersion+") are the same.");
					}
				}
			}

			if ( newPom.isModified() )
			{
				if ( Boolean.FALSE.equals( generateBackupPoms ) )
				{
					logger.info( "Skipping generation of backup file" );
				}
				else
				{
					File backupPomFile = new File( pomFile.getParentFile(), pomFile.getName() + ".versionsBackup" );
					if ( !backupPomFile.exists() )
					{
						logger.info( "Backing up " + pomFile + " to " + backupPomFile );
						FileUtils.copyFile( pomFile, backupPomFile );
					}
					else
					{
						logger.info( "Leaving existing backup " + backupPomFile + " unmodified" );
					}
				}
				writeFile( pomFile, input );
				return retval;
			}
		}
		catch ( IOException e )
		{
			logger.error( e );
			e.printStackTrace();
		}
		catch ( XMLStreamException e )
		{
			logger.error( e );
			e.printStackTrace();
		}
		catch ( Exception e )
		{
			throw new Exception( e.getMessage(), e );
		}
		return retval;

	}

	/**
	 * Creates a {@link org.docker.hackathon.util.ModifiedPomXMLEventReader} from a StringBuilder.
	 *
	 * @param input The XML to read and modify.
	 * @return The {@link org.docker.hackathon.util.ModifiedPomXMLEventReader}.
	 */
	protected final ModifiedPomXMLEventReader newModifiedPomXER( StringBuilder input )
	{
		ModifiedPomXMLEventReader newPom = null;
		try
		{
			XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
			inputFactory.setProperty( XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE );
			newPom = new ModifiedPomXMLEventReader( input, inputFactory );
		}
		catch ( XMLStreamException e )
		{
			logger.error( e );
		}
		return newPom;
	}

	/**
	 * Writes a StringBuilder into a file.
	 *
	 * @param outFile The file to read.
	 * @param input   The contents of the file.
	 * @throws IOException when things go wrong.
	 */
	protected final void writeFile( File outFile, StringBuilder input )
			throws IOException
			{
		Writer writer = WriterFactory.newXmlWriter( outFile );
		try
		{
			IOUtil.copy( input.toString(), writer );
		}
		finally
		{
			IOUtil.close( writer );
		}
			}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Dependency dep = new Dependency();
		dep.setArtifactId("jdp");
		dep.setGroupId("org.docker.hackathon");
		dep.setVersion("1.0.1-1");
		Vector <Dependency> depvec = new Vector <Dependency> ();
		depvec.add(dep);
		PomUtils pomu = new PomUtils();
		try {
			pomu.updateDependencyVersions(new File("/jhome/rthummalapenta/work/trunk6/app2/pom.xml"),depvec,"1.0.2-1");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

}
