/**
 * ToolOutput.java
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
package it.eurix.archtools.tool;

import java.util.HashMap;
import java.util.Map;

public final class ToolOutput<AttributeT> {
	
	private String processOutput;
	private String processError;
	private Map<AttributeT, String> attributeMap;
	
	public ToolOutput() {
		this.attributeMap = new HashMap<>();
	}
	
	public String getProcessOutput() {
		return processOutput;
	}
	
	public void setProcessOutput(String processOutput) {
		this.processOutput = processOutput;
	}

	public String getProcessError() {
		return processError;
	}
	
	public void setProcessError(String processError) {
		this.processError = processError;
	}
	
	public String getAttribute(AttributeT key) {
		return attributeMap.get(key);
	}
	
	public void setAttribute(AttributeT key, String value) {
		attributeMap.put(key, value);
	}
}
