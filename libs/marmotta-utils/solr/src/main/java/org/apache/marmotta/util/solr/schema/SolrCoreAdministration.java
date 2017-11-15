package org.apache.marmotta.util.solr.schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.ConfigSetAdminResponse;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.SimpleSolrResponse;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.CopyFieldsResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.DynamicFieldsResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.FieldTypesResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.FieldsResponse;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

public class SolrCoreAdministration {
    final SolrClient client;
    public SolrCoreAdministration(SolrClient client) {
        if ( client == null ) {
//            client =  new HttpSolrClient.Builder().withBaseSolrUrl("http://localhost:8983/solr").build();
            client = new CloudSolrClient.Builder().withSolrUrl("http://localhost:8983/solr").build();
        }
        this.client = client;
    }
    public SolrSchema getSchema(String coreName) throws SolrServerException {
        SolrSchema schema = new SolrSchema(coreName, this);
        schema.setFields(getFields(coreName));
        schema.setDynamicFields(getDynamicFields(coreName));
        schema.setCopyFields(getCopyFields(coreName));
        schema.setTypes(getFieldTypes(coreName));
        return schema;
    }
    public boolean coreExists(String coreName) throws SolrServerException {
        List<String> cores = getCores();
        return cores.contains(coreName);
    }
    public List<FieldTypeRepresentation> getFieldTypes(String core) throws SolrServerException {
        try {
            FieldTypesResponse resp = new SchemaRequest.FieldTypes().process(client, core);
            return resp.getFieldTypes();
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }
    public List<SolrField> getDynamicFields(String core) throws SolrServerException {
        try {
            List<SolrField> copyFieldList = new LinkedList<>();
            DynamicFieldsResponse resp = new SchemaRequest.DynamicFields().process(client, core);
            // consume all maps
            for (Map<String,Object> cp :resp.getDynamicFields()) {
                copyFieldList.add(new SolrField(cp));
            }
            return copyFieldList;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }
    public List<SolrCopyField> getCopyFields(String core) throws SolrServerException {
        try {
            List<SolrCopyField> copyFieldList = new LinkedList<>();
            CopyFieldsResponse resp = new SchemaRequest.CopyFields().process(client, core);
            // consume all maps
            for (Map<String,Object> cp :resp.getCopyFields()) {
                copyFieldList.add(new SolrCopyField(cp));
            }
            return copyFieldList;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }
    public List<SolrField> getFields(String core) throws SolrServerException {
        try {
            List<SolrField> fieldList = new LinkedList<>();
            FieldsResponse resp = new SchemaRequest.Fields().process(client, core);
            // consume all maps
            for (Map<String,Object> cp :resp.getFields()) {
                fieldList.add(new SolrField(cp));
            }
            return fieldList;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }

    /**
     * Create a new 
     * @param core Names the collection (in solr cloud mode) or the core name where to add the new field
     * @param name
     * @param solrType
     * @param fieldConfig
     * @return
     * @throws SolrServerException
     */
    public boolean addField(String core, Map<String, Object> fieldDefinition) throws SolrServerException {
        try {
            // try to create the schema field
            SchemaResponse.UpdateResponse resp = new SchemaRequest.AddField(fieldDefinition).process(client, core);
            return resp.getStatus() == 0;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }
    public boolean addDynamicField(String core, Map<String, Object> fieldDefinition) throws SolrServerException {
        try {
            // try to create the schema field
            SchemaResponse.UpdateResponse resp = new SchemaRequest.AddDynamicField(fieldDefinition).process(client, core);
            return resp.getStatus() == 0;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }

    public boolean removeField(String core, SolrField fieldDefinition) throws SolrServerException {
        try {
            // try to create the schema field
            SchemaResponse.UpdateResponse resp = new SchemaRequest.DeleteField(fieldDefinition.getName()).process(client, core);
            return resp.getStatus() == 0;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }
    public boolean addCopyField(String core, String fieldName, List<String> destination) throws SolrServerException {
        try {
            SchemaResponse.UpdateResponse resp = new SchemaRequest.AddCopyField(fieldName, destination).process(client, core);
            return resp.getStatus() == 0;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
        
    }
    public boolean addFieldType(String core, FieldTypeDefinition definition) throws SolrServerException {
        try {
            SchemaResponse.UpdateResponse resp = new SchemaRequest.AddFieldType(definition).process(client, core);
            return resp.getStatus() == 0;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }
    /**
     * Obtain the list of currently available cores / collections
     * @return
     * @throws SolrServerException
     */
    public List<String> getCores() throws SolrServerException {
        try {
            if ( isCloudMode()) {
                return CollectionAdminRequest.listCollections(client);
            }
            else {
                CoreAdminRequest request = new CoreAdminRequest();
                request.setAction(CoreAdminAction.STATUS);
                CoreAdminResponse cores = request.process(client);
                // List of the cores
                List<String> coreList = new ArrayList<String>();
                for (int i = 0; i < cores.getCoreStatus().size(); i++) {
                    coreList.add(cores.getCoreStatus().getName(i));
                }
                return coreList;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new ArrayList<String>();
        
        
    }
    public boolean create(String core, String path) throws SolrServerException {
        try {
            CoreAdminResponse response = CoreAdminRequest.createCore(core, path, client);
//            CoreAdminRequest request = new CoreAdminRequest();
//            request.setAction(CoreAdminAction.CREATE);
//            request.setCoreName(core);
//            request.setPath(path);
//            CoreAdminResponse response = request.process(client);
            return response.getStatus() == 0;
        } catch (IOException e) {
            throw new SolrServerException(e);
        }
        
    }
    public boolean reload(String core) throws SolrServerException {
        try {
            CoreAdminRequest request = new CoreAdminRequest();
            request.setAction(CoreAdminAction.RELOAD);
            request.setCoreName(core);
            CoreAdminResponse response = request.process(client, core);
            return response.getStatus() == 0;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }
    public void collectionExists(String collection) throws SolrServerException {
        try {
            List<String> collections = CollectionAdminRequest.listCollections(client);
            collections.size();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
   
    
    public List<String> listConfigSets() throws SolrServerException {
        try {
            ConfigSetAdminResponse resp = new ConfigSetAdminRequest.List().process(client);
            NamedList<Object> obj = resp.getResponse();
            List<String> configs = (List<String>) obj.findRecursive("configSets");
            return configs;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SolrServerException(e);
        }
    }
    /**
     * Test whether remote SOLR instance is running in cloud mode
     * @return
     * @throws SolrServerException
     */
    public boolean isCloudMode() throws SolrServerException {
        // Core Overview
        if ( client instanceof CloudSolrClient ) {
            return true;
        }
        if ( client instanceof EmbeddedSolrServer) {
            return false;
        }
        // CoreAdminRequest.getStatus("LIST", client);
        GenericSolrRequest system = new GenericSolrRequest(METHOD.GET, "/admin/info/system", new ModifiableSolrParams());
        SimpleSolrResponse rsp;
        try {
            rsp = system.process(client);
            String mode = (String) rsp.getResponse().findRecursive("mode");
            if ( "solrcloud".equals(mode)) {
                return true;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    
    protected void info() throws SolrServerException {
        // Core Overview
        GenericSolrRequest system = new GenericSolrRequest(METHOD.GET, "/admin/info/system", new ModifiableSolrParams());
        SimpleSolrResponse rsp;
        try {
            rsp = system.process(client);
            String instanceDir = (String) rsp.getResponse().findRecursive("mode");
            String zkHost = (String) rsp.getResponse().findRecursive("zkHost");
            System.out.println(instanceDir);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
   
    }

    public static void main(String [] args) throws Exception {
        SolrCoreAdministration a = new SolrCoreAdministration(null);
        List<String> cores = a.getCores();
        for (String c : cores) {
            System.out.println(c);
            List<FieldTypeRepresentation> r = a.getFieldTypes(c);
            for (FieldTypeRepresentation rep : r) {
                System.out.println(rep.getAttributes().toString());
                if ( rep.getFields() != null ) {
                    System.out.println(rep.getFields());
                }
            }
            List<SolrField> fieldMap = a.getFields(c);
            for ( SolrField field : fieldMap) {
                for (String key : field.keySet()) {
                    System.out.print(key + ": " + field.get(key) + "; ");
                }
                System.out.println();
            }
        }
        a.listConfigSets();
    }

}
