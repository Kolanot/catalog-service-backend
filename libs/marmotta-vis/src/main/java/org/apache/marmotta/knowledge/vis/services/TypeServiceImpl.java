package org.apache.marmotta.knowledge.vis.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.marmotta.alibaba.api.ObjectService;
import org.apache.marmotta.commons.sesame.repository.ResourceUtils;
import org.apache.marmotta.commons.vocabulary.XSD;
import org.apache.marmotta.knowledge.vis.api.TypeService;
import org.apache.marmotta.knowledge.vis.dao.BaseItem;
import org.apache.marmotta.knowledge.vis.dao.DisplayTypeEnum;
import org.apache.marmotta.knowledge.vis.dao.EdgeType;
import org.apache.marmotta.knowledge.vis.dao.NetworkType;
import org.apache.marmotta.knowledge.vis.dao.NodeType;
import org.apache.marmotta.knowledge.vis.dao.PropertyType;
import org.apache.marmotta.knowledge.vis.dao.PropertyTypeOption;
import org.apache.marmotta.knowledge.vis.model.VisEdgeType;
import org.apache.marmotta.knowledge.vis.model.VisNetwork;
import org.apache.marmotta.knowledge.vis.model.VisNodeType;
import org.apache.marmotta.knowledge.vis.model.VisPropertyType;
import org.apache.marmotta.knowledge.vis.model.VisPropertyValueType;
import org.apache.marmotta.knowledge.vis.model.VisResource;
import org.apache.marmotta.knowledge.vis.model.owl.ObjectProperty;
import org.apache.marmotta.knowledge.vis.model.owl.SymmetricProperty;
import org.apache.marmotta.knowledge.vis.ns.RDF;
import org.apache.marmotta.knowledge.vis.ns.RDFS;
import org.apache.marmotta.knowledge.vis.ns.VIS;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

@ApplicationScoped
public class TypeServiceImpl implements TypeService {
	@Inject
	private ObjectService objectService;
	@Inject
	private ConfigurationService configurationService;
	/* (non-Javadoc)
	 * @see eu.fp7.eagle.knowledge.vis.api.TypeService#getType(org.openrdf.repository.RepositoryConnection, java.lang.String)
	 */
	@Override
	public Resource getType(RepositoryConnection conn, String resource) throws RepositoryException {
		Resource resourceURI = conn.getValueFactory().createURI(resource);
		return getType(conn, resourceURI);
	}
	@Override
	public Resource getType(RepositoryConnection conn, Resource resource) throws RepositoryException {
		String vType = ResourceUtils.getProperty(conn, resource, VIS.visType);
		if ( vType != null) {
			return conn.getValueFactory().createURI(vType);
		}
		return null;
	}
	private Resource getRange(RepositoryConnection conn, String resource) throws RepositoryException {
	    Resource res = conn.getValueFactory().createURI(resource);
	    String range = ResourceUtils.getProperty(conn, res, RDFS.range);
	    if ( range == null ) {
	        return XSD.String;
	    }
	    return conn.getValueFactory().createURI(range);
	}


	@Override
	public Set<BaseItem> getNetworkTypes(Locale locale) throws RepositoryException {
		try {
			ObjectConnection conn = objectService.getConnection();
			try {
				return getNetworkTypes(conn, locale);
			} finally {
				conn.close();
			}
			
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}

	}
	

	@Override
	public NetworkType getNetworkType(ObjectConnection conn, String uri, Locale locale) throws RepositoryException {
		try {
			
			VisNetwork visProject = getVisResource(conn, VisNetwork.class, uri);
			if ( visProject != null ) {
				return makeNetworkType(conn, visProject, locale);
			} 
		}catch (ClassCastException e) {
		}
		throw new RepositoryException(String.format("Resource with uri <%s> is not available or not of type %s", uri, VisNetwork.class.getName()));
	}

	@Override
	public NodeType getNodeType(ObjectConnection conn, String networkType, String uri, Locale locale) throws RepositoryException {
		try {
			Resource networkTypeUri = conn.getValueFactory().createURI(networkType);
			VisNodeType visNodeType = getVisResource(conn, VisNodeType.class, uri);
			if ( visNodeType != null ) {
				// check whether the network itself is marked as editable
				boolean editable = isEditable(conn, networkTypeUri);
				return makeNodeType(conn, networkTypeUri, visNodeType, locale, editable);
			} 
		}catch (ClassCastException e) {
		}
		throw new RepositoryException(String.format("Resource with uri <%s> is not available or not of type %s", uri, VisNetwork.class.getName()));
	}
	@Override
	public EdgeType getEdgeType(ObjectConnection conn, String uri) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PropertyType getPropertyType(ObjectConnection conn, String uri) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetworkType getNetworkType(String uri, Locale locale) throws RepositoryException {
		try {
			ObjectConnection conn = objectService.getConnection();
			try {
				return getNetworkType(conn, uri, locale);
			} finally {
				conn.close();
			}
		}catch (Exception e) {
			throw new RepositoryException(e);
		}
	}
	@Override
	public NodeType getNodeType(String networkType, String uri, Locale locale) throws RepositoryException {
		try {
			ObjectConnection conn = objectService.getConnection();
			try {
				Resource res = conn.getValueFactory().createURI(networkType);
				VisNodeType visProject = getVisResource(conn, VisNodeType.class, uri);
				if ( visProject != null ) {
					boolean editable = isEditable(conn, res);
					return makeNodeType(conn, res, visProject, locale, editable);
				} 
			} finally {
				conn.close();
			}
		}catch (Exception e) {
			throw new RepositoryException(e);
		}
		throw new RepositoryException(String.format("Resource with uri <%s> is not available or not of type %s", uri, VisNetwork.class.getName()));
	}

	@Override
	public EdgeType getEdgeType(String networkType, String uri, Locale locale) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EdgeType getEdgeType(ObjectConnection conn, NetworkType networkType, NodeType sourceNodeType)
			throws RepositoryException {
		NodeType targetNodeType = networkType.getNetworkNode();
		EdgeType et2 = new EdgeType();
		et2.addSourceNodeType(sourceNodeType);
		et2.addTargetNodeType(targetNodeType);
		et2.setType(networkType.getNodeInNetworkUri());
		// for owl inverse & symmetric
		Object typeProp = conn.getObject(networkType.getNodeInNetworkUri());
		if ( typeProp instanceof ObjectProperty) {
			ObjectProperty oTypeProp = (ObjectProperty)typeProp;
			if ( oTypeProp.getInverseOf()!= null) {
				et2.setInverseOf(oTypeProp.getInverseOf().getResource().stringValue());
			}
		}
		if ( typeProp instanceof SymmetricProperty ) {
			et2.setSymmetric(true);
		}
		return et2;
	}
	@Override
	public PropertyType getPropertyType(String networkType, String nodeType, String uri, Locale locale)
			throws RepositoryException {
		try {
			ObjectConnection conn = objectService.getConnection();
			try {
				
				Resource net = conn.getValueFactory().createURI(networkType);
				Resource node = conn.getValueFactory().createURI(nodeType);
				boolean nodeEdit = isEditable(conn, node, net);
				VisPropertyType type = getVisResource(conn, VisPropertyType.class, uri);
				if ( type != null ) {
					return makePropertyType(conn, networkType, type, locale, nodeEdit);
				} 
			} finally {
				conn.close();
			}
		}catch (Exception e) {
			throw new RepositoryException(e);
		}
		throw new RepositoryException(String.format("Resource with uri <%s> is not available or not of type %s", uri, VisNetwork.class.getName()));
	}

	/**
	 * Helper method to make a configuration
	 * @param visProject
	 * @param locale
	 * @param includeProperties
	 * @return
	 * @throws RepositoryException 
	 */
	private NetworkType makeNetworkType(RepositoryConnection conn, VisNetwork visProject, Locale locale) throws RepositoryException {
		NetworkType p = new NetworkType();
		p.setId(visProject.getResource().stringValue());
		p.setLocale(locale);
		p.setLabel(visProject.getLabel(locale));
		p.setComment(visProject.getComment(locale));
		p.setType(visProject.getType());
		p.setEditable(visProject.isEditable());
		// 
		if ( ! visProject.getToChildren().isEmpty()) {
			for(Object s: visProject.getToChildren()) {
				p.getToChildren().add(s.toString());
			}
		}
//		p.setToChildren(visProject.getToChildren());
		p.setNodeInNetworkUri(visProject.getToNetworkProperty());
		// check for the label & comment properties to use
		if ( visProject.getLabelPropertyType()!=null ) {
			p.setLabelProperty(visProject.getLabelPropertyType().getType());
		}
		if ( visProject.getCommentPropertyType()!=null ) {
			p.setCommentProperty(visProject.getCommentPropertyType().getType());
		}
		// check whether we have a inNetwork property 
		if ( visProject.getToNetworkProperty() != null ) {
			// relation for each node back to it's network
			p.setNodeInNetworkUri(visProject.getToNetworkProperty());
		}
		Set<NodeType> nodeTypes = new HashSet<>();
		for ( VisNodeType type : visProject.getVisTypes() ) {
			if ( type.getType()!= null) {
				NodeType t = makeNodeType(conn, visProject.getResource(), type, locale, p.isEditable());
				nodeTypes.add(t);
			}
		}
		p.setNodeTypes(nodeTypes);
		Set<EdgeType> edgeTypes = new HashSet<>();
		for ( VisEdgeType type : visProject.getVisEdges() ) {
			if ( type.getType() != null ) {
				EdgeType t = makeEdgeType(p, type, locale, p.isEditable());
				edgeTypes.add(t);
			}
		}
		p.setEdgeTypes(edgeTypes);
		// 
		Map<String, PropertyType> allPT = new HashMap<String,PropertyType>();
		p.setPropertyTypes(p.getNetworkNode().getPropertyTypes());
		for (NodeType nt : p.getNodeTypes() ) {
			allPT.putAll(nt.getPropertyTypes());
		}
		p.setPropertyTypes(allPT);
		PropertyType labelProperty = p.getPropertyType(p.getLabelProperty(), configurationService.getStringConfiguration("vis.label.default", RDFS.label));
		if ( ! p.getPropertyTypes().containsValue(labelProperty)) {
			p.addPropertyType(labelProperty);
		}
		PropertyType commentProperty = p.getPropertyType(p.getCommentProperty(), configurationService.getStringConfiguration("vis.comment.default", RDFS.comment));
		if ( ! p.getPropertyTypes().containsValue(commentProperty)) {
			p.addPropertyType(commentProperty);
		}
		return p;
		
	}

	private NodeType makeNodeType(RepositoryConnection conn, Resource config, VisNodeType type, Locale locale, boolean parentEditable) throws RepositoryException {
		NodeType t = new NodeType();
		t.setNetworkType(config.stringValue());
		
		t.setId(type.getResource().stringValue());
		t.setLocale(locale);
		
		t.setEditable(parentEditable ? type.isEditable() : false);
		// use the skos properties
		t.setLabel(type.getLabel(locale));
		t.setComment(type.getComment(locale));
		t.setColor(type.getColor());
		t.setImage(type.getImage());
		// 
		t.setType(type.getType());
		t.setTypes(type.getTypes());
		// add the distinct properties
		for ( VisPropertyType pt : type.getPropertyTypes()) {
			//
			PropertyType prop =  makePropertyType(conn, config.stringValue(), pt, locale, t.isEditable());
			t.addPropertyType(prop);
		}
		// check for the label property of the node type
		Resource labelRes = getLabelProperty(type.getObjectConnection(), type.getResource(), config);
		if (labelRes != null) {
			t.setLabelProperty(labelRes.stringValue());
		}
		Resource commentRes = getCommentProperty(type.getObjectConnection(), type.getResource(), config);
		if (commentRes != null) {
			t.setCommentProperty(commentRes.stringValue());
		}
		return t;
		
	}
	public boolean isEditable(RepositoryConnection conn, Resource ... resources) throws RepositoryException {
		if ( resources != null ) {
			List<Resource> fallback = new ArrayList<>();
			fallback.addAll(Arrays.asList(resources));
			if ( fallback.size() > 0 ) {
				Resource parent = fallback.remove(0);
				return isEditable(conn, parent, fallback);
			}
		}
		return false;
	}
	private boolean isEditable(RepositoryConnection conn, Resource instance, List<Resource> fallback) throws RepositoryException {
		String editable = ResourceUtils.getProperty(conn, instance, VIS.editable);
		if ( editable != null ) {
			return Boolean.valueOf(editable);
		}
		if ( fallback.size() > 0 ) {
			Resource parent = fallback.remove(0);
			return isEditable(conn, parent, fallback);
		}
		return false;
	}
	private Resource getResource(RepositoryConnection conn, Resource typeUri, String prop) throws RepositoryException {
		String fallback = null;
		for ( Value v : ResourceUtils.listOutgoingNodes(conn, typeUri, prop) ) {
			if ( v instanceof Resource) {
				return (Resource) v;
			}
			else {
				fallback = v.stringValue();
			}
		}
		if (fallback != null) {
			return conn.getValueFactory().createURI(fallback);
		}
		return null;
	}
	private Resource getPropertyTypeAsString(RepositoryConnection conn, String property, Resource rdfFallback, Resource ...type ) throws RepositoryException {
		for (Resource t : type) {
			Resource labelPropertyType = getResource(conn, t, property);
			if ( labelPropertyType != null ) {
				if ( ResourceUtils.hasType(conn, labelPropertyType, VIS.PropertyType)) {
					Resource res = getResource(conn, labelPropertyType, VIS.visType);
					if ( res != null) {
						return res;//conn.getValueFactory().createURI(res);
					}
				}
			}
		}
		return rdfFallback;
	}

	private EdgeType makeEdgeType(NetworkType config, VisEdgeType type, Locale locale, boolean parentEditable) throws RepositoryException {
		EdgeType t = new EdgeType();
		t.setLocale(locale);
		t.setId(type.getResource().stringValue());
		t.setEditable(parentEditable ? type.isEditable() : parentEditable);
		// use the skos properties
		t.setLabel(type.getLabel(locale));
		t.setComment(type.getComment(locale));
		t.setColor(type.getColor());
		t.setNetworkType(config.getId());		
		t.setType(type.getType());
		t.setSymmetric(false);
		if ( parentEditable) {
			for ( VisNodeType vt : type.getSourceTypes()) {
				NodeType nt = config.getNodeTypeById(vt.getResource().stringValue());
				t.addSourceNodeType(nt);
			}
			for ( VisNodeType vt : type.getTargetTypes()) {
				NodeType nt = config.getNodeTypeById(vt.getResource().stringValue());
				t.addTargetNodeType(nt);
			}
		
			Object typeProp = type.getObjectConnection().getObject(type.getType());
			if ( typeProp instanceof ObjectProperty) {
				ObjectProperty oTypeProp = (ObjectProperty)typeProp;
				if ( oTypeProp.getInverseOf()!= null) {
					t.setInverseOf(oTypeProp.getInverseOf().getResource().stringValue());
				}
			}
			if ( typeProp instanceof SymmetricProperty ) {
				t.setSymmetric(true);
			}
			
		}
	
		
		return t;

	}
	
	private PropertyType makePropertyType(RepositoryConnection conn, String networkType, VisPropertyType type, Locale locale, boolean parentEditable) {
		PropertyType prop = new PropertyType();
		prop.setId(type.getResource().stringValue());
		prop.setLabel(type.getLabel(locale));
		prop.setComment(type.getComment(locale));
		prop.setEditable(parentEditable ? type.getEditable() : parentEditable);
		prop.setLocale(locale);
		prop.setNetworkType(networkType);
		// 
		prop.setType(type.getType());
		Resource range = type.getRdfsRange();
		try {
			DisplayTypeEnum dataType = DisplayTypeEnum.valueOf(type.getDataType());
			
			prop.setDisplayType(dataType);
			switch(dataType){
			case DATE:
				prop.initDataType(XSD.Date);
				break;
			case DATETIME:
				prop.initDataType(XSD.DateTime);
				break;
			case INTEGER:
				prop.initDataType(XSD.Int);
				break;
			case NUMBER:
				prop.initDataType(XSD.Double);
				break;
			case BOOLEAN:
				prop.initDataType(XSD.Boolean);
				break;
			case LANGUAGE:
				prop.initDataType(XSD.Language);
				break;
			case SELECT:
			case URI:
				prop.initDataType(XSD.AnyURI);
				break;
			case STRING:
			    prop.initDataType(XSD.String);
			default:
				prop.initDataType(XSD.String);

			}
		} catch (Exception e) {
		    prop.initDataType(XSD.String);
			prop.setDisplayType(DisplayTypeEnum.STRING);
		}
		prop.setMultiLingual(type.getMultiLingual());
		prop.setMultiValue(type.getMultiValue());
		prop.setRequired(type.getRequired());
//		prop.setDataType(dataType);
		//
		if ( type.getPropertyValues()!= null) {
			Set<PropertyTypeOption> values = new HashSet<>();
			for ( VisPropertyValueType valueType : type.getPropertyValues()) {
				PropertyTypeOption value = new PropertyTypeOption();
				value.setKey(valueType.getIdentifier());
				value.setLabel(valueType.getLabel());
				values.add(value);
			}
			// 
			prop.setOptions(values);
		}
		return prop;
	}
	private <T extends VisResource> T getVisResource(ObjectConnection conn, Class<T> clazz, String uri) throws RepositoryException {
		Resource res = conn.getValueFactory().createURI(uri);
		return getVisResource(conn, clazz, res);
	}
	private <T extends VisResource> T getVisResource(ObjectConnection conn, Class<T> clazz, Resource uri) throws RepositoryException {
		try {
			return conn.getObject(clazz, uri);
		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
	}
	/**
	 * Helper class to retrieve the list of network structures to use as a configuration 
	 * for visualized structures.
	 * @param conn
	 * @param locale
	 * @return
	 * @throws RepositoryException
	 */
	@Override
	public Set<BaseItem> getNetworkTypes(RepositoryConnection conn, Locale locale ) throws RepositoryException {
		try {
			TupleQuery tQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, 
				"SELECT DISTINCT ?uri ?title ?comment ?editable  "
						+ "WHERE "
						+ "{ "
						+ "  { GRAPH ?graph "
						+ "    {"
						+ "      ?uri ?rdfType ?networkType . "
						+ "      OPTIONAL { "
						+ "        ?uri ?isEditable ?editable . "
						+ "      } "
						+ "      OPTIONAL {"
						+ "        ?uri ?rdfsLabel ?title  . "
						+ "        FILTER(lang(?title)='"+locale.getLanguage()+"') . "
						+ "        OPTIONAL { "
						+ "          ?uri ?rdfsComment ?comment  . "
						+ "          FILTER(lang(?comment)='"+locale.getLanguage()+"') . "
						+ "        }"
						+ "      } "
						+ "    } "
						+ "  } "
						+ "} ");
			//		tQuery.setBinding("linkToNetwork", linkToNetwork_URI);
			tQuery.setBinding("networkType", VIS.Network_URI);
			tQuery.setBinding("isEditable", VIS.editable_URI);
			
			tQuery.setBinding("rdfType", RDF.type_URI);
			tQuery.setBinding("rdfsLabel", RDFS.label_URI);
			tQuery.setBinding("rdfsComment", RDFS.comment_URI);
			// filter for provided instance
			// 
			Set<BaseItem> networkTypes = new HashSet<>();
			TupleQueryResult result = tQuery.evaluate();
			while ( result.hasNext()) {
				BindingSet bs = result.next();
				BaseItem g = new BaseItem();
				if ( bs.hasBinding("uri")) {
					g.setId(bs.getValue("uri").stringValue());
				}
				if ( bs.hasBinding("title")){
					g.setLabel(bs.getValue("title").stringValue());
				}
				if ( bs.hasBinding("comment")){
					g.setComment(bs.getValue("comment").stringValue());
				}
				if ( bs.hasBinding("editable")) {
					Boolean b = new Boolean(bs.getValue("editable").stringValue());
					g.setEditable(b);
				}
				g.setType(g.getId());
				g.setLocale(locale);
				networkTypes.add(g);
			}
			return networkTypes;

		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		}
	}
	@Override
	public Resource getLabelProperty(RepositoryConnection conn, Resource... resources) throws RepositoryException {
		 String defaultLabel = configurationService.getStringConfiguration("vis.label.default", RDFS.label);
		 Resource defaultLabelRes = conn.getValueFactory().createURI(defaultLabel);
		 return getPropertyTypeAsString(conn, VIS.labelProperty, defaultLabelRes, resources);
	}

	@Override
	public Resource getCommentProperty(RepositoryConnection conn, Resource... resources) throws RepositoryException {
		 String defaultLabel = configurationService.getStringConfiguration("vis.comment.default", RDFS.comment);		
		 Resource defaultLabelRes = conn.getValueFactory().createURI(defaultLabel);
		 return getPropertyTypeAsString(conn, VIS.descriptionProperty, defaultLabelRes, resources);
	}
	@Override
	public Resource getInNetworkProperty(RepositoryConnection conn, Resource networkType) throws RepositoryException {
		if( ResourceUtils.hasType(conn, networkType, VIS.Network_URI)) {
			String res = ResourceUtils.getProperty(conn, networkType, VIS.inNetwork);
			if ( res != null ) {
				return conn.getValueFactory().createURI(res);
			}
		}
		throw new RepositoryException(String.format("Resource <%s> is not of type <%s>", networkType.stringValue(), VIS.Network));
	}
}

