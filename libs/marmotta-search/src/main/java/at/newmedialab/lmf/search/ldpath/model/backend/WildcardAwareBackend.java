package at.newmedialab.lmf.search.ldpath.model.backend;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.marmotta.ldpath.backend.sesame.ContextAwareSesameConnectionBackend;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import at.newmedialab.lmf.search.ldpath.model.DynKiwiLiteral;

public class WildcardAwareBackend extends ContextAwareSesameConnectionBackend {
	public static final String SEPARATOR = "@@";
	private final RepositoryConnection connection;
	private final boolean includeInferred;
	private final Resource[] contexts;

	public WildcardAwareBackend(RepositoryConnection connection, Resource... contexts) {
		super(connection, contexts);
		this.connection = connection;
		this.includeInferred = true;
		this.contexts = contexts;
	}

	public WildcardAwareBackend(RepositoryConnection connection, boolean includeInferred, Resource... contexts) {
		super(connection, includeInferred, contexts);
		this.connection = connection;
		this.includeInferred = includeInferred;
		this.contexts = contexts;
	}

	@Override
	public Collection<Value> listObjects(Value subject, Value property) {
        try {
            if ( property instanceof Literal ) {
            	if ( property.stringValue().endsWith("*") || property.stringValue().endsWith("%")) {
            		return listObjectsWildcard(connection, (Resource) subject, property.stringValue(), includeInferred, contexts);
            	}
            }
            return super.listObjects(subject, property);
        } catch (RepositoryException e) {
            throw new RuntimeException(
                    "error while querying Sesame repository!", e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(String.format(
                    "Subject needs to be a URI or blank node, property a URI node "
                            + "(types: [subject: %s, property: %s])",
                    debugType(subject), debugType(property)), e);
        }
		
	}
    protected Collection<Value> listObjectsWildcard(RepositoryConnection connection, Resource subject, String property, boolean includeInferred, Resource... contexts)
            throws RepositoryException {
        final ValueFactory valueFactory = connection.getValueFactory();

        Set<Value> result = new HashSet<Value>();
        // retrieve all statements for the given resource
        RepositoryResult<Statement> qResult = connection.getStatements(merge(subject, valueFactory), null, null, includeInferred, contexts);
        try {
        	
            while(qResult.hasNext()) {
            	Statement stmt = qResult.next();
            	// TODO: Improve selection! (possibly use regex)
            	String prop = property.substring(0, property.length()-1);
            	// 
            	if ( stmt.getPredicate().stringValue().startsWith(prop)) {
            		// keep both property and value separated by colon (
            		
            		result.add(createDynLiteral(stmt.getPredicate().getLocalName(), stmt.getObject().stringValue()));
//            		String propValue = String.format("%s"+SEPARATOR+"%s", stmt.getPredicate().stringValue(), stmt.getObject().stringValue());
//            		result.add(connection.getValueFactory().createLiteral(propValue));            	
            	}
            }
        } finally {
            qResult.close();
        }
        return  result;
    }
    protected Collection<Value> listSubjectsInternal(final RepositoryConnection connection, org.openrdf.model.URI property, Value object, boolean includeInferred, Resource... contexts)
            throws RepositoryException {
        final ValueFactory valueFactory = connection.getValueFactory();

       
        Set<Value> result = new HashSet<>();
        RepositoryResult<Statement> qResult = connection.getStatements(null, merge((org.openrdf.model.URI) object, valueFactory), merge(property, valueFactory), includeInferred, contexts);
        try {
            while(qResult.hasNext()) {
                result.add(qResult.next().getSubject());
            }
        } finally {
            qResult.close();
        }
        return  result;
    }

    public Value createDynLiteral(String name, String value) {
    	return new DynKiwiLiteral(name, value);
    }
    public Collection<Value> createDynLiteralCollection(String name, String value) {
    	HashSet<Value> vl = new HashSet<>();
    	Value val = new DynKiwiLiteral(name, value);
    	vl.add(val);
    	return vl;
    }
    public Collection<Value> createDynLiteralCollection(String name, Collection<String> values) {
    	HashSet<Value> vl = new HashSet<>();
    	for ( String value : values ) {
    		Value val = new DynKiwiLiteral(name, value);
    		vl.add(val);
    	}
    	return vl;
    	
    }
}
