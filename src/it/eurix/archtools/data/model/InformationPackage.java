package it.eurix.archtools.data.model;

import it.eurix.archtools.data.model.DIP.DCField;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

public interface InformationPackage {

	public int getCounter();

	public void incrementCounter();

	public void decrementCounter();
	
	public void selfRelease() throws IPException;

	public String getId();

	public List<Node> executeNodeQuery(String xPath) throws IPException;

	public List<String> executeQuery(String xPath) throws IPException;

	public boolean hasAVMaterial() throws IPException;

	public boolean hasRights() throws IPException;

	public List<String> getAVFormats() throws IPException;

	public List<String> getAVMaterial(String mimeType, String location) throws IPException;

	/**
	 * Gets a list of Strings, denoting the paths to all linked AV files
	 * 
	 * @param mimeType
	 *            specifies the format as mimetype, e.g. "video/webm"
	 * @param metsLocType
	 *            specifies the type of mets:FLocat, e.g. "FILE" or "URL"
	 * @param outputLocType
	 *            specifies the replacement for P4 placeholder. Using "FILE" here will return a link into the server's local filesystem. Using "URL" will return a valid http link to the file.
	 * @return
	 * @throws P4IPException
	 */
	public List<String> getAVMaterial(String mimeType, String metsLocType, String outputLocType) throws IPException;

	public String getChecksum(String mimeType, String checksumType) throws IPException;

	public int getDuration() throws IPException;

	public GregorianCalendar getCreateDate() throws IPException;

	public Map<String, List<String>> getDCFields() throws IPException;

	public List<String> getDCField(DCField field) throws IPException;

}
