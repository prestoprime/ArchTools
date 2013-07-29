/**
 * MySQLDatabaseManager.java
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

import it.eurix.archtools.persistence.SQLDatabaseManager;

public class MySQLDatabaseManager extends SQLDatabaseManager {
	
	/**
	 * Builds a new DatabaseManager specific for MySQL databases.<br/>
	 * The database should be accessable at the URI:<br/>
	 * <code>jdbc:mysql://username:password@host:port/context</code><br/>
	 * Needs <code>com.mysql.jdbc.Driver</code>.<br/>
	 * @param host The hostname or IP where the MySQL DB is published.
	 * @param port The port where the MySQL DB is published.
	 * @param context The context where the MySQL DB is published.
	 * @param username The DB username.
	 * @param password The DB password.
	 */
	public MySQLDatabaseManager(String host, int port, String context, String username, String password) {
		super.driver = "com.mysql.jdbc.Driver";
		super.dbURI = "jdbc:mysql://" + host + ":" + port + "/" + context;
		super.dbUser = username;
		super.dbPass = password;
	}
}
