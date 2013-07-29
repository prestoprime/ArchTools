/**
 * ConfigurationPersistenceManager.java
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

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public abstract class ConfigurationPersistenceManager {

	protected static final Logger logger = LoggerFactory.getLogger(ConfigurationPersistenceManager.class);
	
	protected Marshaller marshaller;
	protected Unmarshaller unmarshaller;
	
	protected ConfigurationPersistenceManager(URL schemaURL, Class<?>... clazz) {
		try {
			JAXBContext context = JAXBContext.newInstance(clazz);
			marshaller = context.createMarshaller();
			unmarshaller = context.createUnmarshaller();
			
			if (schemaURL != null) {
				try {
					Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaURL);
					if (schema != null) {
						marshaller.setSchema(schema);
						unmarshaller.setSchema(schema);
					}
				} catch (SAXException e) {
					e.printStackTrace();
				}
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	protected ConfigurationPersistenceManager(Class<?>... clazz) {
		this(null, clazz);
	}
}
