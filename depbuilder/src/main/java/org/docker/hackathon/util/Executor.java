package org.docker.hackathon.util;

import java.io.*;
import java.util.*;
import java.text.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import org.docker.hackathon.depbuilder.Constants;

class StreamGobbler extends Thread
{
    InputStream is;
    String type;
    OutputStream os;
    
    StreamGobbler(InputStream is, String type)
    {
        this(is, type, null);
    }
    StreamGobbler(InputStream is, String type, OutputStream redirect)
    {
        this.is = is;
        this.type = type;
        this.os = redirect;
    }
    
    public void run()
    {
        try
        {
            PrintWriter pw = null;
            if (os != null)
                pw = new PrintWriter(os);
                
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
            {
                if (pw != null)
                    pw.println(line);
                System.out.println(type + ">" + line);    
            }
            if (pw != null)
                pw.flush();
        } catch (IOException ioe)
            {
            ioe.printStackTrace();  
            }
    }
}

/**
 * The ExecUtils class performs the OS specific command line execution. 
 */
public class Executor {
	
	/** The logger. */
	static Logger logger = Logger.getLogger(Executor.class.getName());
	String command = new String ();
	boolean showOutput = false;
	boolean logOutputEnabled = false;
	File outputLog = new File("CmDoUtPuTlOg");
	String osName;
	Runtime rt;

	public Executor() {
		osName = System.getProperty("os.name");
		rt = Runtime.getRuntime();
		logger.setLevel(Level.toLevel("INFO"));
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String passedCommand) {
		command = passedCommand;
	}

	public boolean getShowOutput() {
		return showOutput;
	}

	public void setShowOutput(boolean passedShowOutput) {
		showOutput = passedShowOutput;
	}

	public Level getLogLevel() {
		return logger.getLevel();
	}

	public void setLogLevel(Level passedLevel) {
		logger.setLevel(passedLevel);
	}
	
	public boolean isLogOutputEnabled() {
		return logOutputEnabled;
	}

	public void setLogOutputEnabled(boolean logOutputEnabled) {
		this.logOutputEnabled = logOutputEnabled;
	}

    public File getOutputLog() {
		return outputLog;
	}

	public void setOutputLog(File outputLog) {
		this.outputLog = outputLog;
	}

	public boolean run() {
		return run(getCommand());
	}

    	public boolean run(String passedCommand) {
		int retval = 1;
		setCommand(passedCommand);
		try {
			String[] cmd;
			Process p2;
			FileWriter commandOutputFileWriter = null;
			BufferedWriter commandOutputBufferWriter = null;
			FileOutputStream fos = null;
			if(isLogOutputEnabled()) {
                        commandOutputFileWriter = new FileWriter(outputLog.getAbsoluteFile());
			fos = new FileOutputStream(outputLog.getAbsoluteFile());
                        commandOutputBufferWriter = new BufferedWriter(commandOutputFileWriter);
			}
			if (osName.contains("Windows")) {
				cmd = new String[3];
				cmd[0] = "cmd.exe";
				cmd[1] = "/C";
				cmd[2] = passedCommand;
				logger.debug("Running command : " + getCommand());
				p2 = rt.exec(cmd);
			} else {
				logger.debug("Running command : " + getCommand());
				p2 = rt.exec(getCommand());
			}
	    // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(p2.getErrorStream(), "ERROR", fos);            
            
            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(p2.getInputStream(), "OUTPUT", fos);
                
            // kick them off
            errorGobbler.start();
            outputGobbler.start();
	    /*
			BufferedReader descInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));
			String descline = "";
			while ((descline = descInput.readLine()) != null) {
			   if(showOutput) {
				logger.info(descline);
			   } 
			   if(logOutputEnabled) {
				if(outputLog != null) {
					if(commandOutputBufferWriter != null) {
                           		    commandOutputBufferWriter.write(descline+"\n");
					}
				}
			   } 
			}
			BufferedReader descError = new BufferedReader(new InputStreamReader(p2.getErrorStream()));
			while ((descline = descInput.readLine()) != null) {
			   if(showOutput) {
				logger.info(descline);
			   } 
			   if(logOutputEnabled) {
				if(outputLog != null) {
					if(commandOutputBufferWriter != null) {
                           		    commandOutputBufferWriter.write(descline+"\n");
					}
				}
			   } 
			}
		*/
			int exitVal = p2.waitFor();
			retval = p2.exitValue();
			logger.debug("ExitValue from invocation of " + passedCommand + ": " + exitVal + "\n");
			if(isLogOutputEnabled()) {
				if(commandOutputBufferWriter != null) {
                      commandOutputBufferWriter.close();
				}
				if(commandOutputFileWriter != null) {
                      commandOutputFileWriter.close();
				}
			}
			if (retval != 0) {
				return Constants.FAILURE;
			} else {
				return Constants.SUCCESS;
			}
		} catch (Exception e) {
			System.out.println("[IOException]. Printing Stack Trace");
			e.printStackTrace();
		}
		if (retval != 0) {
			return Constants.FAILURE;
		} else {
			return Constants.SUCCESS;
		}
	}

    public List<String> runWithOutput(String passedCommand) {
		List<String> output = new ArrayList<String>();
		BufferedReader descInput = null;
		try {
			String osName = System.getProperty("os.name");
			String[] cmd;
			Process p2;
			if (osName.contains("Windows")) {
				cmd = new String[3];
				cmd[0] = "cmd.exe";
				cmd[1] = "/C";
				cmd[2] = passedCommand;
				logger.debug("Running command : " + passedCommand);
				p2 = rt.exec(cmd);
			} else {
				logger.debug("Running command : " + passedCommand);
				p2 = rt.exec(passedCommand);
			}

			descInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));
			String descline = "";
			while ((descline = descInput.readLine()) != null) {
				output.add(descline);
			}
    			// wait for the process to complete
			int exitVal = p2.waitFor();

			logger.debug("ExitValue from invocation of " + passedCommand + ": " + exitVal + "\n");
			return output;
		} catch (Exception e) {
			System.out.println("[IOException]. Printing Stack Trace");
			e.printStackTrace();
		} finally {
			try {
				if (descInput != null) descInput.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return output;
	}
    
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		logger.debug("Doing maven build for " + args[0]);
		Executor exec = new Executor();
		exec.run("mvn -B -DskipTests install");
	}

}
