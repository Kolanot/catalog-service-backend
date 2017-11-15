package org.apache.marmotta.util.solr.schema;

import java.util.Map;

public class SolrCopyField extends SolrSchemaElement {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SolrCopyField() {
    }
    public SolrCopyField(String source, String destination) {
        setProperty("source", source);
        setProperty("destination", destination);
    }
    public SolrCopyField(Map<String, Object> map) {
        super(map);
    }

    @Override
    public boolean isValidProperty(String name) {
        switch (name) {
        case "source":
        case "dest":
            return true;
        default:
            return false;

        }
    }
    public String getSource() {
        return getString("source");
    }
    public String getDestination() {
        return getString("dest");
    }

}
