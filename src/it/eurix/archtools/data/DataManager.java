/**
 * DataManager.java
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
package it.eurix.archtools.data;

import it.eurix.archtools.data.model.AIP;
import it.eurix.archtools.data.model.IPException;
import it.eurix.archtools.data.model.InformationPackage;
import it.eurix.archtools.data.model.SIP;
import it.eurix.archtools.persistence.DatabaseException;
import it.eurix.archtools.persistence.impl.ExistDatabaseManager;
import it.eurix.archtools.workflow.jaxb.StatusType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public abstract class DataManager<CollectionT extends Enum<CollectionT>> {

	protected static final Logger logger = LoggerFactory.getLogger(DataManager.class);
	
	protected ExistDatabaseManager<CollectionT> persistenceManager;
	private Map<String, InformationPackage> runningIPMap;
	
	protected DataManager(ExistDatabaseManager<CollectionT> persistenceManager) {
		this.persistenceManager = persistenceManager;
		runningIPMap = new HashMap<>();
	}
	
	protected UUID generateUniqueId() {
		UUID uuid;
		boolean idIsUnique;

		do {
			uuid = UUID.randomUUID();
			idIsUnique = false;

			logger.debug("Check ID uniqueness for ID = " + uuid.toString());

			InformationPackage p4Ip = this.getRunningIP(uuid.toString());
			if (p4Ip == null) { // no conflicting running IP
				idIsUnique = true;
			}

			if (idIsUnique) {
				logger.debug("No conflicting running SIP.");
				try {
					// we should get a DataException if there is no AIP with
					// this ID in the database
					this.getAIPByID(uuid.toString());
					idIsUnique = false;
					logger.debug("Found a conflicting AIP on the database. Generating another ID...");
				} catch (DataException e) {
					// in this case the Exception is good
					logger.debug("No conflicting AIP with this ID in DB. Proceed...");
				}
			}
		} while (!idIsUnique);

		return uuid;
	}
	
	protected InformationPackage getRunningIP(String key) {
		InformationPackage p4IP = runningIPMap.get(key);
		if (p4IP != null) {
			p4IP.incrementCounter();

			logger.debug("Incremented counter for IP " + key + ": total " + p4IP.getCounter());

			return p4IP;
		} else {
			return null;
		}
	}

	protected void registerRunningIP(String key, InformationPackage p4IP) {
		p4IP.incrementCounter();
		runningIPMap.put(key, p4IP);

		logger.debug("Running IPs: " + runningIPMap.keySet());
		logger.debug("Incremented counter for IP " + p4IP.getId() + ": total " + p4IP.getCounter());
	}

	public void releaseIP(InformationPackage ip) {
		if (ip != null && ip instanceof InformationPackage) {
			InformationPackage p4IP = (InformationPackage) ip;
			p4IP.decrementCounter();

			logger.debug("Running IPs: " + runningIPMap.keySet());
			logger.debug("Decremented counter for IP " + ip.getId() + ": total " + p4IP.getCounter());

			if (p4IP.getCounter() <= 0) {
				try {
					p4IP.selfRelease();
				} catch (IPException e) {
					e.printStackTrace();
					throw new RuntimeException("Unable to self release the IP...");
				}
				runningIPMap.remove(p4IP.getId());

				logger.debug("Released IP " + p4IP.getId());
			}
		} else {
			logger.error("Trying releasing an InformationPackage unreleasable...");
			throw new RuntimeException("Invalid releasing IP...");
		}
	}

	public Node getResource(CollectionT collection, String resId) throws DataException {
		try {
			return persistenceManager.readXMLResource(collection, resId);
		} catch (DatabaseException e) {
			throw new DataException("Unable to find resource " + resId + " in collection " + collection + "...");
		}
	}
	
	public abstract String createNewSIP(File file) throws DataException;
	public abstract SIP getSIPByID(String id) throws DataException;
	public abstract void consolidateSIP(SIP sip) throws DataException;
	public abstract void deleteSIP(String id) throws DataException;
	public abstract List<String> getAllAIP(Map<String, String> elements) throws DataException;
	public abstract AIP getAIPByID(String id) throws DataException;
	public abstract List<String> getAIPByMD(String label, boolean isAvailable) throws DataException;
	
	public abstract List<String> getWfStatus(String userID, StatusType status) throws DataException;
	
}
