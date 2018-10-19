package org.apache.marmotta.knowledge.vis.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class Property extends Instance<PropertyType> {
//	private String nodeType;
	private String dataType;
	
	private List<PropertyValue> values;

	/**
	 * @return the valueType
	 */
	public String getDataType() {
		return dataType;
	}

	/**
	 * @param valueType the valueType to set
	 */
	public void setDataType(String valueType) {
		this.dataType = valueType;
	}
	public void addValue(String value, Locale locale) {
	    PropertyValue v = new PropertyValue();
	    v.setValue(value==null?"":value);
	    v.setOldValue(value);
	    v.setLocale(locale);
	    getValues().add(v);
	    
	}

	public void addValue(String value, String language) {
	    if ( language != null ) {
	        addValue(value, Locale.forLanguageTag(language));
	    }
	    addValue(value, (Locale)null);
	}

//	/**
//	 * @return the nodeType
//	 */
//	public String getNodeType() {
//		return nodeType;
//	}
//
//	/**
//	 * @param nodeType the nodeType to set
//	 */
//	public void setNodeType(String nodeType) {
//		this.nodeType = nodeType;
//	}

	@XmlTransient
	public String getComment() {
		return super.getComment();
	}
	/**
	 * be sure to have a unique id for each property
	 */
//	public String getId() {
//		int hash = getType().hashCode();
//		return String.format("%s:%s:%s", super.getId(), hash, getLocale().getLanguage());
//	}

    public List<PropertyValue> getValues() {
        if (values == null ) {
            values = new ArrayList<PropertyValue>();
        }
        return values;
    }

    public void setValues(List<PropertyValue> values) {
        this.values = values;
    }
    @JsonIgnore
    @XmlTransient
    public boolean isModified() {
        for (PropertyValue value :  getValues() ) {
            if ( value.isModified() ) {
                return true;
            }
        }
        return false;
    }
}
