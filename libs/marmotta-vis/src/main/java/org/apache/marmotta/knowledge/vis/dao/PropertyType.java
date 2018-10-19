package org.apache.marmotta.knowledge.vis.dao;

import java.util.Locale;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import org.openrdf.model.URI;

public class PropertyType extends Type {
	private DisplayTypeEnum displayType;
	private Boolean multiValue;
	private Boolean multiLingual;
	private Boolean required;
	private String dataType;
	private Set<PropertyTypeOption> options;


	/**
	 * @return the multiValue
	 */
	public Boolean getMultiValue() {
	    if ( multiValue == null ) {
	        return Boolean.FALSE;
	    }
		return multiValue;
	}

	/**
	 * @param multiValue the multiValue to set
	 */
	public void setMultiValue(Boolean multiValue) {
		this.multiValue = multiValue;
	}

	/**
	 * @return the multiLingual
	 */
	public Boolean getMultiLingual() {
		if ( multiLingual == null ) {
			return Boolean.FALSE;
		}
		return multiLingual;
	}

	/**
	 * @param multiLingual the multiLingual to set
	 */
	public void setMultiLingual(Boolean multiLingual) {
		this.multiLingual = multiLingual;
	}

	/**
	 * @return the required
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * @param required the required to set
	 */
	public void setRequired(Boolean required) {
		this.required = required;
	}

	/**
	 * @return the dataType
	 */
	public DisplayTypeEnum getDisplayType() {
		return displayType;
	}

	/**
	 * @param dataType the dataType to set
	 */
	public void setDisplayType(DisplayTypeEnum dataType) {
		this.displayType = dataType;
	}

	/**
	 * @return the options
	 */
	public Set<PropertyTypeOption> getOptions() {
		return options;
	}

	/**
	 * @param options the options to set
	 */
	public void setOptions(Set<PropertyTypeOption> options) {
		this.options = options;
	}

	/**
	 * @return the dataType
	 */
	public String getDataType() {
		return dataType;
	}

	/**
	 * @param dataType the dataType to set
	 */
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	
	public void initDataType(URI uri) {
		this.dataType = uri.getLocalName();
	}
	@XmlTransient
	public Property asProperty(String value, Locale locale) {
		Property property = new Property();
		if ( getMultiLingual() ) {
			property.setLocale(locale);
		}
		property.setId(getId());
		property.setLabel(this.getLabel());
		property.addValue(value, getMultiLingual()?locale:null);
//		property.setNetworkType(nw.getId());
//		property.setNodeType(no.getId());
		// create an id including the locale
		property.setInstanceType(this);
		property.setDataType(this.getDataType());
		property.setEditable(this.isEditable());
		return property;
		
	}
}
