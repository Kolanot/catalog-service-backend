package org.apache.marmotta.util.solr.schema;

import java.util.Map;

public class SolrField extends SolrSchemaElement {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SolrField() {
        // TODO Auto-generated constructor stub
    }

    public SolrField(Map<String, Object> map) {
        super(map);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean isValidProperty(String name) {
        switch (name) {
        case "name":
        case "type":
        case "indexed":
        case "stored":
        case "compressed":
        case "multiValued":
        case "omitNorms":
        case "termVectors":
        case "termPositions":
        case "termOffset":
        case "default":

            return true;
        default:
            return false;

        }
    }
    public void setName(String name) {
        put("name", name);
    }
    public void setType(String type) {
        put("type", type);
    }
    public String getName() {
        return getString("name");
    }
    public String getType() {
        return getString("type");
    }

}
