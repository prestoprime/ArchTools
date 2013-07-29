/**
 * WorkflowPersistenceManager.java
 * Author: Francesco Rosso (rosso@eurix.it)
 * 
 * This file is part of PrestoPRIME Preservation Platform (P4).
 * 
 * Copyright (C) 2013 EURIX Srl, Torino, Italy
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
package it.eurix.archtools.workflow;

import it.eurix.archtools.config.ConfigurationPersistenceManager;
import it.eurix.archtools.workflow.jaxb.WfDescriptor;
import it.eurix.archtools.workflow.jaxb.WfStatus;

import java.io.File;

public abstract class WorkflowPersistenceManager extends ConfigurationPersistenceManager {

	protected WorkflowPersistenceManager() {
		super(WfDescriptor.class, WfStatus.class);
	}
	
	public abstract WfDescriptor getWfDescriptor();
	public abstract void setWfDescriptor(File descriptor);
	public abstract WfStatus getWfStatus(String jobID);
	public abstract void setWfStatus(WfStatus wfStatus);
	public abstract void deleteWfStatus(String jobID);
}
