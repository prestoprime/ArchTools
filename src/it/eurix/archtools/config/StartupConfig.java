/**
 * StartupConfiguration.java
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
package it.eurix.archtools.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class StartupConfig<PropertyT extends Enum<PropertyT>> {
	
	private String propertiesFilename = null;
	
	protected StartupConfig(String propertiesFileName) {
		this.propertiesFilename = propertiesFileName;
	}
	
	@SuppressWarnings("unchecked")
	public String getProperty(PropertyT property) {
		Properties props = new Properties();
		if (System.getProperty("user.home") != null) {
			File userHomeFile = new File(System.getProperty("user.home"));
			File propertiesFile = new File(userHomeFile, propertiesFilename);
			try {
				props.load(new FileInputStream(propertiesFile));
			} catch (IOException e) {
				
			} finally {
				try {
					PropertyT[] propList = (PropertyT[]) property.getClass().getMethod("values").invoke(property);
					for (PropertyT prop : propList)
						if (props.getProperty(prop.toString()) == null)
							props.put(prop.toString(), "startup.default.value");

					props.store(new FileOutputStream(propertiesFile), "auto-generated props file");
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
		return props.getProperty(property.toString());
	}
}
