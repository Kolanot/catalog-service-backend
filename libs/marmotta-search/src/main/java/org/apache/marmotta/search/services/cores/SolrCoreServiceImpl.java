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
package org.apache.marmotta.search.services.cores;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.marmotta.commons.sesame.filter.SesameFilter;
import org.apache.marmotta.commons.sesame.filter.resource.UriPrefixFilter;
import org.apache.marmotta.commons.util.HashUtils;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.fields.FieldMapping;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.apache.marmotta.platform.core.api.triplestore.SesameService;
import org.apache.marmotta.platform.core.events.ConfigurationChangedEvent;
import org.apache.marmotta.platform.core.events.SystemStartupEvent;
import org.apache.marmotta.platform.core.exception.MarmottaException;
import org.apache.marmotta.platform.core.model.filter.MarmottaLocalFilter;
import org.apache.marmotta.platform.core.qualifiers.event.Created;
import org.apache.marmotta.platform.core.qualifiers.event.Removed;
import org.apache.marmotta.platform.core.qualifiers.event.Updated;
import org.apache.marmotta.platform.ldcache.model.filter.MarmottaNotCachedFilter;
import org.apache.marmotta.search.api.cores.SolrCoreService;
import org.apache.marmotta.search.api.program.SolrProgramService;
import org.apache.marmotta.search.exception.CoreAlreadyExistsException;
import org.apache.marmotta.search.filters.LMFSearchFilter;
import org.apache.marmotta.search.services.cores.SolrCoreConfiguration.SolrClientType;
import org.apache.marmotta.search.services.program.SolrProgramServiceImpl;
import org.apache.marmotta.util.solr.SuggestionRequestHandler;
import org.apache.marmotta.util.solr.schema.SolrCopyField;
import org.apache.marmotta.util.solr.schema.SolrCoreAdministration;
import org.apache.marmotta.util.solr.schema.SolrField;
import org.apache.marmotta.util.solr.schema.SolrSchema;
import org.apache.marmotta.util.solr.suggestion.params.SuggestionRequestParams;
import org.apache.solr.analysis.SolrAnalyzer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.SolrCore;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

/**
 * Add file description here!
 * <p/>
 * User: sschaffe
 */
@ApplicationScoped
public class SolrCoreServiceImpl implements SolrCoreService {

    private static final Set<String> SOLR_FIELD_OPTIONS;
    static {
        HashSet<String> opt = new HashSet<String>();
        opt.add("default");
        opt.add("indexed");
        opt.add("stored");
        opt.add("compressed");
        opt.add("compressThreshold");
        opt.add("multiValued");
        opt.add("omitNorms");
        opt.add("omitTermFreqAndPositions");
        opt.add("termVectors");
        opt.add("termPositions");
        opt.add("termOffsets");

        SOLR_FIELD_OPTIONS = Collections.unmodifiableSet(opt);
    }
    private static final String SOLR_COPY_FIELD_OPTION = "copy";

    private static final String SOLR_SUGGESTION_FIELD_OPTION = "suggestionType";

    @Inject
    private Logger log;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private SolrProgramService solrProgramService;

    // we use this to get access to the embedded SOLR core container
    @Inject
    private LMFSearchFilter searchFilter;

    @Inject
    private SesameService sesameService;

    @Inject
    @Created
    private Event<SolrCoreConfiguration> coreCreatedEvent;

    @Inject
    @Updated
    private Event<SolrCoreConfiguration> coreUpdatedEvent;

    @Inject
    @Removed
    private Event<SolrCoreConfiguration> coreRemovedEvent;

    private Map<String, SolrCoreConfiguration> engines = null;

    private File solrCoreZipTmpl = null;

    /**
     * Don't listen to any configuration changes while storing data
     */
    private boolean storing = false;

    public SolrCoreServiceImpl() {
    }

    @PostConstruct
    public void initialize() {
        log.info("Cloud SOLR Configuration Service initializing (engines: {})",
                configurationService.getListConfiguration("solr.cores"));

        engines = new HashMap<String, SolrCoreConfiguration>();
        // check the type of the client (EMBEDDED, REMOTE(singlenode),
        // CLOUD(solr_cluster)
        String serverType = configurationService.getStringConfiguration("solr.server_type");
        // get the URI of the remote solr instance (for REMOTE && CLOUD)
        String serverUri = configurationService.getStringConfiguration("solr.server_uri");

        if (serverUri == null || serverUri.length() == 0) {
            // when no URI provided switch to EMBEDDED
            serverType = SolrCoreConfiguration.SolrClientType.EMBEDDED.name();
            serverUri = null;
        }

        // load all enhancement engines from the configuration
        for (String engineName : configurationService.getListConfiguration("solr.cores")) {
            SolrCoreConfiguration engine = loadExistingSolrCoreConfiguration(engineName);

            // ensure the core file system structures exist and the core is activated in
            // SOLR
            createAndActivateCore(engine);

            engines.put(engineName, engine);
        }

        configurationService.setRuntimeFlag("solr.initialized", true);
    }

    @PreDestroy
    protected void shutdown() {
        if (solrCoreZipTmpl != null && solrCoreZipTmpl.exists()) {
            if (solrCoreZipTmpl.delete()) {
                log.debug("Deleted tmp-file {} (solr core template)", solrCoreZipTmpl);
            }
        }
    }

    public void startup(@Observes SystemStartupEvent event) {
        // trigger initialisation
    }

    /**
     * Create the configuration for an existing SolrCore, use the parameters stored
     * in the configuration
     * 
     * @param name
     * @return
     */
    private SolrCoreConfiguration loadExistingSolrCoreConfiguration(String name) {
        SolrCoreConfiguration engine = new SolrCoreConfiguration(name);
        engine.setThreads(configurationService.getIntConfiguration("solr.core." + name.toLowerCase() + ".workers", 2));
        engine.setUpdateDependencies(configurationService
                .getBooleanConfiguration("solr.core." + name.toLowerCase() + ".update_dependencies", false));
        engine.setClearBeforeReschedule(configurationService
                .getBooleanConfiguration("solr.core." + name.toLowerCase() + ".clear_before_reschedule", true));
        engine.setQueueSize(
                configurationService.getIntConfiguration("solr.core." + name.toLowerCase() + ".queuesize", 100000));

        // initialise filters
        Set<SesameFilter<Resource>> filters = new HashSet<SesameFilter<Resource>>();
        if (configurationService.getBooleanConfiguration("solr.core." + name.toLowerCase() + ".local_only", true)) {
            filters.add(MarmottaLocalFilter.getInstance());
        }
        if (configurationService.getBooleanConfiguration("solr.core." + name.toLowerCase() + ".omit_cached", true)) {
            filters.add(MarmottaNotCachedFilter.getInstance());
        }
        if (configurationService.getListConfiguration("solr.core." + name.toLowerCase() + ".accept_prefixes")
                .size() > 0) {
            filters.add(new UriPrefixFilter(new HashSet<String>(configurationService
                    .getListConfiguration("solr.core." + name.toLowerCase() + ".accept_prefixes"))));
        }

        if (!configurationService.getStringConfiguration("solr.core." + name.toLowerCase() + ".program", "")
                .equals(engine.getProgramString())) {
            engine.setProgramString(
                    configurationService.getStringConfiguration("solr.core." + name.toLowerCase() + ".program", ""));

            try {
                engine.setProgram(solrProgramService.parseProgram(new StringReader(engine.getProgramString())));
            } catch (LDPathParseException e) {
                log.error("error parsing path program for engine {}", engine.getName(), e);
            }

        }
        //
        String serverType = configurationService
                .getStringConfiguration("solr.core." + name.toLowerCase() + ".server_type", "EMBEDDED");
        engine.setSolrClientType(serverType);
        String serverUri = configurationService
                .getStringConfiguration("solr.core." + name.toLowerCase() + ".server_uri");
        engine.setSolrClientURI(serverUri);

        if (configurationService.getBooleanConfiguration("solr.core." + name.toLowerCase() + ".schedule_program_filter",
                false)) {
            filters.add(new LDPathProgramFilter(engine, sesameService));
        }

        engine.setFilters(filters);
        return engine;
    }

    /**
     * Load/reload the SOLR core configuration from the configuration file. This
     * method does not automatically create the necessary file system structures or
     * register/reload the configuration with SOLR. The caller needs to take care of
     * this task.
     * 
     * @param name
     * @param engine
     */
    private void loadSolrCoreConfiguration(String name, SolrCoreConfiguration engine) {
        engine.setThreads(configurationService.getIntConfiguration("solr.core." + name.toLowerCase() + ".workers", 2));
        engine.setUpdateDependencies(configurationService
                .getBooleanConfiguration("solr.core." + name.toLowerCase() + ".update_dependencies", false));
        engine.setClearBeforeReschedule(configurationService
                .getBooleanConfiguration("solr.core." + name.toLowerCase() + ".clear_before_reschedule", true));
        engine.setQueueSize(
                configurationService.getIntConfiguration("solr.core." + name.toLowerCase() + ".queuesize", 100000));

        // initialise filters
        Set<SesameFilter<Resource>> filters = new HashSet<SesameFilter<Resource>>();
        if (configurationService.getBooleanConfiguration("solr.core." + name.toLowerCase() + ".local_only", true)) {
            filters.add(MarmottaLocalFilter.getInstance());
        }
        if (configurationService.getBooleanConfiguration("solr.core." + name.toLowerCase() + ".omit_cached", true)) {
            filters.add(MarmottaNotCachedFilter.getInstance());
        }
        if (configurationService.getListConfiguration("solr.core." + name.toLowerCase() + ".accept_prefixes")
                .size() > 0) {
            filters.add(new UriPrefixFilter(new HashSet<String>(configurationService
                    .getListConfiguration("solr.core." + name.toLowerCase() + ".accept_prefixes"))));
        }

        if (!configurationService.getStringConfiguration("solr.core." + name.toLowerCase() + ".program", "")
                .equals(engine.getProgramString())) {
            engine.setProgramString(
                    configurationService.getStringConfiguration("solr.core." + name.toLowerCase() + ".program", ""));

            try {
                engine.setProgram(solrProgramService.parseProgram(new StringReader(engine.getProgramString())));
            } catch (LDPathParseException e) {
                log.error("error parsing path program for engine {}", engine.getName(), e);
            }

        }
        //
        String serverType = configurationService
                .getStringConfiguration("solr.core." + name.toLowerCase() + ".server_type", "EMBEDDED");
        engine.setSolrClientType(serverType);
        String serverUri = configurationService
                .getStringConfiguration("solr.core." + name.toLowerCase() + ".server_uri");
        engine.setSolrClientURI(serverUri);

        if (configurationService.getBooleanConfiguration("solr.core." + name.toLowerCase() + ".schedule_program_filter",
                false)) {
            filters.add(new LDPathProgramFilter(engine, sesameService));
        }

        engine.setFilters(filters);

    }

    private void storeSolrCoreConfiguration(SolrCoreConfiguration engine) {
        synchronized (engines) {
            storing = true;

            String prefix = "solr.core." + engine.getName().toLowerCase();

            configurationService.setIntConfiguration(prefix + ".workers", engine.getThreads());
            configurationService.setType(prefix + ".workers", "java.lang.Integer(1|1|*)");
            configurationService.setBooleanConfiguration(prefix + ".update_dependencies",
                    engine.isUpdateDependencies());
            configurationService.setType(prefix + ".update_dependencies", "java.lang.Boolean");
            configurationService.setBooleanConfiguration(prefix + ".clear_before_reschedule",
                    engine.isClearBeforeReschedule());
            configurationService.setType(prefix + ".clear_before_reschedule", "java.lang.Boolean");
            configurationService.setIntConfiguration(prefix + ".queuesize", engine.getQueueSize());
            configurationService.setType(prefix + ".queuesize", "java.lang.Integer(1000|1000|*)");

            boolean localOnly = false, omitCached = false;
            Set<String> acceptPrefixes = new HashSet<String>();

            for (SesameFilter<Resource> filter : engine.getFilters()) {
                if (filter instanceof MarmottaLocalFilter) {
                    localOnly = true;
                } else if (filter instanceof MarmottaNotCachedFilter) {
                    omitCached = true;
                } else if (filter instanceof UriPrefixFilter) {
                    acceptPrefixes = ((UriPrefixFilter) filter).getPrefixes();
                }
            }

            configurationService.setBooleanConfiguration(prefix + ".local_only", localOnly);
            configurationService.setType(prefix + ".local_only", "java.lang.Boolean");
            configurationService.setBooleanConfiguration(prefix + ".omit_cached", omitCached);
            configurationService.setType(prefix + ".omit_cached", "java.lang.Boolean");

            if (acceptPrefixes.size() > 0) {
                configurationService.setListConfiguration(prefix + ".accept_prefixes",
                        new ArrayList<String>(acceptPrefixes));
            } else {
                configurationService.removeConfiguration(prefix + ".accept_prefixes");
            }

            configurationService.setConfiguration(prefix + ".program", engine.getProgramString());
            configurationService.setType(prefix + ".program", "org.marmotta.type.Program");

            // optional
            configurationService.setConfiguration(prefix + "server_type", engine.getSolrClientType().name());
            configurationService.setType(prefix + "server_type", "java.lang.Enum(\"EMBEDDED\"|\"REMOTE\"|\"CLOUD\")");
            if (engine.getSolrClientURI() != null) {
                configurationService.setConfiguration(prefix + "server_uri", engine.getSolrClientURI());
            } else {
                configurationService.removeConfiguration(prefix + "server_uri");
            }
            storing = false;
        }
    }

    /**
     * React to any changes in the configuration that may affect one of the engines
     *
     * @param event
     */
    public void configurationChangedEvent(@Observes ConfigurationChangedEvent event) {
        if (event.containsChangedKeyWithPrefix("solr.core.")) {
            synchronized (engines) {
                if (!storing) {
                    // a configuration option for the enhancer has been changed, check whether we
                    // need to update any engine configuration
                    for (Map.Entry<String, SolrCoreConfiguration> entry : engines.entrySet()) {
                        for (String key : event.getKeys()) {
                            if (key.startsWith("solr.core." + entry.getKey().toLowerCase())) {
                                loadSolrCoreConfiguration(entry.getKey(), entry.getValue());

                                reloadSolrCore(entry.getKey());

                                coreUpdatedEvent.fire(entry.getValue());
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * Return a collection of all configured enhancement engines.
     *
     * @return
     */
    @Override
    public List<SolrCoreConfiguration> listSolrCores() {
        synchronized (engines) {
            return Lists.newArrayList(engines.values());
        }
    }

    /**
     * Return true if the enhancement engine with the given name exists.
     *
     * @param name
     * @return
     */
    @Override
    public boolean hasSolrCore(String name) {
        return engines.containsKey(name);
    }

    /**
     * Return the configuration of the enhancement engine with the given name, or
     * null in case an engine with this name does not exist.
     *
     * @param name
     * @return
     */
    @Override
    public SolrCoreConfiguration getSolrCore(String name) {
        return engines.get(name);
    }

    /**
     * Create and add the enhancement engine with the name and program passed as
     * argument. Throws EngineAlreadyExistsException in case the engine already
     * exists and LDPathParseException in case the program is not correctly parsed.
     *
     * @param name
     * @param program
     * @return the newly created enhancement engine
     */
    @Override
    public SolrCoreConfiguration createSolrCore(String name, String program)
            throws CoreAlreadyExistsException, LDPathParseException {
        if (!engines.containsKey(name)) {
            SolrCoreConfiguration engine = new SolrCoreConfiguration(name);

            engine.setThreads(2);
            engine.setProgramString(program);
            engine.setUpdateDependencies(false);
            engine.setClearBeforeReschedule(true);
            engine.setProgram(solrProgramService.parseProgram(new StringReader(program)));

            Set<SesameFilter<Resource>> filters = new HashSet<SesameFilter<Resource>>();
            filters.add(MarmottaLocalFilter.getInstance());
            filters.add(MarmottaNotCachedFilter.getInstance());
            engine.setFilters(filters);

            engine.setSolrClientType(configurationService.getStringConfiguration("solr.server_type", "EMBEDDED"));
            engine.setSolrClientURI(configurationService.getStringConfiguration("solr.server_uri", null));

            engines.put(engine.getName(), engine);
            storeSolrCoreConfiguration(engine);

            List<String> enabledEngines = new ArrayList<String>(
                    configurationService.getListConfiguration("solr.cores"));
            enabledEngines.add(engine.getName());
            configurationService.setListConfiguration("solr.cores", enabledEngines);

            // make sure the data structures for the core exist before the event is fired
            createAndActivateCore(engine);

            coreCreatedEvent.fire(engine);
            return engine;
        } else {
            throw new CoreAlreadyExistsException("the engine with name " + name + " already exists");
        }
    }

    /**
     * Update the configuration of the enhancement engine given as argument.
     * <p/>
     * Note that this method merely updates the configuration and does not
     * automatically re-run the enhancement process for all resources.
     *
     * @param engine
     */
    @Override
    public void updateSolrCore(SolrCoreConfiguration engine) {
        if (engines.containsKey(engine.getName())) {
            engines.put(engine.getName(), engine);

            storeSolrCoreConfiguration(engine);

            try {
                // update schema.xml and solrconfig.xml with the properties of the program
                if (engine.getSolrClientURI() == null) {
                    // createSolrConfigXml(engine);
                    createSchemaXml(engine);
                }

                reloadSolrCore(engine.getName());

                coreUpdatedEvent.fire(engine);
            } catch (Exception e) {
                log.error("error while initialising SOLR core {}", engine.getName(), e);
                removeSolrCore(engine);
            }
        }
    }

    /**
     * Update the configuration of the enhancement engine given as argument
     * 
     * @param engine
     * @param newProgram
     *            the new program for the engine, replacing the stored engine
     */
    public void updateSolrCore(SolrCoreConfiguration engine, Program<Value> newProgram) {
        // parse the current schema and identify the fields, dynamicFields, copyFields
        // created on behalf of the actual program (engine.getProgram())
        //

        SolrSchema existing = new SolrSchema(engine.getName());

    }

    /**
     * Remove the enhancement engine configuration with the given name.
     * <p/>
     * Note that this method merely updates the configuration and does not
     * automatically re-run the enhancement process for all resources.
     *
     * @param engine
     */
    @Override
    public void removeSolrCore(SolrCoreConfiguration engine) {
        if (engines.containsKey(engine.getName())) {
            engines.remove(engine.getName());

            List<String> enabledEngines = new ArrayList<String>(
                    configurationService.getListConfiguration("solr.cores"));
            enabledEngines.remove(engine.getName());
            configurationService.setListConfiguration("solr.cores", enabledEngines);

            // fire event to allow cleaning up the core, and then unregister and delete the
            // core directory afterwards
            coreRemovedEvent.fire(engine);

            unregisterSolrCore(engine.getName());

            try {
                removeCoreDirectory(engine.getName());
            } catch (IOException ex) {
                log.error("I/O error while trying to remove directory for SOLR core {}", engine.getName());
                log.info("Exception details", ex);
            }

        }
    }

    /**
     * This method creates the file system structure for the core (if it does not
     * exist yet) and activates the core in the embedded SOLR server. It needs to be
     * called after the core configuration is loaded or created, but before the core
     * is used.
     *
     * @param engine
     */
    private void createAndActivateCore(SolrCoreConfiguration engine) {
        boolean activated;

        try {
            if (engine.getSolrClientType() == SolrClientType.EMBEDDED) {
                registerSolrCore(engine);
//                activated = ensureCoreDirectory(engine.getName(), true);
//                if (activated) {
//                    // directory has been created/updated
//                    // update schema.xml and solrconfig.xml with the properties of the program
//                    // createSolrConfigXml(engine);
//                    createSchemaXml(engine);
//
//                }
            } else {
                // check whether the remote system contains the core ...
            }

            // register core with SOLR
            registerSolrCore(engine.getName());

        } catch (MarmottaException ex) {
            log.error("I/O error while trying to set up directory for SOLR core {}", engine.getName());
            removeSolrCore(engine);
        } catch (Exception e) {
            log.error("error while initialising SOLR core {}", engine.getName(), e);
            removeSolrCore(engine);
        }
    }

    
    private File getCoreDirectory(String coreName) {
        String solrHomeName = configurationService.getStringConfiguration("solr.home");
        String coreHomeName = solrHomeName + File.separator + coreName;

        File coreHome = new File(coreHomeName);
        return coreHome;
    }

    /**
     * Ensure that the SOLR directory for the core with the given name exists.
     * Returns true if it was created with a fresh configuration.
     *
     * @param coreName
     * @param unpack
     * @return <code>true</code> if it was created with a fresh configuration.
     * @throws IOException
     */
    private boolean ensureCoreDirectory(String coreName, boolean unpack) throws IOException {
        File coreHome = getCoreDirectory(coreName);
        if (coreHome.exists() && coreHome.isDirectory()) {
            // check readability and version
            if (!(coreHome.canRead() && coreHome.canWrite())) {
                log.error(
                        "SOLR home for core {} is not readable/writeable; please check the permissions in the file system",
                        coreName);
            }

            if (!checkCoreVersion(coreName)) {
                if (unpack) {
                    unpackSolrCore(coreHome);
                    return true;
                } else {
                    log.warn("SOLR home for core {} has been created by an old version of LMF, update required!",
                            coreName);
                    return false;
                }
            } else {
                log.info("SOLR home directory exists and has the right version; no update required");
                return false;
            }
        } else {
            // SOLR home does not exist, so we create it
            if (coreHome.mkdirs() && unpack) {
                unpackSolrCore(coreHome);
                return true;
            } else {
                log.error("could not create SOLR home directory; SOLR will not work properly");
                return false;
            }
        }
    }

    private boolean checkCoreVersion(String coreName) throws IOException {
        File coreHome = getCoreDirectory(coreName);
        if (!coreHome.exists())
            return false;
        File conf = new File(coreHome, "conf");
        if (!conf.exists())
            return false;

        String zsHash = null, zcHash = null;
        ZipFile zf = new ZipFile(getSolrCoreZipTmpl(), ZipFile.OPEN_READ);
        try {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                if (ze.isDirectory()) {
                    continue;
                } else if ("conf/solrconfig-template.xml".equals(ze.getName())) {
                    zcHash = HashUtils.md5sum(zf.getInputStream(ze));
                } else if ("conf/schema-template.xml".equals(ze.getName())) {
                    zsHash = HashUtils.md5sum(zf.getInputStream(ze));
                }
            }
        } finally {
            zf.close();
        }

        // Check if the solrconfig.xml was built from an up-to-date template
        File configTpl = new File(conf, "solrconfig-template.xml");
        if (!configTpl.exists())
            return false;
        String cHash = HashUtils.md5sum(configTpl);
        if (cHash == null || !cHash.equals(zcHash))
            return false;

        // Check if the schema.xml was built from an up-to-date template
        File schemaTpl = new File(conf, "schema-template.xml");
        if (!schemaTpl.exists())
            return false;
        String sHash = HashUtils.md5sum(schemaTpl);
        if (sHash == null || !sHash.equals(zsHash))
            return false;

        return true;
    }

    private void removeCoreDirectory(String coreName) throws IOException {
        File coreHome = getCoreDirectory(coreName);
        FileUtils.deleteDirectory(coreHome);
    }

    /**
     * This method takes as argument a File representing the SOLR home directory and
     * unpacks the kiwi-solr-data zip file that is contained in the kiwi-core.jar
     * file into this directory. It also creates the version information file to
     * determine the KiWi version used for setting up the KiWi home directory.
     *
     * @param directory
     * @throws IOException
     */
    private void unpackSolrCore(File directory) throws IOException {
        ZipFile zipFile = new ZipFile(getSolrCoreZipTmpl(), ZipFile.OPEN_READ);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.isDirectory()) {
                log.info("creating SOLR directory: {}/{}", directory.getAbsolutePath(), entry.getName());
                // This is not robust, just for demonstration purposes.

                File dir = new File(directory, entry.getName());

                dir.mkdirs();

                continue;
            }

            log.debug("extracting SOLR configuration file: {}/{}", directory.getAbsolutePath(), entry.getName());

            File file = new File(directory, entry.getName());
            ByteStreams.copy(zipFile.getInputStream(entry), new FileOutputStream(file));

        }

        zipFile.close();
    }

    private File getSolrCoreZipTmpl() throws IOException {
        if (solrCoreZipTmpl == null || !solrCoreZipTmpl.exists()) {
            solrCoreZipTmpl = File.createTempFile("lmf-solr-core", ".zip");

            InputStream url_is = SolrCoreServiceImpl.class.getResourceAsStream("/lmf-solr-core.zip");

            IOUtils.copy(url_is, new FileOutputStream(solrCoreZipTmpl));
        }
        return solrCoreZipTmpl;
    }

    /**
     * Create/update the schema.xml file for the given core according to the core
     * definition.
     *
     * @param engine
     *            the engine configuration
     */
    private void createSchemaXml(SolrCoreConfiguration engine) throws MarmottaException {
        log.info("generating schema.xml for search program {}", engine.getName());

        SAXBuilder parser = new SAXBuilder(XMLReaders.NONVALIDATING);
        File schemaTemplate = new File(getCoreDirectory(engine.getName()),
                "conf" + File.separator + "schema-template.xml");
        File schemaFile = new File(getCoreDirectory(engine.getName()), "conf" + File.separator + "managed-schema");
        try {
            Document doc = parser.build(schemaTemplate);

            Element schemaNode = doc.getRootElement();
            if (schemaNode.getAttribute("version").getDoubleValue() < 1.6) {
                //
                Element fieldsNode = schemaNode.getChild("fields");
                if (!schemaNode.getName().equals("schema") || fieldsNode == null)
                    throw new MarmottaException(schemaTemplate + " is an invalid SOLR schema file");

            }

            schemaNode.setAttribute("name", engine.getName());

            Program<Value> program = engine.getProgram();

            for (FieldMapping<?, Value> fieldMapping : program.getFields()) {
                String fieldName = fieldMapping.getFieldName();
                String solrType = null;
                try {
                    solrType = solrProgramService.getSolrFieldType(fieldMapping.getFieldType().toString());
                } catch (MarmottaException e) {
                    solrType = null;
                }
                if (solrType == null) {
                    log.error("field {} has an invalid field type; ignoring field definition", fieldName);
                    continue;
                }
                if (fieldMapping.getFieldConfig() != null
                        && fieldMapping.getFieldConfig().containsKey("dynamicField")) {
                    String dynamicFieldName = fieldMapping.getFieldConfig().get("dynamicField");
                    Element dynamicElement = new Element("dynamicField");
                    addField(schemaNode, dynamicElement, dynamicFieldName, solrType, fieldMapping.getFieldConfig());

                } else {
                    Element fieldElement = new Element("field");
                    addField(schemaNode, fieldElement, fieldName, solrType, fieldMapping.getFieldConfig());
                }
            }

            if (!schemaFile.exists() || schemaFile.canWrite()) {
                FileOutputStream out = new FileOutputStream(schemaFile);

                XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat().setIndent("    "));
                xo.output(doc, out);
                out.close();
            } else {
                log.error("schema file {} is not writable", schemaFile);
            }

        } catch (JDOMException e) {
            throw new MarmottaException("parse error while parsing SOLR schema template file " + schemaTemplate, e);
        } catch (IOException e) {
            throw new MarmottaException("I/O error while parsing SOLR schema template file " + schemaTemplate, e);
        }

    }

    private boolean fieldAlreadyExists(Element schemaNode, String fieldType, String fieldName) {
        for (Element child : schemaNode.getChildren()) {
            if (child.getName().equals(fieldType)) {
                if (child.getAttributeValue("name").equals(fieldName)) {
                    return true;
                }
            }
        }
        if (schemaNode.getName() != "fields") {
            Element fieldsNode = schemaNode.getChild("fields");
            if (fieldsNode != null) {
                return fieldAlreadyExists(fieldsNode, fieldType, fieldName);
            }
        }
        return false;

    }

    private void addField(Element schemaNode, Element fieldElement, String fieldName, String solrType,
            Map<String, String> fieldConfig) {
        if (solrType.equals("dynamic")) {
            fieldElement = new Element("dynamicField");
            solrType = "string"; // use string as default
            if (fieldConfig != null) {
                // when
                if (fieldConfig.containsKey("solrType")) {
                    solrType = fieldConfig.get("solrType");
                }
                if (fieldConfig.containsKey("dynamicField")) {
                    fieldName = fieldConfig.get("dynamicField");
                }
            } else {
                // when no name in config - default to string
                fieldName = "*_s"; // default to string ...
            }

        }
        if (fieldAlreadyExists(schemaNode, fieldElement.getName(), fieldName)) {
            // do not output an existing field
            return;
        }

        fieldElement.setAttribute("name", fieldName);
        // when dynamic, we need a valid solrType
        fieldElement.setAttribute("type", solrType);
        // Set the default properties
        fieldElement.setAttribute("stored", "true");
        fieldElement.setAttribute("indexed", "true");
        fieldElement.setAttribute("multiValued", "true");

        // FIXME: Hardcoded Stuff!
        if (solrType.equals("location")) {
            fieldElement.setAttribute("indexed", "true");
            fieldElement.setAttribute("multiValued", "false");
        }

        if (fieldConfig != null) {
            for (Map.Entry<String, String> attr : fieldConfig.entrySet()) {
                if (SOLR_FIELD_OPTIONS.contains(attr.getKey())) {
                    fieldElement.setAttribute(attr.getKey(), attr.getValue());
                }
            }
        }
        Element fieldsNode = schemaNode.getChild("fields");
        if (fieldsNode != null) {
            fieldsNode.addContent(fieldElement);
        } else {
            schemaNode.addContent(fieldElement);
        }

        if (fieldConfig != null && fieldConfig.keySet().contains(SOLR_COPY_FIELD_OPTION)) {
            String[] copyFields = fieldConfig.get(SOLR_COPY_FIELD_OPTION).split("\\s*,\\s*");
            for (String copyField : copyFields) {
                if (copyField.trim().length() > 0) { // ignore 'empty' fields
                    Element copyElement = new Element("copyField");
                    copyElement.setAttribute("source", fieldName);
                    copyElement.setAttribute("dest", copyField.trim());
                    schemaNode.addContent(copyElement);
                }
            }
        } else {
            Element copyElement = new Element("copyField");
            copyElement.setAttribute("source", fieldName);
            copyElement.setAttribute("dest", "lmf.text_all");
            schemaNode.addContent(copyElement);
        }

        // for suggestions, copy all fields to lmf.spellcheck (used for spellcheck and
        // querying);
        // only facet is a supported type at the moment
        if (fieldConfig != null && fieldConfig.keySet().contains(SOLR_SUGGESTION_FIELD_OPTION)) {
            String suggestionType = fieldConfig.get(SOLR_SUGGESTION_FIELD_OPTION);
            if (suggestionType.equals("facet")) {
                Element copyElement = new Element("copyField");
                copyElement.setAttribute("source", fieldName);
                copyElement.setAttribute("dest", "lmf.spellcheck");
                schemaNode.addContent(copyElement);
            } else {
                log.error("suggestionType " + suggestionType + " not supported");
            }
        }

    }

    /**
     * Create/update the solrconfig.xml file for the given core according to the
     * core configuration.
     *
     * @param engine
     *            the solr core configuration
     */
    private void createSolrConfigXml(SolrCoreConfiguration engine) throws MarmottaException {
        File configTemplate = new File(getCoreDirectory(engine.getName()),
                "conf" + File.separator + "solrconfig-template.xml");
        File configFile = new File(getCoreDirectory(engine.getName()), "conf" + File.separator + "solrconfig.xml");

        try {
            SAXBuilder parser = new SAXBuilder(XMLReaders.NONVALIDATING);
            Document solrConfig = parser.build(configTemplate);

            FileOutputStream out = new FileOutputStream(configFile);

            // Configure suggestion service: add fields to suggestion handler
            Program<Value> program = engine.getProgram();
            for (Element handler : solrConfig.getRootElement().getChildren("requestHandler")) {
                if (handler.getAttribute("class").getValue().equals(SuggestionRequestHandler.class.getName())) {
                    for (Element lst : handler.getChildren("lst")) {
                        if (lst.getAttribute("name").getValue().equals("defaults")) {
                            // set suggestion fields
                            for (FieldMapping<?, Value> fieldMapping : program.getFields()) {

                                String fieldName = fieldMapping.getFieldName();
                                final Map<String, String> fieldConfig = fieldMapping.getFieldConfig();

                                if (fieldConfig != null
                                        && fieldConfig.keySet().contains(SOLR_SUGGESTION_FIELD_OPTION)) {
                                    String suggestionType = fieldConfig.get(SOLR_SUGGESTION_FIELD_OPTION);
                                    if (suggestionType.equals("facet")) {
                                        Element field_elem = new Element("str");
                                        field_elem.setAttribute("name", SuggestionRequestParams.SUGGESTION_FIELD);
                                        field_elem.setText(fieldName);
                                        lst.addContent(field_elem);
                                    } else {
                                        log.error("suggestionType " + suggestionType + " not supported");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat().setIndent("    "));
            xo.output(solrConfig, out);
            out.close();
        } catch (JDOMException e) {
            throw new MarmottaException("parse error while parsing SOLR schema template file " + configTemplate, e);
        } catch (IOException e) {
            throw new MarmottaException("I/O error while parsing SOLR schema template file " + configTemplate, e);
        }
    }

    /**
     * Register the core with the name given as argument with the SOLR core admin.
     *
     * @param coreName
     */
    private void registerSolrCore(String coreName) {
        log.info("registering collection {} with Cloud SOLR service", coreName);

        if (searchFilter.getCores().getCore(coreName) == null) {
            // CoreDescriptor x = new Co
            File f = getCoreDirectory(coreName).getAbsoluteFile();
            Path p = f.toPath();
            // CoreDescriptor d = new CoreDescriptor(searchFilter.getCores().getHostName(),
            // coreName, p);
            SolrCore core = searchFilter.getCores().create(coreName, new HashMap<String, String>());
            searchFilter.getCores().reload(coreName);// , false);
        } else {
            log.error("core {} already registered, cannot reregister it", coreName);
        }

    }
    /**
     * Registers 
     * @param config
     * @throws MarmottaException
     */
    private void registerSolrCore(SolrCoreConfiguration config) throws MarmottaException {
        SolrCoreAdministration admin = new SolrCoreAdministration(config.getSolrClient());
        try {
            if (!admin.getCores().contains(config.getName())) {
                File home = getCoreDirectory(config.getName());
                if (! home.exists() ) {
                    ensureCoreDirectory(config.getName(), true);
                }
                // create the core
                admin.create(config.getName(), getCoreDirectory(config.getName()).getAbsolutePath());
                // adopt the schema
                SolrSchema schema = admin.getSchema(config.getName());
                processSchema(schema, config.getProgram());
            } else {
                // reload the core so that the schema mapping is based on actual data
                admin.reload(config.getName());
            }

        } catch (SolrServerException e) {
            throw new MarmottaException(e);
        } catch (IOException e) {
            throw new MarmottaException(e);
        }
    }
    private void processSchema(SolrSchema schema, Program<Value> program) throws SolrServerException {
        for ( FieldMapping<?,Value> map : program.getFields()) {
            if ( ! schema.hasField(map.getFieldName())) {
                SolrField field = new SolrField();
                field.setName(map.getFieldName());
                field.setType(SolrProgramServiceImpl.xsdSolrTypeMap.get(map.getFieldType().toString()));
                field.setProperty("stored", Boolean.TRUE);
                field.setProperty("multiValued", Boolean.TRUE);
                field.setProperty("indexed", Boolean.TRUE);
                if ( map.getFieldConfig()!= null ) {
                    for (String key : map.getFieldConfig().keySet()) {
                        field.setProperty(key, map.getFieldConfig().get(key));
                    }
                }
                if (! schema.hasType(field.getType())) {
                    throw new SolrServerException(String.format("Cannot add field %s, the requested field type [%s] is not present", field.getName(), field.getType()));
                }
                    
                if ( schema.addField(field) ) {
                    if (! schema.hasCopyField("*")) {
                        if ( schema.hasField("lmf.text_all")) {
                            SolrCopyField cf = new SolrCopyField(field.getName(), "lmf.text_all");
                        }
                        else {
                            SolrCopyField cf = new SolrCopyField(field.getName(), "_TEXT_");
                        }
                    }
                }
                System.out.println(String.format("Need to create %s with type %s", map.getFieldName(), map.getFieldType()));
            }               
        }
    }

    private void reloadSolrCore(String coreName) {
        log.info("reloading core {} in embedded SOLR service", coreName);

        searchFilter.getCores().reload(coreName);
    }

    /**
     * Unregister the core with the name given as argument with the SOLR core admin.
     *
     * @param coreName
     */
    private void unregisterSolrCore(String coreName) {
        log.info("unregistering core {} from embedded SOLR service", coreName);
        searchFilter.getCores().unload(coreName, true, true, true);
        // SolrCore core = searchFilter.getCores().unload(coreName, true, true, true);
        // if(core != null)
        // core.close();
    }

}
