/**
 * UserPersistenceManager.java
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
package it.eurix.archtools.user;

import it.eurix.archtools.config.ConfigurationPersistenceManager;
import it.eurix.archtools.user.jaxb.Users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UserPersistenceManager extends ConfigurationPersistenceManager {

	protected static final Logger logger = LoggerFactory.getLogger(UserPersistenceManager.class);
	
	public UserPersistenceManager() {
		super(Users.class);
	}
	
	public abstract Users getUsersDescriptor();
	public abstract void setUserDescriptor(Users users);
}
