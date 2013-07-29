/**
 * ExistDatabaseManager.java
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
package it.eurix.archtools.persistence.impl;

import it.eurix.archtools.persistence.AbstractDatabaseManager;
import it.eurix.archtools.persistence.DatabaseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;

import org.exist.storage.DBBroker;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.XQueryService;
import org.w3c.dom.Node;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

public class ExistDatabaseManager<CollectionT extends Enum<CollectionT>> extends AbstractDatabaseManager {

	private String rootCollection = null;
	private XQueryService service;
	private Map<String, CompiledExpression> queryMap;

	protected enum MyCollection {
	};

	/**
	 * Builds a new DatabaseManager specific for Exist databases.<br/>
	 * The database should be accessable at the URI:<br/>
	 * <code>xmldb:exist://username:password@host:port/context</code><br/>
	 * Needs <code>org.exist.xmldb.DatabaseImpl</code>.<br/>
	 */
	public ExistDatabaseManager(String host, int port, String context, String username, String password, String rootCollection) {
		super.driver = "org.exist.xmldb.DatabaseImpl";
		super.dbURI = "xmldb:exist://" + host + ":" + port + "/" + context;
		super.dbUser = username;
		super.dbPass = password;
		this.rootCollection = super.dbURI + DBBroker.ROOT_COLLECTION + "/" + rootCollection;
	}

	@Override
	protected void startDatabase() throws DatabaseException {
		try {
			// initialize driver
			Database database = (Database) Class.forName(super.driver).newInstance();

			logger.debug("Successfully loaded persistence DB driver...");

			// initialize persistence DB properties
			database.setProperty("create-database", "true");
			DatabaseManager.registerDatabase(database);

			logger.debug("Successfully registered persistence DB instance...");

			// get root-collection
			Collection rootCollection = DatabaseManager.getCollection(super.dbURI + DBBroker.ROOT_COLLECTION);
			CollectionManagementService mgtService = (CollectionManagementService) rootCollection.getService("CollectionManagementService", "1.0");
			mgtService.createCollection(this.rootCollection);

			// get query-service
			service = (XQueryService) rootCollection.getService("XQueryService", "1.0");

			// set pretty-printing on
			service.setProperty(OutputKeys.INDENT, "yes");
			service.setProperty(OutputKeys.ENCODING, "UTF-8");

			logger.debug("Successfully created XQueryService...");

			// initialize query-map
			queryMap = new HashMap<>();
		} catch (IllegalAccessException | InstantiationException | ClassNotFoundException | XMLDBException e) {
			logger.error(e.getMessage());
			throw new DatabaseException(e.getMessage());
		}

		logger.debug("Successfully initialized ExistDB...");
	}

	@Override
	protected void executeInitUpdates() throws DatabaseException {

	}

	@Override
	protected void stopDatabase() throws DatabaseException {

	}

	private void cleanUpResources(Collection collection, Resource... resources) {
		try {
			for (int i = 0; i < resources.length; i++) {
				if (resources[i] != null)
					((EXistResource) resources[i]).freeResources();
			}
			if (collection != null)
				collection.close();
		} catch (XMLDBException e) {
			logger.error(e.getMessage() + "...");
		}
	}

	private CompiledExpression getCompiledExpression(URL queryFile) throws DatabaseException {

		// get compiledExpression
		CompiledExpression expression = queryMap.get(queryFile.toString());

		// if it doesn't exists yet, compile&store
		if (expression == null) {
			try {
				BufferedReader f = new BufferedReader(new InputStreamReader(queryFile.openStream()));

				logger.debug("Query File " + queryFile + " found...");

				String line;
				StringBuffer xml = new StringBuffer();
				while ((line = f.readLine()) != null)
					xml.append(line + "\n");
				f.close();

				expression = service.compile(xml.toString());

				queryMap.put(queryFile.toString(), expression);

				logger.debug("============== Added new CompiledExpression ==============");
				logger.debug(xml.toString());
				logger.debug("==========================================================");
			} catch (IOException e) {
				logger.error("Unable to read the query file " + queryFile);
				throw new DatabaseException(e.getMessage());
			} catch (XMLDBException e) {
				logger.error("Unable to compile the query in file " + queryFile);
				throw new DatabaseException(e.getMessage());
			}
		}

		return expression;
	}

	/**
	 * Connects to persistence DB and creates a new XML resource or updates an existing one.
	 */
	public synchronized void storeXMLResource(CollectionT customCollection, String resId, Node resource) throws DatabaseException {
		CustomCollectionWrapper wrappedCollection = new CustomCollectionWrapper(customCollection);

		Collection collection = null;
		XMLResource xmlResource = null;
		try {
			collection = wrappedCollection.getCollection();
			xmlResource = (XMLResource) collection.createResource(resId, "XMLResource");
			xmlResource.setContentAsDOM(resource);
			collection.storeResource(xmlResource);

			logger.debug("Stored XML resource to persistence DB (Collection: " + wrappedCollection + " - Resource: " + xmlResource.getId() + ")");
		} catch (XMLDBException e) {
			e.printStackTrace();
			logger.error("Unable to store XML resource to persistence DB (Collection: " + wrappedCollection + " - Resource: " + resId + ")");
			throw new DatabaseException(e.getMessage());
		} finally {
			cleanUpResources(collection, xmlResource);
		}
	}

	/**
	 * Connects to persistence DB and reads an already present XML resource.
	 */
	public synchronized Node readXMLResource(CollectionT customCollection, String resId) throws DatabaseException {
		CustomCollectionWrapper wrappedCollection = new CustomCollectionWrapper(customCollection);

		Node node = null;
		Collection collection = null;
		XMLResource resource = null;
		try {
			collection = wrappedCollection.getCollection();
			resource = (XMLResource) collection.getResource(resId);
			if (resource != null) {
				node = resource.getContentAsDOM();

				logger.debug("Retrieved XML resource from persistence DB (Collection: " + wrappedCollection + " - Resource: " + resource.getId() + ")");

				return node;
			}
			throw new DatabaseException("Resource " + resId + " not present in collection " + wrappedCollection + "...");
		} catch (XMLDBException e) {
			e.printStackTrace();
			logger.error("Unable to read XML resource from persistence DB (Collection: " + wrappedCollection + " - Resource: " + resId + ")");
			throw new DatabaseException(e.getMessage());
		} finally {
			cleanUpResources(collection, resource);
		}
	}

	/**
	 * Connects to persistence DB and deletes an already present XML resource.
	 */
	public synchronized void deleteXMLResource(CollectionT customCollection, String resId) throws DatabaseException {
		CustomCollectionWrapper wrappedCollection = new CustomCollectionWrapper(customCollection);

		Collection collection = null;
		Resource res = null;
		try {
			collection = wrappedCollection.getCollection();
			res = collection.getResource(resId);
			if (res != null) {
				collection.removeResource(res);

				logger.debug("Deleted XML resource from persistence DB (Collection: " + wrappedCollection + " - Resource: " + resId + ")");
				return;
			}

			logger.debug("No XML resource to delete from persistence DB (Collection: " + wrappedCollection + " - Resource: " + resId + ")");
		} catch (XMLDBException e) {
			e.printStackTrace();
			logger.error("Unable to delete XML resource from persistence DB (Collection: " + wrappedCollection + " - Resource: " + resId + ")");
			throw new DatabaseException(e.getMessage());
		} finally {
			cleanUpResources(collection, res);
		}
	}

	public synchronized List<String> executeQuery(URL queryFile, CollectionT customCollection, Map<String, String> params, boolean recompile) throws DatabaseException {
		CustomCollectionWrapper wrappedCollection = new CustomCollectionWrapper(customCollection);
		CompiledExpression expression = this.getCompiledExpression(queryFile);

		List<String> resultList = new ArrayList<>();

		try {
			XQueryService service = (XQueryService) wrappedCollection.getCollection().getService("XQueryService", "1.0");
			// set pretty-printing on
			service.setProperty(OutputKeys.INDENT, "yes");
			service.setProperty(OutputKeys.ENCODING, "UTF-8");
			
			for (String key : params.keySet())
				service.declareVariable(key, params.get(key));

//			service.setCollection(wrappedCollection.getCollection());
			
			ResourceSet res = service.execute(expression);
			ResourceIterator it = res.getIterator();

			while (it.hasMoreResources()) {
				Resource resource = it.nextResource();
				resultList.add(resource.getContent().toString());
				cleanUpResources(wrappedCollection.getCollection(), resource);
			}
		} catch (XMLDBException e) {
			e.printStackTrace();
			throw new DatabaseException(e.getMessage());
		} finally {
			// TODO
		}

		return resultList;
	}

	private class CustomCollectionWrapper {
		private CollectionT collection;

		protected CustomCollectionWrapper(CollectionT collection) {
			this.collection = collection;
		}

		private Collection getCollection() throws XMLDBException {

			// try to get collection
			logger.debug(ExistDatabaseManager.this.rootCollection + "/" + this.collection.toString().toLowerCase());
			Collection collection = DatabaseManager.getCollection(
					ExistDatabaseManager.this.rootCollection + "/" + this.collection.toString().toLowerCase(),
					ExistDatabaseManager.this.dbUser,
					ExistDatabaseManager.this.dbPass);
			if (collection == null) {
				// if collection does not exist, get root collection and create
				// the new collection as a direct child of the root collection
				Collection rootCollection = DatabaseManager.getCollection(
						ExistDatabaseManager.this.rootCollection,
						ExistDatabaseManager.this.dbUser,
						ExistDatabaseManager.this.dbPass);
				CollectionManagementService mgtService = (CollectionManagementService) rootCollection.getService("CollectionManagementService", "1.0");
				collection = mgtService.createCollection(ExistDatabaseManager.this.rootCollection + "/" + this.collection.toString());

				logger.info("Created new Collection " + collection);
			}

			return collection;
		}

		@Override
		public String toString() {
			return this.collection.toString().toLowerCase();
		}
	};
}
