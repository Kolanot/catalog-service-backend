package org.apache.marmotta.knowledge.vis.dao;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;



public abstract class Type extends Base {
	private String networkType;
	private transient String type;
	private String comment;
	private transient Set<String> types;
	/**
	 * @return the rdf:type
	 */
	@XmlTransient
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the config
	 */
	@XmlTransient
	public String getNetworkType() {
		return networkType;
	}

	/**
	 * @param config the config to set
	 */
	public void setNetworkType(String config) {
		this.networkType = config;
	}
	

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the types
	 */
	@XmlTransient
	public Set<String> getTypes() {
	    if ( types == null ) {
	        types = new HashSet<String>();
	    }
		return types;
	}

	/**
	 * @param types the types to set
	 */
	public void setTypes(Set<String> types) {
		this.types = types;
	}
	public String toString() {
		if ( getType() != null ) {
			return getType();
		}
		return super.toString();
	}
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Type)) {
			return false;
		}
		Type other = (Type) object;
		if ( type == null ) {
			return false;
		}
		return type.equals(other.getType());
	}

}
