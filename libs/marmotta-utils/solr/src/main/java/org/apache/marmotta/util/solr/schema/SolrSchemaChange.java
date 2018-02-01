package org.apache.marmotta.util.solr.schema;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
/**
 * Helper class to process schema changes
 * @author dglachs
 *
 */
class SolrSchemaChange {
    private final SolrSchema schema;
    private final List<SolrField> existing;

    public SolrSchema getSchema() {
        return schema;
    }
    /**
     * Construct a schema change model with 
     * empty list of existing fields
     * 
     * @param schema
     */
    public SolrSchemaChange(SolrSchema schema) {
        this(schema, new ArrayList<SolrField>());
    }
    /**
     * Construct schema change model with list of already 
     * created list of field / dynamic fields
     * @param schema
     * @param existing
     */
    public SolrSchemaChange(SolrSchema schema, List<SolrField> existing) {
        this.schema = schema;
        this.existing = existing;
    }

    /**
     * Update the schema with the provided list of new fields / dynamic fields.
     * The new list is compared with the schema and the already created fields
     * and computes additions, changes and deletions accordingly.
     * 
     * @param newFields
     * @return
     * @throws SolrServerException 
     */
    public boolean updateSchema(List<SolrField> newFields) throws SolrServerException {
        final List<SolrField> toAdd = new ArrayList<>();
        final List<SolrField> update= new ArrayList<>();

        for ( SolrField field : newFields ) {
            SolrField schemaField = null;
            if (field.isDynamic()) {
                schemaField = schema.getDynamicField(field.getName());
            }
            else {
                schemaField = schema.getField(field.getName());
            }
            // the new field is not in the schema and not in the list of existing
            if ( schemaField==null && findByName(field.getName())==null) {
                toAdd.add(field);
            }
            else {
                SolrField stored = findByName(field.getName());
                if ( stored != null ) {
                    if (! stored.equals(field)) {
                        update.add(field);
                        existing.remove(stored);
                        
                    }
                    else {
                        // no change so delete from existing
                        existing.remove(stored);
                    }
                }
                
            }
        }
        if ( toAdd.size() > 0 ) {
            // new fields detected
            schema.addFields(toAdd);
        }
        if ( update.size() > 0 ) {
            // changed fields
            schema.updateFields(update);
        }
        if ( existing.size()>0 ) {
            // remove obsolete fields
            schema.deleteFields(existing);
        }
        return true;
        
    }
    /**
     * Find an existing element by name
     * @param name
     * @return
     */
    private SolrField findByName(String name) {
        for (SolrField d : existing) {
            if (d.getName().equals(name)) {
                return d;
            }
        }
        return null;
    }
    
}
