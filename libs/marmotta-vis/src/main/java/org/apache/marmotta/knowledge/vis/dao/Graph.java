package org.apache.marmotta.knowledge.vis.dao;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class Graph extends Instance<GraphType> {
	private Set<Locale> languages;

	/**
	 * @return the languages
	 */
	public Set<Locale> getLanguages() {
	    if ( languages == null ) {
	        languages = new HashSet<Locale>();
	    }
		return languages;
	}

	/**
	 * @param languages the languages to set
	 */
	public void setLanguages(Set<Locale> languages) {
		this.languages = languages;
	}
//	/**
//	 * 
//	 * @return
//	 */
//	
//	public Set<BaseItem> getNetworks() {
//		return getItems();
//	}
	
}
