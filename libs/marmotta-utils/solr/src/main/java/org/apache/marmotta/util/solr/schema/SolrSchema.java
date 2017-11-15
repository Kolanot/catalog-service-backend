package org.apache.marmotta.util.solr.schema;

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
    public boolean addCopyField(String source, String dest) throws SolrServerException {
        if ( admin == null ) {
            throw new IllegalStateException("No Admin Client");
        }
        return admin.addCopyField(coreName, source, Collections.singletonList(dest));
    }
    public boolean addDynamicField(String coreName, SolrField defnition) throws SolrServerException {
        if ( admin == null ) {
            throw new IllegalStateException("No Admin Client");
        }
        return admin.addDynamicField(coreName, defnition);
    }
//
//    public void loadSchema(File file) throws MarmottaException {
//
//        SAXBuilder parser = new SAXBuilder(XMLReaders.NONVALIDATING);
//        try {
//            Document doc = parser.build(file);
//
//            Element schemaNode = doc.getRootElement();
//            version = schemaNode.getAttribute("version").getDoubleValue();
//            if (schemaNode.getAttribute("version").getDoubleValue() < 1.6) {
//                //
//                Element fieldsNode = schemaNode.getChild("fields");
//                if (!schemaNode.getName().equals("schema") || fieldsNode == null)
//                    throw new MarmottaException(file.getName() + " is an invalid SOLR schema file");
//
//            }
//            // check for fields
//            List<Element> fieldElements = getChildren(schemaNode, "field");
//            for (Element fieldElement : fieldElements) {
//                Field schemaField = fromAttributes(fieldElement);
//                fields.add(schemaField);
//            }
//            // check for fieldTypes
//            List<Element> fieldTypeElements = getChildren(schemaNode, "fieldType");
//            for (Element fieldTypeElement : fieldTypeElements) {
//                Field fieldType = fromAttributes(fieldTypeElement);
//                FieldTypeRepresentation rep = new FieldTypeRepresentation();
//                rep.setAttributes(fieldType);
//                types.add(rep);
//            }
//            //
//            List<Element> dynamicFieldElements = getChildren(schemaNode, "dynamicField");
//            for (Element dynamicFieldElement : dynamicFieldElements) {
//                Field fieldType = fromAttributes(dynamicFieldElement);
//                dynamicFields.add(fieldType);
//            }
//            List<Element> copyFieldElements = getChildren(schemaNode, "copyField");
//            for (Element copyFieldElement : copyFieldElements) {
//                Field fieldType = fromAttributes(copyFieldElement);
//                copyFields.add(fieldType);
//            }
//        } catch (JDOMException e) {
//            throw new MarmottaException("parse error while parsing SOLR schema template file " + file.getAbsolutePath(),
//                    e);
//        } catch (IOException e) {
//            throw new MarmottaException("I/O error while parsing SOLR schema template file " + file.getAbsolutePath(),
//                    e);
//        }
//
//    }


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
