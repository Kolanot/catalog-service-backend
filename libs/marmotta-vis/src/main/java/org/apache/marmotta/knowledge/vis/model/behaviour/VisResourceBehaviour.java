package org.apache.marmotta.knowledge.vis.model.behaviour;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.marmotta.knowledge.vis.model.VisResource;
import org.apache.marmotta.knowledge.vis.ns.RDF;
import org.apache.marmotta.knowledge.vis.ns.RDFS;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.object.LangString;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public abstract class VisResourceBehaviour implements VisResource {

    public VisResourceBehaviour() {
    }

    public Set<Locale> getLocales() {
        Set<Locale> lang = new HashSet<>();
        lang.addAll(this.getAvailableLocales());
        return lang;

    }

    @Override
    public boolean isEditable() {
        if (getEditable() == null) {
            return true;
        }
        return getEditable();
    }

    public Set<String> getTypes() {
        Set<String> types = new HashSet<>();
        types.add(getType().toString());
        for (Object o : getRdfTypes()) {
            types.add(o.toString());
        }
        return types;
    }

    /**
     * Retrieve the label in the current language
     */
    @Override
    public String getLabel() {
        return getLabel(getLocale());
    }

    /**
     * Store the provided label in the current language
     * 
     * @see #setLabel(String, Locale)
     */
    @Override
    public void setLabel(String label) {
        setLabel(label, getLocale());

    }

    @Override
    public Locale checkLanguageUsed() {
        for (Object o : getRdfLabels()) {
            if (o instanceof LangString) {
                LangString s = (LangString) o;
                return s.getLocale();
            }
        }
        return null;
    }

    /**
     * Retrieve the label in the requested language. if the requested language is
     * not yet stored, the default language is returned.
     * 
     * @return the label
     */
    @Override
    public String getLabel(Locale locale) {
        String fallBack = null;
        for (Object o : getRdfLabels()) {
            if (o instanceof LangString) {
                LangString s = (LangString) o;
                if (s.getLocale().equals(locale)) {
                    return s.toString();
                } else if (s.getLocale().equals(Locale.getDefault())) {
                    fallBack = s.toString();
                } else {
                    if (fallBack == null) {
                        fallBack = s.toString();
                    }
                }
            }
            if (o instanceof String) {
                fallBack = o.toString();
            }
        }
        return fallBack;
    }

    /**
     * Store a label with the provided locale.
     */
    @Override
    public void setLabel(String label, Locale locale) {
        if (label != null) {
            LangString newLabel = new LangString(label, locale);
            Set<Object> prefLabels = getRdfLabels();
            // get the default language
            for (Object l : prefLabels) {
                if (l instanceof LangString) {
                    LangString ls = (LangString) l;
                    if (ls.getLocale().equals(locale)) {
                        prefLabels.remove(l);
                        // break;
                    }

                } else {
                    // label without language - remove & replace
                    prefLabels.remove(l);
                }
            }
            prefLabels.add(newLabel);
        }
    }

    @Override
    public String getComment() {
        return getComment(getLocale());
    }

    @Override
    public void setComment(String label) {
        setComment(label, getLocale());

    }

    /**
     * Retrieve the label in the requested language. if the requested language is
     * not yet stored, the default language is returned.
     * 
     * @return the label
     */
    @Override
    public String getComment(Locale locale) {
        String fallBack = null;
        for (Object o : getRdfComments()) {
            if (o instanceof LangString) {
                LangString s = (LangString) o;
                if (s.getLocale().equals(locale)) {
                    return s.toString();
                } else if (s.getLocale().equals(Locale.getDefault())) {
                    fallBack = s.toString();
                } else {
                    if (fallBack == null) {
                        fallBack = s.toString();
                    }
                }
            }
            if (o instanceof String) {
                fallBack = o.toString();
            }
        }
        return fallBack;
    }

    /**
     * Store a label with the provided locale.
     */
    @Override
    public void setComment(String comment, Locale locale) {
        if (comment != null) {
            LangString newLabel = new LangString(comment, locale);
            Set<Object> comments = getRdfComments();
            // get the default language
            for (Object l : comments) {
                if (l instanceof LangString) {
                    LangString ls = (LangString) l;
                    if (ls.getLocale().equals(locale)) {
                        comments.remove(l);
                        // break;
                    }

                } else {
                    // label without language - remove & replace
                    comments.remove(l);
                }
            }
            comments.add(newLabel);
        }
    }

    @Override
    public void setDefinition(String label, Locale locale) {
        if (label != null) {
            LangString newLabel = new LangString(label, locale);
            Set<LangString> definitions = getDefinitions();
            // get the default language
            for (LangString l : definitions) {
                if (l.getLocale().equals(locale)) {
                    definitions.remove(l);
                    // break;
                }
            }
            definitions.add(newLabel);
        }
    }

    @Override
    public void setDefinition(String label) {
        // String lang = getObjectConnection().getLanguage();
        Locale locale = getLocale();
        setDefinition(label, locale);

    }

    @Override
    public String getDefinition() {
        // String lang = getObjectConnection().getLanguage();
        Locale locale = getLocale();
        return getDefinition(locale);
    }

    @Override
    public String getDefinition(Locale locale) {
        String fallBack = null;
        for (LangString s : getDefinitions()) {
            if (s.getLocale().equals(locale)) {
                return s.toString();
            } else if (s.getLocale().equals(getLocale())) {
                fallBack = s.toString();
            } else {
                if (fallBack == null) {
                    fallBack = s.toString();
                }
            }
        }
        return fallBack;
    }

    protected Locale getLocale() {
        Locale locale = getLanguage();
        if (locale != null) {
            return Locale.forLanguageTag(locale.getLanguage());
        }
        return Locale.forLanguageTag(Locale.getDefault().getLanguage());
    }

    @Override
    public URI getResourceContext() {
        try {
            RepositoryResult<Statement> types = getObjectConnection().getStatements(getResource(), RDF.type_URI, null);
            if (types.hasNext()) {
                Statement stmt = types.next();
                Resource context = stmt.getContext();
                if (context != null) {
                    return getObjectConnection().getValueFactory().createURI(context.stringValue());
                }
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            return null;
        }
        // make a URI out of it
        return getObjectConnection().getValueFactory().createURI(getResource().stringValue());
    }

    @Override
    public Resource getRdfsRange() {
        ObjectConnection conn = getObjectConnection();
        if (getType() != null) {
            try {
                Resource rangeRes = conn.getValueFactory().createURI(getType());
                RepositoryResult<Statement> range = getObjectConnection().getStatements(rangeRes, RDFS.range_URI, null);
                if ( range.hasNext() ) {
                    Value val =  range.next().getObject();
                    if (val instanceof Resource) {
                        return (Resource) val;
                    }
                }
            } catch (RepositoryException e) {
            }
        }
        return XMLSchema.STRING;
    }

    public String getNTriples() {
        StringBuilder sb = new StringBuilder();
        try {
            MemoryStore store = new MemoryStore();
            Repository sailRepository = new SailRepository(store);
            sailRepository.initialize();
            ObjectRepository objectRepository = new ObjectRepositoryFactory().createRepository(sailRepository);
            ObjectConnection connection = objectRepository.getConnection();
            connection.addObject(this);
            GraphQueryResult result = sailRepository.getConnection()
                    .prepareGraphQuery(QueryLanguage.SPARQL, "CONSTRUCT { ?s ?p ?o. } WHERE { ?s ?p ?o. } ").evaluate();
            while (result.hasNext()) {
                Statement item = result.next();

                sb.append(item.getSubject()).append(" ").append(item.getPredicate()).append(" ")
                        .append(item.getObject()).append(".").append(System.getProperty("line.separator"));
            }
            result.close();
            connection.close();
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (RepositoryConfigException e) {
            e.printStackTrace();
        } catch (MalformedQueryException e) {
            e.printStackTrace();
        } catch (QueryEvaluationException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

}
