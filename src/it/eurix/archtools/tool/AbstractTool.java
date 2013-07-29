/**
 * AbstractTool.java
 * Author: Francesco Gallo (gallo@eurix.it)
 * 
 * This file is part of PrestoPRIME Preservation Platform (P4).
 * 
 * Copyright (C) 2009-2012 EURIX Srl, Torino, Italy
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eurix.archtools.tool;

import it.eurix.archtools.tool.jaxb.Dynlib;
import it.eurix.archtools.tool.jaxb.Executable;
import it.eurix.archtools.tool.jaxb.Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTool<OutputT extends Enum<OutputT>> {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractTool.class);

	private String OS;
	private Tool tool;
	private String[] env;

	public AbstractTool(Tool tool) {
		this.OS = System.getProperty("os.name").toLowerCase();
		this.tool = tool;

		try {
			this.configLibraryPath();
		} catch (ToolException e) {
			e.printStackTrace();
			logger.error("Unable to create instance for tool " + tool.getName());
		}
	}

	public AbstractTool(String toolName) {
		this(new Tool());
		this.tool.setName(toolName);
		
		Executable executable = new Executable();
		executable.setOsName(this.OS);
		executable.setValue(null);
		this.tool.getExecutable().add(executable);
	}
	
	/**
	 * Retrieves tool information from tools.xml, copies dynamic libraries to a temp path and sets up the LD_LIBRARY_PATH for command execution.
	 * 
	 * @throws ToolException
	 */
	private void configLibraryPath() throws ToolException {
		if (tool != null) {
			// tool dynamic libraries (if any) exported on disk
			List<Dynlib> dynlibList = tool.getDynlib();
			if (dynlibList.size() == 0)
				return;

			// creates temp dir for LD_LIBRARY_PATH
			File outDir = new File(System.getProperty("java.io.tmpdir"), tool.getName().toUpperCase());
			if (!outDir.exists()) {
				outDir.mkdirs();
				outDir.deleteOnExit();
			}
			String[] linuxEnv = { "LD_LIBRARY_PATH=" + System.getenv("LD_LIBRARY_PATH") + File.pathSeparator + outDir.getAbsolutePath() };
			env = linuxEnv;

			// copy dyn libs to temp path
			for (Dynlib dynlib : dynlibList) {
				if (!dynlib.getOsName().equalsIgnoreCase(OS))
					continue;
				String dynlibPath = dynlib.getValue();
				int slash = dynlibPath.lastIndexOf(File.separatorChar);
				dynlibPath = dynlibPath.substring(slash + 1);
				File dynlibFile = new File(outDir, dynlibPath);
				if (!dynlibFile.exists())
					copyResource(dynlib.getValue(), dynlibFile.getAbsolutePath());
				dynlib.setValue(dynlibFile.getAbsolutePath()); // update Tool
																// library
			}
		}
	}

	private void copyResource(String source, String target) throws ToolException {
		try {
			FileChannel sourceFile = new FileInputStream(source).getChannel();
			FileChannel targetFile = new FileOutputStream(target).getChannel();

			sourceFile.transferTo(0, sourceFile.size(), targetFile);

		} catch (IOException e) {
			e.printStackTrace();
			throw new ToolException("Unable to copy Tool Resource: " + source);
		}
	}

	/**
	 * Prints tool configuration (executables and dynamic libraries) for the current architecture.
	 */
	public final void showInfo() {
		logger.info("##### " + tool.getName() + " Configuration #####");
		List<Executable> exeList = tool.getExecutable();
		for (Executable executable : exeList) {
			logger.info("Executable: " + executable.getValue() + " (" + executable.getOsName() + ")");
		}
		List<Dynlib> libList = tool.getDynlib();
		for (Dynlib dynlib : libList) {
			logger.info("Dynamic Library: " + dynlib.getValue() + " (" + dynlib.getOsName() + ")");
		}
	}

	/**
	 * Overwrites default executable path with a custom one. Used also for custom tools.
	 * 
	 * @param execPath
	 *            Absolute path of the new executable.
	 */
	public void setCustomExecutable(String execPath) {
		List<Executable> execList = tool.getExecutable();
		for (Executable executable : execList) {
			if (executable.getOsName().equalsIgnoreCase(this.OS)) {
				executable.setValue(execPath);
				break;
			}
		}
	}

	/**
	 * @return Absolute path of the executable.
	 */
	public String getExecutable() throws ToolException {
		List<Executable> execList = tool.getExecutable();
		for (Executable executable : execList) {
			if (executable.getOsName().equalsIgnoreCase(this.OS))
				return executable.getValue();
		}
		throw new ToolException("No executable defined for current OS...");
	}

	/**
	 * Compute the command line
	 */
	private String[] getCommandLine(String... params) throws ToolException {
		String[] cmd = new String[params.length + 1];
		cmd[0] = this.getExecutable();
		for (int i = 0; i < params.length; i++) {
			cmd[i + 1] = params[i];
		}
		return cmd;
	}
	
	/**
	 * Compute the command line
	 */
	private String getStringCommandLine(String... params) throws ToolException {
		String[] cmd = this.getCommandLine(params);
		StringBuffer cmdline = new StringBuffer();
		for (String s : cmd) {
			cmdline.append(s + " ");
		}
		return cmdline.toString();
	}
	
	/**
	 * Start a new process, adding the shutdown hook
	 */
	protected Process start(String... params) throws ToolException {
		String[] cmd = this.getCommandLine(params);

		logger.info("Executing command: " + this.getStringCommandLine(cmd));
		
		try {
			Process process = Runtime.getRuntime().exec(cmd, env, null);

			ProcessKiller terminator = new ProcessKiller(process);
			Runtime.getRuntime().addShutdownHook(terminator);
			
			return process;
		} catch (IOException e) {
			logger.debug(e.getMessage(), e);
			throw new ToolException("Unable to execute command " + this.getStringCommandLine(cmd));
		}
	}

	/**
	 * Executes command for a particular tool invoking {@link ToolProcessor}. Provides access to process output and error streams for parsing.
	 * 
	 * @param cmd Command for executing a tool.
	 * @return Map with process output stream and process error stream.
	 * @throws ToolException
	 */
	protected ToolOutput<OutputT> execute(String... params) throws ToolException {
		StringBuffer processOutput = new StringBuffer();
		StringBuffer processError = new StringBuffer();
		
		try {
			Process process = this.start(params);
			
			ProcessStreamReader processOutputReader = new ProcessStreamReader(process.getInputStream(), processOutput);
			ProcessStreamReader processErrorReader = new ProcessStreamReader(process.getErrorStream(), processError);
			processOutputReader.start();
			processErrorReader.start();

			int exitValue = process.waitFor();
			if (exitValue != 0) {
				throw new ToolException("Exec failed with error " + exitValue + ": " + this.getCommandLine(params));
			} else {
				logger.info("##### Process Output Messages #####\n" + processOutput);
				logger.warn("##### Process Error Messages #####\n" + processError);
			}
			
			processOutputReader.join();
			processErrorReader.join();
			
			ToolOutput<OutputT> output = new ToolOutput<>();
			output.setProcessOutput(processOutput.toString());
			output.setProcessError(processError.toString());
			return output;
		} catch (InterruptedException e) {
			logger.debug(e.getMessage(), e);
			throw new ToolException("Wrong process execution...");
		}
	}
	
	@Deprecated
	protected ToolOutput<OutputT> run(String... params) throws ToolException {
		return this.execute(params);
	}
	
	private class ProcessStreamReader extends Thread {
		
		private InputStream input;
		private StringBuffer output;
		
		private ProcessStreamReader(InputStream input, StringBuffer output) {
			this.input = input;
			this.output = output;
		}
		
		@Override
		public void run() {
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			try {
				while ((line = reader.readLine()) != null) {
					logger.debug(line);
					output.append(line + "\n");
				}
			} catch (IOException e) {
				logger.error(e.getMessage());
				logger.debug(e.getMessage(), e);
			}
		}
	}

	private class ProcessKiller extends Thread {

		private Process deadProcessWalking;

		public ProcessKiller(Process process) {
			deadProcessWalking = process;
		}

		@Override
		public void run() {
			deadProcessWalking.destroy();
		}
	}
}
