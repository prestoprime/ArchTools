/**
 * MessageDigestExtractor.java
 * Author: Francesco Gallo (gallo@eurix.it)
 * 
 * This file is part of PrestoPRIME Preservation Platform (P4).
 * 
 * Copyright (C) 2009-2012 EURIX Srl, Torino, Italy
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
package it.eurix.archtools.tool.impl;

import it.eurix.archtools.tool.AbstractTool;
import it.eurix.archtools.tool.ToolException;
import it.eurix.archtools.tool.ToolOutput;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestExtractor extends AbstractTool<MessageDigestExtractor.AttributeType> {

	public static enum AttributeType {
		MD5,
		SHA1
	}
	
	private MessageDigest mdAlgorithm;

	public MessageDigestExtractor() {
		super("MDE");
	}

	public ToolOutput<MessageDigestExtractor.AttributeType> extract(String input) throws ToolException {
		ToolOutput<MessageDigestExtractor.AttributeType> output = new ToolOutput<>(); 
		
		// extract MD5
		byte[] md5Bytes = getMessageDigest(input, "MD5");
		String md5Sum = getHexString(md5Bytes);
		output.setAttribute(AttributeType.MD5, md5Sum);

		// extract SHA-1
		byte[] sha1Bytes = getMessageDigest(input, "SHA-1");
		String sha1Sum = getHexString(sha1Bytes);
		output.setAttribute(AttributeType.SHA1, sha1Sum);

		logger.debug("File: " + input + " MD5: " + md5Sum + " SHA-1: " + sha1Sum);
		
		return output;
	}

	private byte[] getMessageDigest(String input, String algorithm) throws ToolException {
		FileInputStream fis;
		try {
			fis = new FileInputStream(input);
			mdAlgorithm = MessageDigest.getInstance(algorithm);

			byte[] dataBytes = new byte[1024];
			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				mdAlgorithm.update(dataBytes, 0, nread);
			}
		} catch (IOException e) {
			throw new ToolException("Error reading input file " + input);
		} catch (NoSuchAlgorithmException e) {
			throw new ToolException("Unable to find algorithm " + algorithm);
		}
		return mdAlgorithm.digest();
	}

	private String getHexString(byte[] bytes) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xff & bytes[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
