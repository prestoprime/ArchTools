/**
 * ToolManager.java
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ToolManager {
	
	protected static final Logger logger = LoggerFactory.getLogger(ToolManager.class);
	
	private ToolPersistenceManager toolPersistence;
	
	protected ToolManager(ToolPersistenceManager toolPersistence) {
		this.toolPersistence = toolPersistence;
		this.toolPersistence.getTools();
	}

	public <T extends AbstractTool> T getTool(Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Tool getToolDescriptor(String toolName) {
		return toolPersistence.getTool(toolName);
	}
	
	public void list() {
		for (Tool tool : toolPersistence.getTools()) {
			logger.info("##### " + tool.getName() + " #####");
			List<Executable> exeList = tool.getExecutable();
			for (Executable executable : exeList) {
				logger.info("Executable: " + executable.getValue() + " (" + executable.getOsName() + ")");
			}
			List<Dynlib> libList = tool.getDynlib();
			for (Dynlib dynlib : libList) {
				logger.info("Dynamic Library: " + dynlib.getValue() + " (" + dynlib.getOsName() + ")");
			}
		}
	}

	public void show(Tool tool) {
		for (Tool aTool : toolPersistence.getTools()) {
			if (aTool.getName().equalsIgnoreCase(tool.getName())) {
				logger.info("##### " + tool.getName() + " #####");
				List<Executable> exeList = tool.getExecutable();
				for (Executable executable : exeList) {
					logger.info("Executable: " + executable.getValue() + " (" + executable.getOsName() + ")");
				}
				List<Dynlib> libList = tool.getDynlib();
				for (Dynlib dynlib : libList) {
					logger.info("Dynamic Library: " + dynlib.getValue() + " (" + dynlib.getOsName() + ")");
				}
			}
		}
	}
}
