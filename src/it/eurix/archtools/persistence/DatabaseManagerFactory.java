/**
 * DatabaseManagerFactory.java
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
package it.eurix.archtools.persistence;

import java.util.HashMap;

public final class DatabaseManagerFactory {

	private static DatabaseManagerFactory instance;

	private HashMap<String, AbstractDatabaseManager> dbCache = new HashMap<>();

	public static DatabaseManagerFactory getInstance() {
		if (instance == null)
			instance = new DatabaseManagerFactory();
		return instance;
	}

	private DatabaseManagerFactory() {

	}

	public <T extends AbstractDatabaseManager> T getDB(Class<T> clazz) throws DatabaseException {
		if (dbCache.get(clazz.getName()) == null) {
			try {
				AbstractDatabaseManager db = (AbstractDatabaseManager) clazz.newInstance();
				db.startDatabase();
				db.executeInitUpdates();
				dbCache.put(clazz.getName(), db);
			} catch (InstantiationException | IllegalAccessException e) {
//				logger.error("Unable to instantiate the implementing class..."):
				throw new DatabaseException(e.getMessage());
			}
		}
		return clazz.cast(dbCache.get(clazz.getName()));
	}
	
	public <T extends AbstractDatabaseManager> void resetDB(Class<T> clazz) {
		dbCache.remove(clazz.getName());
	}
}
