package org.apache.marmotta.util.solr.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
/**
 * Helper/Wrapper class covering the SolrSchemaField (attributes)
 * @author dglachs
 *
 */
public abstract class SolrSchemaElement extends HashMap<String,Object> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public SolrSchemaElement() {
        super();
    }
    public SolrSchemaElement(Map<String, Object> map) {
        map.forEach(new BiConsumer<String, Object>() {

            @Override
            public void accept(String t, Object u) {
               if ( isValidProperty(t)) {
                   //
                   put(t,u);
               }
                
            }
            
        });
        putAll(map);
        // TODO Auto-generated constructor stub
    }
    public abstract boolean isValidProperty(String name);
    public void setProperty(String key, Object value) {
        if ( isValidProperty(key)) {
            // add if a valid property
            put(key, value);
        }
    }
    
    public Boolean getBoolean(String name) {
        Object value = get(name);
        if ( value!= null && value instanceof Boolean) {
            return (Boolean)value;
        }
        return null;
    }
    public String getString(String name) {
        Object value = get(name);
        if ( value!= null && value instanceof String) {
            return (String)value;
        }
        return null;
    }

    public abstract String getName();
}
