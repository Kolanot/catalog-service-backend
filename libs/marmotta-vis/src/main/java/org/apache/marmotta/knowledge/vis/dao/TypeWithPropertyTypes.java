package org.apache.marmotta.knowledge.vis.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.marmotta.knowledge.vis.ns.DCTERMS;
import org.apache.marmotta.knowledge.vis.ns.RDFS;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class TypeWithPropertyTypes extends Type {
    // map for propertytypes, where the propertyType.getType is thekey
    private Map<String, PropertyType> propertyTypes;
    private transient String labelUri;
    private transient String commentUri;

    /**
     * @return the properties
     */
    @XmlTransient
    public Map<String, PropertyType> getPropertyTypes() {
        if (propertyTypes == null) {
            propertyTypes = new HashMap<String, PropertyType>();
        }
        return propertyTypes;
    }
    /**
     * Retrieve the edgeType's as a map with {@link EdgeType#getId()}
     * as the key
     * @return
     */
    @JsonProperty(value="propertyType")
    public Map<String, PropertyType> getPropertyTypeMap() {
        Map<String, PropertyType> map = new HashMap<String, PropertyType>();
        for (PropertyType item : propertyTypes.values()) {
            map.put(item.getId(), item);
        }
        return map;
    }    /**
     * @param properties
     *            the properties to set
     */
    public void setPropertyTypes(Map<String, PropertyType> properties) {
        this.propertyTypes = properties;
    }

    public void addPropertyType(PropertyType pt) {
        getPropertyTypes().put(pt.getType(), pt);
    }

    @XmlTransient
    public Set<String> getPropertyTypesFormatted() {
        Set<String> targets = new HashSet<>();
        for (PropertyType nt : propertyTypes.values()) {
            targets.add(String.format("<%s>", nt.getType()));
        }
        return targets;
    }

    @XmlTransient
    public PropertyType getPropertyType(String property) {
        return getPropertyType(property, null);
    }

    @XmlTransient
    public PropertyType getPropertyType(String property, String fallback) {
        PropertyType pt = propertyTypes.get(property);
        if (pt != null) {
            return pt;
        } else if (fallback != null) {
            pt = propertyTypes.get(fallback);
        }
        if (pt == null) {

            pt = new PropertyType();
            pt.setDisplayType(DisplayTypeEnum.STRING);
            pt.setEditable(true);
            pt.setMultiLingual(true);
            pt.setMultiValue(false);
            pt.setRequired(true);
            pt.setLocale(getLocale());
            pt.setId(fallback);

            switch (fallback) {
            case RDFS.label:
                pt.setLabel("RDFS Label");
                pt.setComment("RDFS Label");
                pt.setType(fallback);
                return pt;
            case DCTERMS.title:
                pt.setLabel("DCTERMS Title");
                pt.setComment("DCTERMS Title");
                pt.setType(fallback);
                return pt;
            case RDFS.comment:
                pt.setLabel("RDFS Comment");
                pt.setComment("RDFS Comment");
                pt.setType(fallback);
                return pt;
            case DCTERMS.description:
                pt.setLabel("DCTERMS Description");
                pt.setComment("DCTERMS Description");
                pt.setType(fallback);
                return pt;

            default:
                break;
            }
        }
        return pt;
    }

    @XmlTransient
    public PropertyType getPropertyTypeById(String id) {
        for (PropertyType nt : propertyTypes.values()) {
            if (nt.getId().equals(id)) {
                return nt;
            }
        }
        throw new IllegalStateException(String.format("Property with ID <%s> not found!", id));
    }

    /**
     * @return the labelUri
     */
    @XmlTransient
    public String getLabelProperty() {
        if (labelUri == null) {
            return RDFS.label;
        }
        return labelUri;
    }

    /**
     * @param labelUri
     *            the labelUri to set
     */
    public void setLabelProperty(String labelUri) {
        this.labelUri = labelUri;
    }

    /**
     * @return the commentUri
     */
    @XmlTransient
    public String getCommentProperty() {
        if (commentUri == null) {
            return RDFS.comment;
        }
        return commentUri;
    }

    /**
     * @param commentUri
     *            the commentUri to set
     */
    public void setCommentProperty(String commentUri) {
        this.commentUri = commentUri;
    }

    @XmlTransient
    public String getLabelUriFormatted() {
        return String.format("<%s>", getLabelProperty());
    }

    @XmlTransient
    public String getCommentUriFormatted() {
        return String.format("<%s>", getCommentProperty());
    }

}
