package org.apache.marmotta.util.solr.schema;

import java.util.Map;

public class SolrFieldType extends SolrSchemaElement {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SolrFieldType() {
        // TODO Auto-generated constructor stub
    }

    public SolrFieldType(Map<String, Object> map) {
        super(map);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean isValidProperty(String name) {
        switch (name) {
        case "name":
        case "class":
        case "omitNorms":
        case "sortMissingLast":
        case "sortMissingFirst":
        case "dimension":
        case "positionIncrementGap":

            return true;
        default:
            return false;

        }
    }
    public String getName() {
        return getString("name");
    }
    public String getType() {
        return getString("type");
    }

}
