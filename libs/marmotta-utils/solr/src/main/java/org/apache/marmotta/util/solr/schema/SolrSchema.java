package org.apache.marmotta.util.solr.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;

public class SolrSchema {
    // private final SolrCoreConfiguration config;
    private final String coreName;
    private SolrCoreAdministration admin;
    private double version;
    private List<SolrField> fields = new LinkedList<>();
    private List<SolrField> dynamicFields = new LinkedList<>();
    private List<SolrCopyField> copyFields = new LinkedList<>();
    private List<FieldTypeRepresentation> types = new LinkedList<>();

    public SolrSchema(String coreName) {
        this.coreName = coreName;
    }
    public SolrSchema(String coreName, SolrCoreAdministration admin) {
        this(coreName);
        this.admin = admin;
    }

    public SolrField getField(String name) {
        for (SolrField f : fields) {
            if (name.equals(f.getName())) {
                return f;
            }
        }
        return null;
    }
    public SolrField getDynamicField(String name) {
        for (SolrField f : dynamicFields) {
            if (name.equals(f.getName())) {
                return f;
            }
        }
        return null;
    }
    public SolrField getDynamicFieldForType(String type) {
        for (SolrField f : dynamicFields) {
            if (type.equals(f.getType())) {
                return f;
            }
        }
        return null;
        
    }
    public SolrCopyField getCopyField(String name) {
        for (SolrCopyField f : copyFields) {
            if (name.equals(f.getSource())) {
                return f;
            }
        }
        return null;
    }
    public FieldTypeRepresentation getFieldType(String name) {
        for (FieldTypeRepresentation f : types) {
            if (f.getAttributes()!= null && name.equals(f.getAttributes().get("name"))) {
                return f;
            }
        }
        return null;
    }
    public boolean hasType(String name) {
        FieldTypeRepresentation t = getFieldType(name);
        return t!=null;
    }
    public boolean hasField(String name) {
        SolrField f = getField(name);
        return f != null;
    }
    public boolean hasDynamicField(String name) {
        SolrField f = getDynamicField(name);
        return f != null;
    }
    public boolean hasDynamicFieldForType(String type) {
        SolrField f = getDynamicFieldForType(type);
        return f != null;
    }
    public boolean hasCopyField(String name) {
        SolrCopyField f  = getCopyField(name);
        return f !=null;
    }
    public boolean addField(SolrField field) throws SolrServerException {
        if ( admin == null ) {
            throw new IllegalStateException("No Admin Client");
        }
        return admin.addField(coreName, field);
    }
    /**
     * 
     * @param oldFields
     * @param newFields
     * @return
     * @throws SolrServerException 
     */
    public boolean updateSchema(List<SolrField> oldFields, List<SolrField> newFields) throws SolrServerException {
        SolrSchemaChange change = new SolrSchemaChange(this, oldFields);
        change.updateSchema(newFields);
        return false;
        
    }
    public boolean updateSchema(List<SolrField> newFields) throws SolrServerException {
        SolrSchemaChange change = new SolrSchemaChange(this);
        change.updateSchema(newFields);
        return false;
    }
    boolean addFields(List<SolrField> toAdd) throws SolrServerException {
        if ( admin == null ) {
            throw new IllegalStateException("No Admin Client");
        }
        return admin.addFields(coreName, toAdd);
    }
    boolean updateFields(List<SolrField> toUpdate) throws SolrServerException {
        if ( admin == null ) {
            throw new IllegalStateException("No Admin Client");
        }
        return admin.updateFields(coreName, toUpdate);
    }
    boolean deleteFields(List<SolrField> toDelete) throws SolrServerException {
        if ( admin == null ) {
            throw new IllegalStateException("No Admin Client");
        }
        return admin.deleteFields(coreName, toDelete);
    }

    public boolean addCopyField(String source, String dest) throws SolrServerException {
        if ( admin == null ) {
            throw new IllegalStateException("No Admin Client");
        }
        return admin.addCopyField(coreName, source, Collections.singletonList(dest));
    }
    public boolean addDynamicField(SolrField defnition) throws SolrServerException {
        if ( admin == null ) {
            throw new IllegalStateException("No Admin Client");
        }
        return admin.addDynamicField(coreName, defnition);
    }

    public List<FieldTypeRepresentation> getTypes() {
        return types;
    }

    public void setTypes(List<FieldTypeRepresentation> types) {
        this.types = types;
    }
    public List<SolrField> getFields() {
        return fields;
    }
    public void setFields(List<SolrField> fields) {
        this.fields = fields;
    }
    public List<SolrField> getDynamicFields() {
        return dynamicFields;
    }
    public void setDynamicFields(List<SolrField> dynamicFields) {
        this.dynamicFields = dynamicFields;
    }
    public List<SolrCopyField> getCopyFields() {
        return copyFields;
    }
    public void setCopyFields(List<SolrCopyField> copyFields) {
        this.copyFields = copyFields;
    }



}
