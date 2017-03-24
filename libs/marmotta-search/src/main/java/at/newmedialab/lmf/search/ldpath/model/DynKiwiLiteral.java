package at.newmedialab.lmf.search.ldpath.model;

import org.apache.marmotta.commons.vocabulary.XSD;
import org.apache.marmotta.kiwi.model.rdf.KiWiLiteral;
import org.apache.marmotta.kiwi.model.rdf.KiWiUriResource;

public class DynKiwiLiteral extends KiWiLiteral {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String name;
	
	private String value;

	public DynKiwiLiteral(String name, String value) {
		this(name, value, XSD.String.stringValue());
	}
	public DynKiwiLiteral(String name, String value, String type) {
		this.name = name;
		setType(new KiWiUriResource(type));
		setContent(value);
	}
	@Override
	public String getContent() {
		return value;
	}

	@Override
	public void setContent(String content) {
		this.value = content;
	}

	public String getName() {
		return name;
	}


}
