/**
 * UserManager.java
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

import it.eurix.archtools.user.jaxb.Users;
import it.eurix.archtools.user.jaxb.Users.User;
import it.eurix.archtools.user.jaxb.Users.User.Service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserManager {

	protected static final Logger logger = LoggerFactory.getLogger(UserManager.class);
	
	public static enum UserRole {
		guest(0), consumer(1), producer(2), admin(3), superadmin(4);

		private int level;

		private UserRole(int level) {
			this.level = level;
		}

		public int getLevel() {
			return level;
		}
	};
	
	private UserPersistenceManager userPersistenceManager;
	private Users users;
	
	protected UserManager(UserPersistenceManager userPersistenceManager) {
		this.userPersistenceManager = userPersistenceManager;
		this.users = this.userPersistenceManager.getUsersDescriptor();
	}
	
	public boolean isValidUser(String userID) {
		if (users != null) {
			for (User user : users.getUser()) {
				if (user.getId().equals(userID)) {
					return true;
				}
			}
		}
		return false;
	}

	public UserRole getUserRole(String userID) {
		if (users != null) {
			for (User user : users.getUser()) {
				if (user.getId().equals(userID)) {
					try {
						UserRole role = UserRole.valueOf(user.getRole());
						return role;
					} catch (IllegalArgumentException e) {
						logger.error("No role found for user with serviceID " + userID);
					}
				}
			}
		}
		return UserRole.guest;
	}

	public String getService(String userID, String serviceName) {
		if (users != null) {
			for (User user : users.getUser()) {
				if (user.getId().equals(userID)) {
					for (Service service : user.getService()) {
						if (service.getKey().equals(serviceName)) {
							return service.getValue();
						}
					}
				}
			}
		}
		return null;
	}

	public String addUser(String role) {
		if (users != null) {

			// create new userID
			String userID = UUID.randomUUID().toString();

			// else add new user
			User user = new User();
			user.setId(userID);
			user.setRole(role);
			users.getUser().add(user);
			userPersistenceManager.setUserDescriptor(users);

			logger.debug("Added new user with userID " + userID);

			return userID;
		}
		return null;
	}

	public void deleteUser(String userID) {
		if (users != null) {

			// check if already existing
			for (User user : users.getUser()) {
				if (user.getId().equals(userID)) {
					users.getUser().remove(user);
					userPersistenceManager.setUserDescriptor(users);

					logger.debug("Deleted user with userID " + userID);

					break;
				}
			}
		}
	}

	public void addUserService(String userID, String key, String value) {
		if (users != null) {

			// search user
			for (User user : users.getUser()) {
				if (user.getId().equals(userID)) {

					// search service
					for (Service service : user.getService()) {
						if (service.getKey().equals(key)) {

							// delete service
							user.getService().remove(service);
							break;
						}
					}

					// add new service
					Service service = new Service();
					service.setKey(key);
					service.setValue(value);
					user.getService().add(service);

					// commit
					userPersistenceManager.setUserDescriptor(users);

					logger.debug("Added new service for userID " + userID);

					break;
				}
			}
		}
	}

	public void deleteUserService(String userID, String key) {
		// search user
		for (User user : users.getUser()) {
			if (user.getId().equals(userID)) {

				// search service
				for (Service service : user.getService()) {
					if (service.getKey().equals(key)) {

						// delete service
						user.getService().remove(service);
						break;
					}
				}

				// commit
				userPersistenceManager.setUserDescriptor(users);

				logger.debug("Deleted service " + key + " for userID " + userID);

				break;
			}
		}
	}
}
