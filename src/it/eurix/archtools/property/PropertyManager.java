/**
 * PropertyManager.java
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
package it.eurix.archtools.property;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyManager<PropertyT extends Enum<PropertyT>> {

	protected static final Logger logger = LoggerFactory.getLogger(PropertyManager.class);

	private PropertyPersistenceManager propertyPersistence;
	private Properties properties;
	
	protected PropertyManager(PropertyPersistenceManager propertyPersistence) {
		this.propertyPersistence = propertyPersistence;
		properties = this.propertyPersistence.getProperties();
	}

	public Properties getProperties() {
		return properties;
	}

	public String getProperty(PropertyT property) {
		return properties.getProperty(property.toString(), "property.default.value");
	}

	public void setProperty(PropertyT property, String value) {
		properties.setProperty(property.toString(), value);
		propertyPersistence.setProperties(properties);
	}
}
