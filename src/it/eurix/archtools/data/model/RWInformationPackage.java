package it.eurix.archtools.data.model;

import it.eurix.archtools.data.model.DIP.DCField;

import java.util.List;

public interface RWInformationPackage extends InformationPackage {

	public void setDCField(DCField field, List<String> values) throws IPException;

	public void addExternalFile(String mimeType, String href, String md5sum, long size) throws IPException;

	public void addFile(String mimeType, String locType, String href, String md5sum, long size) throws IPException;

	public void replaceFileLocation(String mimeType, String oldLocType, String newLocType, String href, String md5sum, long size) throws IPException;

}
