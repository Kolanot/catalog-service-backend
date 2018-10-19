package org.apache.marmotta.knowledge.vis.model;

import java.util.Locale;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.marmotta.knowledge.vis.ns.DCTERMS;
import org.apache.marmotta.knowledge.vis.ns.RDFS;
import org.apache.marmotta.knowledge.vis.ns.VIS;
import org.openrdf.annotations.Iri;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.object.LangString;
import org.openrdf.repository.object.RDFObject;

@Iri(VIS.Resource)
public interface VisResource extends RDFObject {
	/**
	 * Retrieve the default locale for this
	 * particular object
	 * @return
	 */
	@Iri(VIS.language)
	Locale getDefaultLocale();
	/**
	 * Set the default locale for this
	 * particular object
	 * @param locale
	 */
	@Iri(VIS.language)
	public void setDefaultLocale(Locale locale);
	/**
	 * Retrieve the list of available locales
	 * @return
	 */
	@Iri(VIS.hasLanguage)
	Set<Locale> getAvailableLocales();
	/**
	 * Set the list of available locales
	 * @param locales
	 */
	@Iri(VIS.hasLanguage)
	public void setAvailableLocales(Set<Locale> locales);

	/**
	 * Retrieve the main rdf:type property for this object. This
	 * type is used when working with the objects.
	 * @return
	 */
	@Iri(VIS.visType)
	String getType();
	@Iri(VIS.visType)
	public void setType(String type);
	/**
	 * The set of rdfTypes to use when creating an instance
	 * of this project. The main type {@link VisResource#getType()}
	 * is not added automatically to this set.
	 * 
	 * @return
	 */
	@Iri(VIS.rdfType)
	public Set<Object> getRdfTypes();
	@Iri(VIS.rdfType)
	public void setRdfTypes(Set<Object> rdfTypes);
	
	
	@Iri(VIS.editable)
	Boolean getEditable();
	@Iri(VIS.editable)
	public void setEditable(Boolean editable);
	
	@Iri(VIS.labelProperty)
	VisPropertyType getLabelPropertyType();
	@Iri(VIS.labelProperty)
	public void setLabelPropertyType(VisPropertyType labelPropertyType);
	@Iri(VIS.descriptionProperty)
	VisPropertyType getCommentPropertyType();
	@Iri(VIS.descriptionProperty)
	public void setCommentPropertyType(VisPropertyType labelPropertyType);

	
	/**
	 * check whether the resource is editable
	 * @return
	 */
	boolean isEditable();
	/**
	 * Retrieve the complete list of all rdf:types for this resource
	 * @return
	 */
	Set<String> getTypes();
	/**
	 * The proper list of available locales for the resource
	 * @return
	 */
	public Set<Locale> getLocales();
	
	/**
	 * @return the created
	 */
	@Iri(DCTERMS.created)
	XMLGregorianCalendar getCreated();

	/**
	 * @param created the created to set
	 */
	@Iri(DCTERMS.created)
	void setCreated(XMLGregorianCalendar created);

	/**
	 * @return the modified
	 */
	@Iri(DCTERMS.modified)
	XMLGregorianCalendar getModified();

	/**
	 * @param modified the modified to set
	 */
	@Iri(DCTERMS.modified)
	void setModified(XMLGregorianCalendar modified);

	/**
	 * @return the identifier
	 */
	@Iri(DCTERMS.identifier)
	String getIdentifier();

	/**
	 * @param identifier the identifier to set
	 */
	@Iri(DCTERMS.identifier)
	void setIdentifier(String identifier);

	/**
	 * @return the language
	 */
	@Iri(DCTERMS.language)
	Locale getLanguage();


	/**
	 * @param language the language to set
	 */
	@Iri(DCTERMS.language)
	void setLanguage(Locale language);
	public abstract String getLabel();
	public abstract void setLabel(String label);
	public abstract String getLabel(Locale locale);
	public abstract void setLabel(String label, Locale locale);
    /**
     * The default label for a resource
     * @return
     */
    @Iri(RDFS.label)
    public abstract Set<Object> getRdfLabels();
    @Iri(RDFS.label)
    public abstract void setRdfLabels(Set<Object> label);
    @Iri(RDFS.comment)
    public abstract Set<Object> getRdfComments();
    @Iri(RDFS.comment)
    public abstract void setRdfComment(Set<Object> comment);
    
	public abstract String getComment();
	public abstract void setComment(String label);
	public abstract String getComment(Locale locale);
	public abstract void setComment(String label, Locale locale);
	
	public abstract Resource getRdfsRange();

    @Iri(DCTERMS.description)
    public abstract Set<LangString> getDefinitions();
    @Iri(DCTERMS.description)
    public abstract void setDefinitions(Set<LangString> descriptions);
    /** 
     * Store the definition with the provided {@link Locale}
     * 
     * @param label
     * @param locale
     */
    void setDefinition(String label, Locale locale);
    /**
     * Store the definition with the defalut (resource's) {@link Locale}
     * @param label
     */
    void setDefinition(String label);
    String getDefinition();
    String getDefinition(Locale locale);
    
    
    public String getNTriples();

	Locale checkLanguageUsed();

	URI getResourceContext();

}
