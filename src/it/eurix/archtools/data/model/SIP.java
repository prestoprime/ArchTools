package it.eurix.archtools.data.model;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

public interface SIP extends RWInformationPackage {

	public Map<String, List<String>> getDCFields() throws IPException;

	public String setRights(Node rights) throws IPException;

	//FIXME: DNX not available in this library...
	//TODO: Improve DataManager capabilities with generics and InformationPackage instantiation...
	public void addDNX(Object dnx, String id, boolean isRef) throws IPException;

	public void setCreateDate(GregorianCalendar date) throws IPException;

	public void purgeFiles() throws IPException;
}
