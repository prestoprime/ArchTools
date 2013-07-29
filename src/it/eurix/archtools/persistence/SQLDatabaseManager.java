/**
 * DatabaseManager.java
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public abstract class SQLDatabaseManager extends AbstractDatabaseManager {

	private Set<String> initUpdates = new HashSet<>();

	protected Connection connection;

	protected SQLDatabaseManager() {

	}

	@Override
	protected void startDatabase() throws DatabaseException {
		if (driver != null && dbURI != null && initUpdates != null) {
			try {
				Class.forName(driver).newInstance();
				connection = DriverManager.getConnection(dbURI, dbUser, dbPass);
				logger.debug("DataBaseManager instance created properly...");
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				logger.error("Unable to load database driver...");
				throw new DatabaseException(e.getMessage());
			} catch (SQLException e) {
				logger.error("Unable to instantiate a connection with the database...");
				throw new DatabaseException(e.getMessage());
			}
		}
	}

	@Override
	protected void executeInitUpdates() throws DatabaseException {
		try {
			connection.setAutoCommit(false);

			for (String table : initUpdates) {
				try (PreparedStatement createTable = connection.prepareStatement(table);) {
					createTable.executeUpdate();
					connection.commit();
				} catch (SQLException e) {
					logger.debug(e.getMessage() + "...");
					connection.rollback();
					// throw new DatabaseException(e.getMessage());
				}
			}

			connection.commit();
		} catch (SQLException e) {
			logger.error(e.getMessage() + "...");
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void stopDatabase() throws DatabaseException {
		try {
			connection.close();
			logger.debug("DB stopped...");
		} catch (SQLException e) {
			throw new DatabaseException(e.getMessage());
		}
	}

	@Override
	protected void finalize() throws Throwable {
		this.stopDatabase();
	}

	public void addInitUpdate(String sql) {
		this.initUpdates.add(sql);
	}
}
