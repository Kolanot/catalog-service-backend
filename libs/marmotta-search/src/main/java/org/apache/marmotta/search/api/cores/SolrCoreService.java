/**
 * Copyright (C) 2013 Salzburg Research.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.search.api.cores;

import java.util.List;

import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.marmotta.platform.core.exception.MarmottaException;
import org.apache.marmotta.search.exception.CoreAlreadyExistsException;
import org.apache.marmotta.search.services.cores.SolrCoreConfiguration;
import org.apache.solr.client.solrj.SolrClient;
import org.openrdf.model.Value;

/**
 * The SolrCoreService allows to manage SOLR cores for different path programs.
 * <p/>
 * User: sschaffe
 */
public interface SolrCoreService {


    /**
     * Return a list of all core names that are currently activated in the system.
     *
     * @return
     */
    public List<SolrCoreConfiguration> listSolrCores();


    /**
     * Return true if the enhancement engine with the given name exists.
     * @param name
     * @return
     */
    public boolean hasSolrCore(String name);

    /**
     * Return the configuration of the SOLR core with the given name, or null in case an engine with this
     * name does not exist.
     *
     * @param name
     * @return
     */
    public SolrCoreConfiguration getSolrCore(String name);


    /**
     * Create and add the SOLR core with the name and program passed as argument. Throws CoreAlreadyExistsException
     * in case the engine already exists and LDPathParseException in case the program is not correctly parsed.
     *
     * @param name
     * @param program
     * @return the newly created SOLR core engine
     */
    public SolrCoreConfiguration createSolrCore(String name, String program) throws CoreAlreadyExistsException, LDPathParseException;


    /**
     * Update the configuration of the SOLR core given as argument.
     * <p/>
     * Note that this method merely updates the configuration and does not automatically re-run the indexing
     * process for all resources.
     *
     * @param engine
     */
    public void updateSolrCore(SolrCoreConfiguration engine, String program);


    /**
     * Remove the SOLR core configuration with the given name.
     * <p/>
     * Note that this method merely updates the configuration and does not automatically re-run the indexing
     * process for all resources.
     *
     * @param engine
     */
    public void removeSolrCore(SolrCoreConfiguration engine);

    /**
     * Create a {@link SolrClient} for the provided {@link SolrCoreConfiguration}. Depending
     * on {@link SolrCoreConfiguration#getSolrClientType()} the corresponding 
     * {@link SolrClient} is returned. 
     * @param config The core configuration
     * @return SolrClient for the core config
     * @throws MarmottaException
     */
    public SolrClient getSolrClient(SolrCoreConfiguration config);
    /**
     * Retrieve the link to the remote SOLR core instance. Returns null if the 
     * core is running in EMBEDDED mode.
     * @param name
     * @return The external core uri or <code>null</code> if local/embedded
     */

    public String getSolrCoreUri(String name);

}