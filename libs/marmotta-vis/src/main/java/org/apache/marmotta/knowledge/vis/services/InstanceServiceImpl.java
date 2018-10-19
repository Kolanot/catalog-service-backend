package org.apache.marmotta.knowledge.vis.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.apache.marmotta.alibaba.api.ObjectService;
import org.apache.marmotta.commons.sesame.repository.ResourceUtils;
import org.apache.marmotta.commons.vocabulary.XSD;
import org.apache.marmotta.knowledge.vis.api.InstanceService;
import org.apache.marmotta.knowledge.vis.api.TypeService;
import org.apache.marmotta.knowledge.vis.dao.Base;
import org.apache.marmotta.knowledge.vis.dao.BaseItem;
import org.apache.marmotta.knowledge.vis.dao.Edge;
import org.apache.marmotta.knowledge.vis.dao.EdgeItem;
import org.apache.marmotta.knowledge.vis.dao.EdgeType;
import org.apache.marmotta.knowledge.vis.dao.Graph;
import org.apache.marmotta.knowledge.vis.dao.GraphItem;
import org.apache.marmotta.knowledge.vis.dao.GraphWithData;
import org.apache.marmotta.knowledge.vis.dao.InstanceWithData;
import org.apache.marmotta.knowledge.vis.dao.InstanceWithProperties;
import org.apache.marmotta.knowledge.vis.dao.NetworkItem;
import org.apache.marmotta.knowledge.vis.dao.NetworkType;
import org.apache.marmotta.knowledge.vis.dao.NodeItem;
import org.apache.marmotta.knowledge.vis.dao.NodeType;
import org.apache.marmotta.knowledge.vis.dao.Property;
import org.apache.marmotta.knowledge.vis.dao.PropertyType;
import org.apache.marmotta.knowledge.vis.dao.PropertyValue;
import org.apache.marmotta.knowledge.vis.ns.DCTERMS;
import org.apache.marmotta.knowledge.vis.ns.RDF;
import org.apache.marmotta.knowledge.vis.ns.RDFS;
import org.apache.marmotta.knowledge.vis.ns.VIS;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;


@ApplicationScoped
public class InstanceServiceImpl implements InstanceService {
	@Inject
	private TypeService visType;
	@Inject
	private ObjectService objectService;
	@Inject
	private ConfigurationService configurationService;

	@Override
	public Set<GraphItem> getGraphs(String networkType, Locale locale) {
		try {
			ObjectConnection conn = objectService.getConnection();
			try {
				//conn.begin();
				NetworkType project = visType.getNetworkType(conn, networkType, locale);
				if ( project != null ) {
//					Resource targetType_URI = createUri(project.getType());
					Set<GraphItem> graphs = getGraphs(conn, project, locale);
					return graphs;
										
				}
				return new HashSet<>();
			} finally {
				conn.close();
			}
			
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public Set<NetworkItem> getNetworks(String networkType, String graph, Locale locale, boolean countAll) throws RepositoryException {
		try {
			ObjectConnection conn = objectService.getConnection();
			try {
				//conn.begin();
				NetworkType project = visType.getNetworkType(conn, networkType, locale);
				if ( project != null ) {
//					Resource targetType_URI = createUri(project.getType());
					Graph g = null;
					if ( graph!=null) {
						Resource graphRes = conn.getValueFactory().createURI(graph);
						g = getGraph(conn, graphRes, locale);
					}
					Set<NetworkItem> graphs = getNetworks(conn, project, g, locale, countAll);
					return graphs;
										
				}
				return new HashSet<>();
			} finally {
				conn.close();
			}
			
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public Set<NodeItem> getNodes(RepositoryConnection conn, NetworkType networkType, Base graph, InstanceWithProperties instance, Locale locale, boolean complete) throws RepositoryException {
		// apply a filter for the given node
		Resource networkType_URI = conn.getValueFactory().createURI(networkType.getType());
		Resource graph_URI = conn.getValueFactory().createURI(graph.getId());
		Resource networkURI = null;
		Resource nodeURI = null;
		Resource inNetworkURI = null;
		boolean includeNetwork = true;
		String instanceFilter = "";
		String networkFilter = "";
		String freeNodeFilter = "";
		String instance2Filter = "";
		String networkBinding = "?network";
		// 
		
		if ( networkType.getNodeInNetworkUri() != null ) {
			inNetworkURI = conn.getValueFactory().createURI(networkType.getNodeInNetworkUri());
			networkFilter = "OPTIONAL { ?uri ?inNetwork ?network  } . ";
		}
	
		/*
		 * cases:
		 * 1) no network, no instance set (complete) -> search for all node & edge types, (incomplete) -> search for free nodes, network nodes and connected nodes
		 * 2) network set, no instance
		 * 3) network & instance set
		 */
		if ( instance != null ) {
			// do we check the nodes for a network?
			if ( instance.getInstanceType().equals(networkType.getNetworkNode())) {
				networkURI = conn.getValueFactory().createURI(instance.getId());
				networkBinding = "";
				Set<String> propertyList = (complete ? networkType.getEdgeTargets() : networkType.getEdgeTargets(networkType.getType()));
				instanceFilter = 
						" ?uri ?property ?outgoing .  "+ 
						" FILTER( ?property IN (" + StringUtils.join(propertyList, ",") + ")) . ";
				if ( networkType.getNodeInNetworkUri()!=null) {
					// remove optional from network filter...
					if ( complete ) {
						instanceFilter = 
								" OPTIONAL { "
								+"    ?uri ?property ?outgoing .  " 
								+ "   FILTER( ?property IN (" + StringUtils.join(propertyList, ",") + ")). "
								+ "} . ";
					}
					else {
						instanceFilter = "    ?uri ?property ?outgoing .  " 
								+ "   FILTER( ?property IN (" + StringUtils.join(propertyList, ",") + ")). ";

					}
					networkFilter =
							" ?uri ?inNetwork ?network .  ";
				}
			}
			// we are checking the nodes for an existing instance
			else {
				nodeURI = conn.getValueFactory().createURI(instance.getId());
				Set<String> propertyList = (complete ? networkType.getEdgeTargets() : networkType.getEdgeTargets(instance.getInstanceType().getType()));
				instanceFilter = 
						" ?instance ?property ?uri .  "+ 
						" FILTER( ?property IN (" + StringUtils.join(propertyList, ",") + ")) . ";
				instance2Filter = 
						" FILTER (?outgoing != ?instance) . ";
			}
		}
		if(! complete) {
			//
			String notExistsTemplate = "  FILTER NOT EXISTS { %s %s %s } . ";
			
			StringBuilder notExistsFilter = new StringBuilder();
			for ( String notExistProperty : networkType.getToChildrenFormatted() ) {
				notExistsFilter.append(String.format(notExistsTemplate, "?sub", notExistProperty, "?uri" ));
			}
			freeNodeFilter = 
					  "UNION { GRAPH ?graph "
					+ "    {"
					+ "      ?uri ?rdfType ?type . "
					+ "      FILTER (?type IN (" + StringUtils.join(networkType.getNodeTargetsFormatted(false), ",") + ")) ."
					+ notExistsFilter.toString()
					+ networkFilter
					+ "      OPTIONAL {"
					+ "        ?uri "+ networkType.getLabelUriFormatted()+" ?title  . "
					+ "        FILTER(lang(?title)='"+locale.getLanguage()+"') . "
					+ "        OPTIONAL { "
					+ "          ?uri "+ networkType.getCommentUriFormatted() +" ?comment  . "
					+ "          FILTER(lang(?comment)='"+locale.getLanguage()+"') . "
					+ "        }"
					+ "      } "
					+ "      OPTIONAL {"
					+ "        ?uri ?edgeType ?outgoing . "
					+ "	       FILTER( ?edgeType IN ("+ StringUtils.join(networkType.getEdgeTargets(), ",") + ")) . "
					+ "      }"
					+ "    } "
					+ "} ";
			
			Set<String> propertyList = (complete ? networkType.getEdgeTargets() : networkType.getEdgeTargets(networkType.getType()));
			instanceFilter = 
					" ?uri ?property ?outgoing .  "+ 
					" FILTER( ?property IN (" + StringUtils.join(propertyList, ",") + ")) . ";
		
			
		}
		try {
			// filtering for types will exclude the network-node when provided instance is a network!
			
			TupleQuery tQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, 
						"SELECT DISTINCT ?uri "+networkBinding+" ?title ?comment ?type (COUNT(DISTINCT ?outgoing) as ?count) "
						+ "WHERE "
						+ "{ "
						+ "  { GRAPH ?graph "
						+ "    {"
						+ "      ?uri ?rdfType ?type . "
						+ "      FILTER (?type IN (" + StringUtils.join(networkType.getNodeTargetsFormatted(includeNetwork), ",") + ")) ."	// use the proper types
						+ instanceFilter 
						+ networkFilter
						+ "      OPTIONAL {"
						+ "        ?uri "+ networkType.getLabelUriFormatted()+" ?title  . "
						+ "        FILTER(lang(?title)='"+locale.getLanguage()+"') . "
						+ "        OPTIONAL { "
						+ "          ?uri "+ networkType.getCommentUriFormatted() +" ?comment  . "
						+ "          FILTER(lang(?comment)='"+locale.getLanguage()+"') . "
						+ "        }"
						+ "      } "
						+ "      OPTIONAL {"
						+ "        ?uri ?edgeType ?outgoing . "
						+ "	       FILTER( ?edgeType IN ("+ StringUtils.join(networkType.getEdgeTargets(), ",") + ")) . "
						+          instance2Filter
						+ "      }"
						+ "    } "
						+ "  } "
						+ freeNodeFilter
						+ "} GROUP BY ?uri "+networkBinding+" ?title ?comment ?type ");
			//		tQuery.setBinding("linkToNetwork", linkToNetwork_URI);
			tQuery.setBinding("graph", graph_URI);
			tQuery.setBinding("networkType", networkType_URI);
			tQuery.setBinding("rdfType", RDF.type_URI);
			// filter for provided instance
			if ( nodeURI != null ) {
				tQuery.setBinding("instance", nodeURI);
			}
			if ( inNetworkURI !=null) {
				tQuery.setBinding("inNetwork", inNetworkURI);
			}
			if ( networkURI != null) {
				tQuery.setBinding("network", networkURI);
			}
			// 
			Set<NodeItem> graphs = new HashSet<>();
			TupleQueryResult result = tQuery.evaluate();
			while ( result.hasNext()) {
				BindingSet bs = result.next();
				NodeItem g = new NodeItem();
				if ( networkURI!=null) {
					g.setNetwork(networkURI.stringValue());
				}
				g.setNamedGraph(graph.getId());
				if ( bs.hasBinding("uri")) {
					g.setId(bs.getValue("uri").stringValue());
					if ( bs.hasBinding("network")) {
						g.setNetwork(bs.getValue("network").stringValue());
					}
					if ( bs.hasBinding("title")){
						g.setLabel(bs.getValue("title").stringValue());
					}
					if ( bs.hasBinding("comment")){
						g.setComment(bs.getValue("comment").stringValue());
					}
					if ( bs.hasBinding("type")){
						NodeType nt = networkType.getNodeType(bs.getValue("type").stringValue());
						// use the nodeType's Id
						g.setType(nt.getId());
					}
					if ( bs.hasBinding("count")) {
						Integer v = new Integer(bs.getValue("count").stringValue());
						g.setCount(v);
					}
					if ( bs.hasBinding("graph")) {
						//
						g.setNamedGraph(bs.getValue("graph").stringValue());
					}
					else {
						if ( graph_URI!=null) {
							g.setNamedGraph(graph_URI.stringValue());
						}
					}
					g.setLocale(locale);
					g.setEditable(graph.isEditable());
					graphs.add(g);
				}
			}
			return graphs;
	
		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		}
	
	}
	@Override
		public Set<EdgeItem> getEdges(RepositoryConnection conn, NetworkType networkType, Base graph, InstanceWithProperties instance, Locale locale, boolean complete) throws RepositoryException {
			try {
				// the graph must be given
				Resource graph_URI = conn.getValueFactory().createURI(graph.getId());
				Resource inNetworkURI = null;
				Resource networkURI = null;
				Resource nodeURI = null;
				String instanceFilter = "";
				String networkFilter = "";
				Set<String> propertyList = (complete ? networkType.getEdgeTargets() : networkType.getEdgeTargets(networkType.getType()));
				instanceFilter = 
						" ?uri ?property ?target .  "+ 
						" FILTER( ?property IN (" + StringUtils.join(propertyList, ",") + ")) . ";
				if ( instance != null ) {
					if ( instance.getInstanceType().equals(networkType.getNetworkNode())) {
						if ( networkType.getNodeInNetworkUri()!=null) {
							networkURI = conn.getValueFactory().createURI(instance.getId());
							inNetworkURI = conn.getValueFactory().createURI(networkType.getNodeInNetworkUri());
							networkFilter =
									" ?uri ?inNetwork ?network .  ";
						}
					}
					else {
						nodeURI = conn.getValueFactory().createURI(instance.getId());
						propertyList = (complete ? networkType.getEdgeTargets() : networkType.getEdgeTargets(instance.getInstanceType().getType()));
						instanceFilter = 
								" ?uri ?property ?target .  " + 
								" FILTER( ?property IN (" + StringUtils.join(propertyList, ",") + ")) . ";
					}
				}
				else {
					
				}
				
				TupleQuery tQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, 
						"SELECT DISTINCT ?uri ?property ?target "
								+ "WHERE  { "
								+ "  { GRAPH ?graph "
								+ "     {"
								+ instanceFilter
								+ networkFilter
								+ "        ?uri ?rdfType ?type . "
								+ "        FILTER (?type IN (" + StringUtils.join(networkType.getNodeTargetsFormatted(), ",") + ")) ."	// use the proper types from project types
	//							+ "        ?uri ?property ?target . "
	//							+ "        FILTER (?property IN (" + StringUtils.join(networkType.getEdgeTargets(), ",") + ")) ."	// use the proper edge types from project types
								+ "     }"
								+ "  }"
								+ "}");
				tQuery.setBinding("graph", graph_URI);
				tQuery.setBinding("rdfType", RDF.type_URI);
				if ( inNetworkURI !=null ) {
					tQuery.setBinding("inNetwork", inNetworkURI);
					tQuery.setBinding("network", networkURI);
				}
				if ( nodeURI != null ) {
					tQuery.setBinding("uri", nodeURI);
				}
				//
				Set<EdgeItem> edges = new HashSet<>();
				TupleQueryResult result = tQuery.evaluate();
				while ( result.hasNext()) {
					BindingSet bs = result.next();
					EdgeItem g = new EdgeItem();
					g.setNamedGraph(graph.getId());
	//				g.setId(UUID.randomUUID().toString());
					if ( bs.hasBinding("uri")) {
						g.setFrom(bs.getValue("uri").stringValue());
						g.setLabel(bs.getValue("uri").stringValue());
					}
					if ( bs.hasBinding("target")){
						g.setTo(bs.getValue("target").stringValue());
					}
					if ( bs.hasBinding("property")) {
						EdgeType edgeType = networkType.getEdgeType(bs.getValue("property").stringValue());
						g.setType(edgeType.getId());
						g.setLabel(edgeType.getLabel());
					}
					// 
					g.setLocale(locale);
					int hash = String.format("%s:%s%s", g.getFrom(), g.getType(), g.getTo()).hashCode();
					g.setId(""+hash);
					edges.add(g);
				}
				return edges;
				
			} catch (MalformedQueryException e) {
				throw new RepositoryException(e);
			} catch (QueryEvaluationException e) {
				throw new RepositoryException(e);
			}
	
		}
	@Override
	public InstanceWithData getGraphData(@NotNull String networkType, @NotNull String graph, Locale locale, boolean countAll) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			//conn.begin();
			NetworkType project = visType.getNetworkType(conn, networkType, locale);
			if ( project != null ) {
				Resource graphUri = conn.getValueFactory().createURI(graph);
				Graph g = getGraph(conn, graphUri, locale);
				return getGraphData(conn, project, g, locale, countAll);
									
			}
			return null;
		} finally {
			conn.close();
		}
		
	}
	@Override
	public InstanceWithData getNetwork(String networkType, String network, Locale locale, boolean complete) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			return getNetwork(conn, networkType, network, locale, complete);
		} finally {
			conn.close();
		}
	}
	@Override
	public Graph getGraph(String graph, Locale locale) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			Resource graphRes = conn.getValueFactory().createURI(graph);
			return getGraph(conn, graphRes, locale);
		} finally {
			conn.close();
		}
	}
	private Resource setGraph(RepositoryConnection conn, Graph graph, Locale locale) throws RepositoryException {
        if ( graph.getId() == null ) {
            String uri = configurationService.getBaseContext() 
                    + graph.getClass().getSimpleName().toLowerCase() +"-" 
                    + UUID.randomUUID().toString();
            graph.setId(uri);
        }
        URI context = conn.getValueFactory().createURI(graph.getId());
        if (!ResourceUtils.hasType(conn, context, VIS.Graph)) {
            ResourceUtils.addType(conn, context, VIS.Graph_URI, context);
        }
        // store the editable setting
        ResourceUtils.removeProperty(conn, context, VIS.editable, context);
        Literal editable = conn.getValueFactory().createLiteral(graph.isEditable());
        Statement edit = conn.getValueFactory().createStatement(context, VIS.editable_URI, editable, context);
        conn.add(edit);
        // provide the language settings
        ResourceUtils.setProperty(conn, context, VIS.language, locale.stripExtensions().getLanguage(), context);
        if ( graph.getLabel() != null ) {
            ResourceUtils.removeProperty(conn, context, RDFS.label, context);
            ResourceUtils.setProperty(conn, context, RDFS.label, graph.getLabel(), locale, context);
        }
        if ( graph.getComment() != null ) {
            ResourceUtils.removeProperty(conn, context, RDFS.comment, context);
            ResourceUtils.setProperty(conn, context, RDFS.comment, graph.getComment(), locale, context);
        }
        // be sure to have 
        if ( locale != null && !graph.getLanguages().contains(locale)) {
            graph.getLanguages().add(locale);
        }
        if (graph.getLanguages()!= null && graph.getLanguages().size() > 0) {
            ResourceUtils.removeProperty(conn, context, VIS.hasLanguage, context);
            
            for(Locale l : graph.getLanguages()) {
                ResourceUtils.setProperty(conn,  context,  VIS.hasLanguage, l.stripExtensions().getLanguage());
//                Literal lit = conn.getValueFactory().createLiteral(l.stripExtensions().getLanguage());
//                Statement stmt = conn.getValueFactory().createStatement(context, VIS.hasLanguage_URI, lit, context);
//                conn.add(stmt);
            }
        }
        if (graph.getType() != null) {
            boolean typePresent = false;
            for (String type : ResourceUtils.getProperties(conn,  context,  VIS.hasType)) {
                if ( type.equals(graph.getType())) {
                    typePresent = true;
                }
            };
            if (!typePresent) {
                ResourceUtils.setProperty(conn, context, VIS.hasType, graph.getType(), context);
            }
        }
        return context;
	}
	@Override
	public Graph setGraph(Graph graph, Locale locale) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			conn.begin();
			// be sure to have a new id
			Resource context = setGraph(conn, graph, locale);
			conn.commit();
			if ( graph.getId()!=null) {
				return getGraph(conn, context, locale);
			}	
			throw new RepositoryException("Graph settings not stored...");
		} finally {
			conn.close();
		}

	}

	@Override
	public InstanceWithProperties setNetwork(String networkType, InstanceWithProperties network) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			NetworkType nwType = visType.getNetworkType(conn, networkType, network.getLocale());
			if ( nwType != null ) {
				NodeType nt = nwType.getNetworkNode();
				if ( nt != null) {
					// initialize the network properly
					if ( validateInstance(conn, nwType, nt, network.getId(), network)) {
                        if ( !conn.isActive()) {
                            conn.begin();
                        }
						processInstanceWithProperties(conn, nwType.getNetworkNode(), network);
						// 
						conn.commit();
						if ( network.getId()!=null) {
							return getNetwork(conn, networkType, network.getId(), network.getLocale(), true);
						}	

					}
				}
			}
			return null;
		} finally {
			conn.close();
		}
	}
	@Override
	public InstanceWithProperties getNode(String networkType, String nodeTypeUri, String nodeInstance, Locale locale, boolean complete) throws RepositoryException  {
		ObjectConnection conn = objectService.getConnection();
		try {
			NetworkType project = visType.getNetworkType(conn, networkType, locale);
			if ( project != null ) {
				//
				if ( ResourceUtils.isSubject(conn, nodeInstance)) {
					NodeType nodeType = project.getNodeTypeById(nodeTypeUri);
					
					return getNodeWithData(conn, project, nodeType, nodeInstance, locale, complete);
				}
			}
			return null;
		} finally {
			conn.close();
		}
		
	}
	@Override
	public InstanceWithProperties setNode(String networkType, String graph, String network, InstanceWithProperties node) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			Locale locale = node.getLocale();
			if ( locale == null ) {
				locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
			}
			NetworkType nType = visType.getNetworkType(conn, networkType, locale);
			if ( networkType !=null) {
				// the node's type is the id of it's nodeType
				NodeType nt = nType.getNodeTypeById(node.getType());
				if ( nt !=null ) {
					// be sure initialize the graph 
					
					if ( node.getGraph() == null && graph != null) {
						node.setGraph(graph);
					}
					// node may contain a network uri
					if ( validateInstance(conn, nType, nt, network, node)) {
						conn.begin();
						processInstanceWithProperties(conn, nt, node);
						if ( network!= null && nType.getNodeInNetworkUri()!=null) {
							processInstanceToNetwork(conn, nType, nt, node.getId(), network);
						}
						conn.commit();
					}
					return getNode(conn, nType, nt, network, node.getId(), node.getLocale());
				}
			}
			
			return null;
		} finally {
			conn.close();
		}
		

	}
	
	/* (non-Javadoc)
	 * @see eu.fp7.eagle.knowledge.vis.api.InstanceService#getProperties(java.lang.String, java.lang.String, java.lang.String, java.util.Locale)
	 */
	@Override
	public Set<Property> getProperties(String networkType, String nodeType, String node, final Locale locale)
			throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();

		try {
			Locale ntLoc = Locale.forLanguageTag(Locale.getDefault().getLanguage());
			NetworkType nw = visType.getNetworkType(conn, networkType, ntLoc);
			
			NodeType nt = nw.getNodeTypeById(nodeType);
			if ( nt != null ) {
				Resource nodeInstanceUri = conn.getValueFactory().createURI(node);
				Set<Property> props = getProperties(conn, nt, nodeInstanceUri);
				if (locale != null ) {
					props.removeIf(new Predicate<Property>() {
	
						@Override
						public boolean test(Property t) {
							if (t.getLocale() != null && ! locale.equals(t.getLocale())  ) {
								return true;
							}
							return false;
						}
					});
				}
				for (PropertyType pt : nt.getPropertyTypes().values() ) {
					boolean found = false;
					for ( Property used : props ) {
						if (pt.getId().equals(used.getType())) {
							found = true;
							break;
						}
					}
					if ( !found && pt.isEditable() ) {
						
						Property n = pt.asProperty(null, locale);
						props.add(n);
					}
				}
				
				return props;
				
			}
		} finally {
			conn.close();
		}
		return null;
	}
	/**
	 * 
	 * @param nodeType
	 * @param nodeInstance
	 * @param property
	 * @return
	 * @throws RepositoryException
	 */
	@Override
	public Property setProperty(String nw, String no, String nodeInstance, Property property) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			Resource instanceUri = conn.getValueFactory().createURI(nodeInstance);
			NetworkType networkType = visType.getNetworkType(conn, nw, property.getLocale());
			if ( networkType != null ) {
				NodeType nodeType = networkType.getNodeTypeById(no); 
						//visType.getNodeType(conn, property.getNetworkType(), property.getNodeType(), property.getLocale());
				if ( nodeType != null ) {
					conn.begin();
					PropertyType pt = nodeType.getPropertyTypeById(property.getType());
					property.setInstanceType(pt);
					// 
					Resource gr = getContext(conn, instanceUri, nodeType.getType());
					setProperty(conn, instanceUri, pt, property, gr);
					URI grUri = conn.getValueFactory().createURI(gr.stringValue());
					setLastModified(conn, instanceUri, new Date(), grUri);
					conn.commit();
//					property.setLabel(property.getValue());
//					property.setOldValue(property.getValue());
					property.setId(pt.getId());
					return property;
				}
			}
			return null;
			
		} finally {
			conn.close();
		}
	
	}
	@Override
	public boolean setEdge(String networkType, String edgeType, Edge edge, boolean delete) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			//conn.begin();
			NetworkType project = visType.getNetworkType(conn, networkType, edge.getLocale());
			if ( project != null ) {
				EdgeType et = project.getEdgeTypeById(edgeType);
				if ( et != null ) {
					conn.begin();
					processEdge(conn, project, et, edge, delete);
					conn.commit();
					return true;
				}
									
			}
			return false;
		} finally {
			conn.close();
		}
	}
	@Override
	public String getLabel(RepositoryConnection conn, Resource instance, Locale locale, Resource ...resources ) throws RepositoryException {
		Resource r = visType.getLabelProperty(conn, resources);
		if ( r != null ) {
			return ResourceUtils.getProperty(conn, instance, r.stringValue(), locale);
		}
		return ResourceUtils.getLabel(conn, instance, locale);
	}
	@Override
	public String getComment(RepositoryConnection conn, Resource instance, Locale locale, Resource ...resources ) throws RepositoryException {
		Resource r = visType.getCommentProperty(conn, resources);
		if ( r != null ) {
			return ResourceUtils.getProperty(conn, instance, r.stringValue(), locale);
		}
		return null;
		
	}
	@Override
	public boolean deleteGraph(String graph, Locale locale) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			Resource nodeInstanceUri = conn.getValueFactory().createURI(graph);
			Graph g = getGraph(conn, nodeInstanceUri, locale);
			if ( g.isEditable() ) {
				if ( ResourceUtils.isContext(conn, nodeInstanceUri)) {
					conn.begin();
					ResourceUtils.removeResource(conn, nodeInstanceUri);
					conn.commit();
					return true;
				}
				
			}
			return false;
		} finally {
			conn.close();
		}

	}
	@Override
	public boolean deleteNetwork(String nt, String network, Locale locale) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			NetworkType networkType = visType.getNetworkType(conn, nt, locale);
			Resource nodeInstanceUri = conn.getValueFactory().createURI(network);
			if ( ResourceUtils.isContext(conn, nodeInstanceUri)) {
				Graph g = getGraph(conn, networkType, nodeInstanceUri, locale);
				if ( g.getCount() > 1) {
					throw new RepositoryException(String.format("Network %s is a graph and contains other networks, delete with method: @DELETE /graph", network));
				}
			}
			if ( networkType != null ) {
				if ( ResourceUtils.isSubject(conn, nodeInstanceUri)) {
					if ( ResourceUtils.hasType(conn, nodeInstanceUri, networkType.getType())) {
						conn.begin();
						ResourceUtils.removeResource(conn, nodeInstanceUri);
						conn.commit();
					}
				}
			}
			return true;
			
		} finally {
			conn.close();
		}

	}
	@Override
	public boolean deleteNode(String networkTypeId, String nodeTypeId, String node, Locale locale) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			NetworkType networkType = visType.getNetworkType(conn, networkTypeId, locale);
			Resource nodeInstanceUri = conn.getValueFactory().createURI(node);
			if ( networkType != null ) {
				if ( ResourceUtils.isSubject(conn, nodeInstanceUri)) {
					NodeType nodeType = networkType.getNodeTypeById(nodeTypeId);
					if ( ResourceUtils.hasType(conn, nodeInstanceUri, nodeType.getType())) {
						conn.begin();
						ResourceUtils.removeResource(conn, nodeInstanceUri);
						conn.commit();
					}
					else {
						throw new RepositoryException("Incompatible node types");
					}
				}
				else {
					throw new RepositoryException("Node not found");
				}
			}
			return true;
		} finally {
			conn.close();
		}
	
	}
	@Override
	public boolean deleteProperty(String networkType, String nodeType, String instance, Property property) throws RepositoryException {
		ObjectConnection conn = objectService.getConnection();
		try {
			NetworkType nwType = visType.getNetworkType(conn, networkType, property.getLocale());
			if ( nwType != null ) {
				conn.begin();
				NodeType nt = nwType.getNodeTypeById(nodeType);
				if ( nt != null ) {
					PropertyType pt = nt.getPropertyTypeById(property.getType());
					Resource instanceUri = conn.getValueFactory().createURI(instance);
					Resource graph = getContext(conn, instanceUri, nt.getType());
					deleteProperty(conn, instanceUri, pt, property, graph);
					conn.commit();
					return true;
				}
			}
		} finally {
			conn.close();
		}
		return false;
	}
	
//	private Node getNode(RepositoryConnection conn, NetworkType networkType, NodeType nodeType, String node, Locale locale) throws RepositoryException {
//		return getNode(conn, networkType, nodeType, null, node, locale);
//	}
	/**
	 * Get & construct a node including properties
	 * @param conn
	 * @param networkType
	 * @param nt
	 * @param network
	 * @param nodeInstanceUri
	 * @param locale
	 * @return
	 * @throws RepositoryException
	 */
	private InstanceWithProperties getNode(RepositoryConnection conn, NetworkType networkType, NodeType nt, String network, String nodeInstance, Locale locale) throws RepositoryException {
		Resource nodeInstanceUri = conn.getValueFactory().createURI(nodeInstance);
		if ( ResourceUtils.isSubject(conn, nodeInstanceUri)) {
			// the type URI's possibly specifying the property for label & comment
			Resource nodeTypeURI = conn.getValueFactory().createURI(nt.getId());
			Resource networkTypeURI = conn.getValueFactory().createURI(networkType.getId());
			// obtain the label & comment for the subject resource
			Resource context = getContext(conn, nodeInstanceUri, nt.getType());
			String label = getLabel(conn, nodeInstanceUri, locale, nodeTypeURI, networkTypeURI);
			String comment = getComment(conn, nodeInstanceUri, locale, nodeTypeURI, networkTypeURI);
			// construct a node and initialize with values & properties
			InstanceWithProperties node = new InstanceWithProperties();
			node.setId(nodeInstanceUri.stringValue());
			node.setLabel(label);
			node.setComment(comment);
			node.setLocale(locale);
			node.setEditable(nt.isEditable());
			node.setInstanceType(nt);
			node.setGraph(context.stringValue());
			Set<Property> props = getProperties(conn, nt, nodeInstanceUri);
			// process the values
			node.setProperties(props);
			return node;
		}
		throw new RepositoryException(String.format("Node instance with id <%s> not found!", nodeInstanceUri));
	
	}
	private InstanceWithProperties getNodeWithData(RepositoryConnection conn, NetworkType networkType, NodeType nodeType, String nodeUri, Locale locale, boolean complete) throws RepositoryException  {
		Resource nodeInstance = conn.getValueFactory().createURI(nodeUri);
		Resource graphURI = getContext(conn, nodeInstance, nodeType.getType());
		Base graphBase = new BaseItem();
		graphBase.setId(graphURI.stringValue());
		graphBase.setEditable(networkType.isEditable());
		graphBase.setNamedGraph(graphURI.stringValue());
		// the type URI's possibly specifying the property for label & comment
		Resource nodeTypeURI = conn.getValueFactory().createURI(nodeType.getId());
		Resource networkTypeURI = conn.getValueFactory().createURI(networkType.getId());
		// obtain the label & comment for the subject resource
		String label = getLabel(conn, nodeInstance, locale, nodeTypeURI, networkTypeURI);
		String comment = getComment(conn, nodeInstance, locale, nodeTypeURI, networkTypeURI);
		
		
		InstanceWithProperties node = null;
		if ( complete ) {
			node = new InstanceWithData();
		}else {
			node = new InstanceWithProperties();
		}
		node.setId(nodeInstance.stringValue());
		node.setLabel(label);
		node.setComment(comment);
		// 
		//node.setNetwork(network);
		node.setLocale(nodeType.getLocale());
		node.setEditable(nodeType.isEditable());
		node.setInstanceType(nodeType);
		node.setGraph(graphBase.getId());
		Set<Property> props = getProperties(conn, nodeType, nodeInstance);
		node.setProperties(props);
		if ( node instanceof InstanceWithData  ) {
			InstanceWithData nd = (InstanceWithData) node;
			Set<NodeItem> nodes = getNodes(conn, networkType, graphBase, node, locale, complete);
			//Set<NodeItem> nodes = queryForNodes(conn, project, graphBase, node, locale, complete);
			nd.setNodes(nodes);
			Set<EdgeItem> edges = getEdges(conn, networkType, graphBase, node, locale, complete);
			nd.setEdges(edges);
		}
	
		return node;
	}
	/**
	 * add a new relationship between existing nodes
	 * @param conn
	 * @param config
	 * @param edgeType
	 * @param newEdge
	 * @throws RepositoryException
	 */
	private void addNewEdge(ObjectConnection conn, NetworkType config, EdgeType edgeType, Edge newEdge) throws RepositoryException {
		processEdge(conn, config, edgeType, newEdge, false);
	}
	private Set<Property> getProperties(RepositoryConnection conn, NodeType nodeType,  Resource nodeInstanceUri) throws RepositoryException {
		try {
			TupleQuery tQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, 
					"SELECT DISTINCT ?uri ?property ?target "
							+ "WHERE  { "
							+ "   ?uri ?property ?target . "
							+ "   FILTER (?property IN (" + StringUtils.join(nodeType.getPropertyTypesFormatted(), ",") + ")) ."	// use the proper edge types from project types
							+ "}");
			tQuery.setBinding("uri", nodeInstanceUri);
			TupleQueryResult result = tQuery.evaluate();
			// create property set
			Map<String, Property> propMap = new HashMap<>();
			while ( result.hasNext()) {
				BindingSet bs = result.next();
				PropertyType pt = null;
				if ( bs.hasBinding("property")) {
				    //g.setType(bs.getValue("property").stringValue());
				    pt = nodeType.getPropertyType(bs.getValue("property").stringValue());
				}
				if ( pt != null ) {
				    Property prop = propMap.get(pt.getType());
				    String value = null;
				    Locale locale = null;
				    
				    if ( bs.hasBinding("target")){
				        Value v = bs.getValue("target");
				        if ( v instanceof Literal ) {
				            Literal l = (Literal) v;
				            value = l.getLabel();
				            if ( l.getLanguage() != null ) {
				                locale = Locale.forLanguageTag(l.getLanguage());
				            }
				        }
				        //g.setValue(bs.getValue("target").stringValue());
				    }
				    if ( prop == null ) {
				        prop = pt.asProperty(value, locale);
				        propMap.put(pt.getType(), prop);
				    }
				    else {
				        // add additional values only when multiValue
				        if ( pt.getMultiValue()) {
				            prop.addValue(value, locale);
				        }
				    }
				}
//				if ( bs.hasBinding("uri")) {
//					// keep the subject uri with the property object
//					g.setId(bs.getValue("uri").stringValue());
//					
//				}
				//g.setNodeType(nodeType.getId());
			}
			Set<Property> set = new HashSet<Property>();
			set.addAll(propMap.values());
			return set;
			
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
	}
	
	/**
	 * Any instance may have labels & comments either as fully qualified properties (e.g. 
	 * check {@link InstanceWithProperties#getProperty(String)} for labelType & commentType.
	 * This method will construct missing properties for provided label & comment!. 
	 * @param networkType
	 * @param forType
	 * @param fromInstance
	 */
	private void processInstanceLabels(NetworkType networkType, InstanceWithProperties fromInstance) {
		NodeType forType = fromInstance.getInstanceType();
		final Set<Property> propsInUse = fromInstance.getPropertySet();
		// make sure the label & comment are duplicated for node type label & network type label 
		if ( fromInstance.getLabel()!= null ) {
			Property labelPropertyUsed = fromInstance.getProperty(forType.getLabelProperty());
			if ( labelPropertyUsed == null) {
				PropertyType labelType = forType.getPropertyType(forType.getLabelProperty(), networkType.getLabelProperty());
				labelPropertyUsed = labelType.asProperty(fromInstance.getLabel(), fromInstance.getLocale());
				propsInUse.add(labelPropertyUsed);
				
			}
			if ( networkType.getLabelProperty() != null ) {
				if (! labelPropertyUsed.getInstanceType().getType().equals(networkType.getLabelProperty())) {
					PropertyType labelType = networkType.getPropertyType(networkType.getLabelProperty(), RDFS.label);
					labelPropertyUsed = labelType.asProperty(fromInstance.getLabel(), fromInstance.getLocale());
					propsInUse.add(labelPropertyUsed);
					fromInstance.getInstanceType().addPropertyType(labelType);
				}
			}
		}
	
		if ( fromInstance.getComment()!= null ) {
			Property commentPropertyUsed = fromInstance.getProperty(forType.getCommentProperty());
			if ( commentPropertyUsed == null) {
				PropertyType commentType = forType.getPropertyType(forType.getCommentProperty(), networkType.getCommentProperty());
				commentPropertyUsed = commentType.asProperty(fromInstance.getComment(), fromInstance.getLocale());
				propsInUse.add(commentPropertyUsed);
			}
			if ( networkType.getCommentProperty() != null ) {
				if (! commentPropertyUsed.getInstanceType().getType().equals(networkType.getCommentProperty())) {
					PropertyType commentType = networkType.getPropertyType(networkType.getCommentProperty(), RDFS.comment);
					commentPropertyUsed = commentType.asProperty(fromInstance.getComment(), fromInstance.getLocale());
					propsInUse.add(commentPropertyUsed);
					fromInstance.getInstanceType().addPropertyType(commentType);
				}
			}
		}
		// reset the 
		fromInstance.setProperties(propsInUse);

	}
	/** 
	 * Validate & Pre-Process the provided instance. Ensure
	 * that the {@link InstanceWithProperties#getId()} and the
	 * {@link InstanceWithProperties#getNamedGraph()} return proper values! 
	 * 
	 * @param conn
	 * @param networkType
	 * @param networkUri
	 * @param fromInstance
	 * @return <code>true</code> if validation successfull, <code>false</code> otherwise
	 * @throws RepositoryException in case the named graph cannot be determined!
	 */
	private boolean validateInstance(RepositoryConnection conn, NetworkType networkType, NodeType nt, String networkUri, InstanceWithProperties fromInstance) throws RepositoryException {
		fromInstance.setInstanceType(nt);
		for ( Property p : fromInstance.getProperties()) {
			p.setInstanceType(networkType.getPropertyTypeById(p.getType()));
		}
		if ( fromInstance.getLocale() == null ) {
			// use default when not set
			fromInstance.setLocale(Locale.forLanguageTag(Locale.getDefault().getLanguage()));
		}
//		if ( fromInstance.getInstanceType() == null ) {
//		}
		Resource graph = null;
		if ( fromInstance.getId() == null ) {
			// 
			String uri = configurationService.getBaseUri() + "resource/" 
					+ fromInstance.getClass().getSimpleName().toLowerCase() +"-" 
					+ UUID.randomUUID().toString();
			fromInstance.setId(uri);
			if ( fromInstance.getGraph() == null ) {
				if ( networkUri != null) {
					Resource networkRes = conn.getValueFactory().createURI(networkUri);
					// new instance in an existing network - detect graph from network
					graph = getContext(conn, networkRes, networkType.getType());
					
				}
				else {
					// no network provide, no graph provided ...
					
					// when the network's node type equals to the instance's node type 
					// we can use the instance's uri as the context / named graph URI
					if ( fromInstance.getInstanceType().equals(networkType.getNetworkNode())) {
					    if ( !conn.isActive()) {
					        conn.begin();
					    }
					    Graph theGraph = new Graph();
					    theGraph.setLabel(fromInstance.getLabel());
					    theGraph.setComment(fromInstance.getComment());
					    theGraph.setEditable(true);
					    theGraph.setLocale(fromInstance.getLocale());
					    theGraph.getLanguages().add(fromInstance.getLocale());
					    theGraph.setType(networkType.getId());
					    graph = setGraph(conn, theGraph, fromInstance.getLocale());
					    //
						graph = conn.getValueFactory().createURI(theGraph.getId());
					}
					else {
						// it is not possible to determine the graph
						throw new RepositoryException("Cannot determine Graph!");
					}
				}
				// keep the named graph with the instance
				fromInstance.setNamedGraph(graph.stringValue());
			}
			else {
				graph = conn.getValueFactory().createURI(fromInstance.getGraph());
			}
		}
		else {
			// determine graph from the node itself by retriving the context
			// from the the triple <fromInstanceId> rdf:type <nodeType.getType()>
			Resource i = conn.getValueFactory().createURI(fromInstance.getId());
			graph =	getContext(conn, i, fromInstance.getInstanceType().getType());

			fromInstance.setNamedGraph(graph.stringValue());
		}
		String editable = ResourceUtils.getProperty(conn, graph, VIS.editable);
		// when the graph is 
		if ( editable != null && editable.equalsIgnoreCase("false") ) {
			throw new RepositoryException("Graph is not editable!");
		}
		// ensure, the provided label & comment are expressed as properties
		processInstanceLabels(networkType, fromInstance);
		return true;

	}
	private void processInstanceToNetwork(ObjectConnection conn, NetworkType networkType, NodeType sourceNodeType, String instance, String network) throws RepositoryException {
		// get the edge type
		EdgeType et = visType.getEdgeType(conn, networkType, sourceNodeType);
		//
		Edge inNetwork = new Edge();
		// source subject uri
		inNetwork.setFrom(instance);
		// target uri
		inNetwork.setTo(network);
		inNetwork.setInstanceType(et);
		addNewEdge(conn, networkType, et, inNetwork);
	
	}
	/**
	 * Create a resource, add the required types and process all the 
	 * properties
	 * @param conn
	 * @param forType
	 * @param fromInstance
	 * @throws RepositoryException
	 */
	private void processInstanceWithProperties(RepositoryConnection conn, NodeType forType, InstanceWithProperties fromInstance) throws RepositoryException {
		URI instanceUri = conn.getValueFactory().createURI(fromInstance.getId());
		URI typeUri = conn.getValueFactory().createURI(forType.getType());
		URI graph = conn.getValueFactory().createURI(fromInstance.getNamedGraph());
		if (!ResourceUtils.isSubject(conn, fromInstance.getId())) {
			ResourceUtils.addType(conn, instanceUri, typeUri, graph);
			setCreatedDate(conn, instanceUri, new Date(), graph);
		}
		else {
			setLastModified(conn, instanceUri, new Date(), graph);
		}
		// apply all rdf:types 
		for (String rdfType : forType.getTypes()) {
			URI otherType = conn.getValueFactory().createURI(rdfType.toString());
			if (!ResourceUtils.hasType(conn, instanceUri, otherType)) {
				ResourceUtils.addType(conn, instanceUri, otherType, graph);
			}
		}
		// process the properties piecewise
		for (String typeIdUsed : fromInstance.getPropertyTypeIds() ) {
			// obtain the property type from the nodeType info
			PropertyType propType = fromInstance.getInstanceType().getPropertyTypeById(typeIdUsed);
			// retrieve & process the properties for the current property type
			for ( Property propVal : fromInstance.getProperties(propType.getId())) {
				if ( propType.isEditable() ) {
					setProperty(conn, instanceUri, propType, propVal, graph);
				}
			}
		}

	}
	private void processEdge(ObjectConnection conn, NetworkType config, EdgeType edgeType, Edge newEdge, boolean delete) throws RepositoryException {
		Resource sourceUri = conn.getValueFactory().createURI(newEdge.getFrom());
		Resource contextURI = null;
		if ( ResourceUtils.isSubject(conn, sourceUri)) {
			for (String nodeType : edgeType.getSourceTypes()) {
				NodeType id = config.getNodeTypeById(nodeType);
				if (ResourceUtils.hasType(conn, sourceUri, id.getType())) {
					contextURI = getContext(conn, sourceUri, id.getType());
					break;
				}
			}
			if (contextURI != null ) {
				Resource inverseContext = null;
				Resource targetUri = conn.getValueFactory().createURI(newEdge.getTo());
				for (String nodeType : edgeType.getTargetTypes()) {
					NodeType id = config.getNodeTypeById(nodeType);
					if (ResourceUtils.hasType(conn, targetUri, id.getType())) {
						inverseContext = getContext(conn, targetUri, id.getType());
						break;
					}
				}
				// when no type found, inverse context is null nothingw
				if ( inverseContext != null ) {
					setEdgeValue(conn, sourceUri, edgeType.getType(), targetUri, contextURI.stringValue(), delete);
					
					if (edgeType.isSymmetric()) {
						setEdgeValue(conn, targetUri, edgeType.getType(), sourceUri, inverseContext.stringValue(), delete );
					}
					else {
						if (edgeType.getInverseOf() != null ) {
							setEdgeValue(conn, targetUri, edgeType.getInverseOf(), sourceUri, inverseContext.stringValue(), delete);				
						}
					}
				}
			}
		}
		
	}
	/**
	 * creates a literal with associated data type and locale 
	 * 
	 * @param conn
	 * @param value
	 * @param locale
	 * @param dataType
	 * @return
	 * @throws RepositoryException
	 */
	private Literal createLiteral(RepositoryConnection conn, String value, Locale locale, String dataType) throws RepositoryException {
		Literal literal = null;
		if ( dataType == null ) {
			dataType = "default";
		}
		switch(dataType) {
		case "boolean":
			literal = conn.getValueFactory().createLiteral(value, XSD.Boolean);
			break;
		case "int":
			literal = conn.getValueFactory().createLiteral(value, XSD.Int);
			break;
		case "language":
			literal = conn.getValueFactory().createLiteral(value, XSD.Language);
			break;
		case "double":
			literal = conn.getValueFactory().createLiteral(value, XSD.Double);
			break;
		default:
			if ( locale != null) {
				literal = conn.getValueFactory().createLiteral(value,locale.getLanguage());
			}
			else {
				literal = conn.getValueFactory().createLiteral(value);
			}
			break;
		}
		if ( literal == null ) {
			throw new IllegalStateException("Literal must not be null");
		}
		return literal;
	}

	/**
	 * Delete all triples of the provided property-type
	 * @param conn
	 * @param instance
	 * @param type
	 * @param property
	 * @param graph
	 * @throws RepositoryException
	 */
	private void deleteProperty(RepositoryConnection conn, Resource instance, PropertyType type, Property property, Resource graph) throws RepositoryException {
        URI graphURI = conn.getValueFactory().createURI(graph.stringValue());
        if ( type.getMultiValue() ) {
            ResourceUtils.removeProperty(conn,  instance, type.getType(), graphURI);
        }
	}
	private void createPropertyValue(RepositoryConnection conn, Resource instance, PropertyType type, PropertyValue value, URI graph) throws RepositoryException {
	    if ( value.getValue() != null ) {
	        URI propertyUri = conn.getValueFactory().createURI(type.getType());
	        Literal val = createLiteral(conn, value.getValue(), value.getLocale(), type.getDataType());
	        // create the new statement and add 
	        Statement stmt = conn.getValueFactory().createStatement(instance, propertyUri, val, graph);
	        conn.add(stmt);
	    }
        
	}
	private void setProperty(RepositoryConnection conn, Resource instance, PropertyType type, Property value, Resource graph) throws RepositoryException {
	    if ( value.isModified()) {
    	    URI graphURI = conn.getValueFactory().createURI(graph.stringValue());
    	    Set<Locale> processedLocale = new HashSet<Locale>();
    	    
    		for ( PropertyValue propVal : value.getValues() ) {
    		    if ( propVal.getLocale() != null && ! processedLocale.contains(propVal.getLocale())) {
    		        // multiLingual property
    		        // remove all properties for the given language before adding the new ones
    		        ResourceUtils.removeProperty(conn, instance, type.getType(), propVal.getLocale(), graphURI);
    		        processedLocale.add(propVal.getLocale());
    		    }
    		    else {
    		        // no language property - use the type's locale for delete-check
    		        if ( ! processedLocale.contains(type.getLocale())) {
    		            ResourceUtils.removeProperty(conn, instance, type.getType(), graphURI);
    		            processedLocale.add(type.getLocale());
    		        }
    		    }
    		    
    		    // create 
    		    createPropertyValue(conn, instance, type, propVal, graphURI);
    
    		}
	    }
	}


	private void setEdgeValue(RepositoryConnection conn, Resource instance, String prop, Resource target, String context, boolean delete) throws RepositoryException {
		URI predicate = conn.getValueFactory().createURI(prop); 
		URI contextURI = conn.getValueFactory().createURI(context);
		if ( delete ) {
			RepositoryResult<Statement> edgeResult = conn.getStatements(instance, predicate, target, true);
			while ( edgeResult.hasNext()) {
				conn.remove(edgeResult);
			}
		}
		else if ( !ResourceUtils.existsStatement(conn, instance, predicate, target, contextURI) ) {
			// no need to insert again
			Statement stmt = conn.getValueFactory().createStatement(instance, predicate, target, contextURI);
			conn.add(stmt);
		}
	}

	private Base getInstance(RepositoryConnection conn, String networkType, Resource instance, Locale locale, Resource ... resources ) throws RepositoryException {
		if ( ResourceUtils.isContext(conn, instance)) {
			Base b = new BaseItem();
			b.setId(instance.stringValue());
			b.setLabel(getLabel(conn, instance,  locale, resources));
			b.setComment(getComment(conn, instance,  locale, resources));
			b.setLocale(locale);
			b.setEditable(visType.isEditable(conn, resources));
			return b;
		}
		if (  ResourceUtils.isSubject(conn, instance)) {
			if ( ResourceUtils.hasType(conn, instance, networkType)) {
				Base b = new BaseItem();
				b.setId(instance.stringValue());
				b.setLabel(getLabel(conn, instance,  locale, resources));
				b.setComment(getComment(conn, instance,  locale, resources));
				b.setLocale(locale);
				b.setEditable(visType.isEditable(conn, resources));
				return b;
			}
		}
//		else {
//			if ( ResourceUtils.isContext(conn, instance)) {
//				Base b = new BaseItem();
//				b.setId(instance.stringValue());
//				b.setLabel(getLabel(conn, instance,  locale, resources));
//				b.setComment(getComment(conn, instance,  locale, resources));
//				b.setLocale(locale);
//				b.setEditable(visType.isEditable(conn, resources));
//				return b;
//			}
//		}
		throw new IllegalArgumentException("At least one Resource-URI must be provided!");

	}

	private InstanceWithData getGraphData(RepositoryConnection conn, NetworkType project, Graph gr, Locale locale, boolean complete) throws RepositoryException {
		NodeType nt = project.getNetworkNode();
		
		GraphWithData graph = new GraphWithData();
		// set the id 
		graph.setId(gr.getId());
		graph.setNamedGraph(gr.getId());
		// keep the network type (for convenience)
		graph.setLocale(locale);
		graph.setLabel(gr.getLabel());
		graph.setComment(gr.getComment());
		// keep the languages
		graph.setLanguages(gr.getLanguages());
		// check for label & comment
		graph.setInstanceType(nt);
		// set editable to the graph's editable setting
		graph.setEditable(gr.isEditable());
		// project overrides graph when set to false
		if (! project.isEditable() ) {
			graph.setEditable(false);
		}
		// handle property values & types
		Collection<Property> values = new ArrayList<>();
		PropertyType pt = nt.getPropertyType(project.getLabelProperty(), RDFS.label);
		if ( pt != null ) {
			// create the property for the network's label
			Property value = new Property();
//			value.setId(graph.getId());
			value.setId(graph.getId());
			value.setLabel(graph.getLabel());
            value.addValue(graph.getLabel(),  graph.getLocale());
			value.setLocale(graph.getLocale());
			value.setEditable(graph.isEditable());
			//
			value.setNamedGraph(graph.getNamedGraph());
			//value.setNetwork(network.getId());
//			value.setNodeType(nt.getId());
			value.setInstanceType(pt);
			values.add(value);
			
		}
		pt = nt.getPropertyType(project.getCommentProperty(), RDFS.comment);
		if ( pt != null ) {
			Property value = new Property();
			value.setId(graph.getId());
//			value.setId(graph.getId());
			value.setLabel(graph.getComment());
			value.addValue(graph.getComment(),  graph.getLocale());
			value.setLocale(graph.getLocale());
			value.setEditable(graph.isEditable());
			//
			value.setNamedGraph(graph.getNamedGraph());
			//value.setNetwork(network.getId());
//			value.setNodeType(nt.getId());
			value.setInstanceType(pt);
			values.add(value);
			
		}

		graph.setProperties(values);
		// query for the nodes in a network
		Set<NodeItem> nodes = getNodes(conn, project, graph, null, locale, complete);
		graph.setNodes(nodes);
		Set<EdgeItem> edges = getEdges(conn, project, graph, null, locale, complete);
		graph.setEdges(edges);
		return graph;
		

	}


	private Graph getGraph(RepositoryConnection conn, Resource graphUri, Locale locale) throws RepositoryException {
		if ( ResourceUtils.isContext(conn, graphUri)) {
			Graph g = new Graph();
			g.setId(graphUri.stringValue());
			// TODO: check for networktype
			//g.setNetworkType(nt.getId());
			Set<Locale> locales = new HashSet<>();
			String s = ResourceUtils.getProperty(conn, graphUri, VIS.language);
			if ( s != null) {
				Locale l = Locale.forLanguageTag(s).stripExtensions();
				g.setLocale(l);
			}
			else {
				g.setLocale(locale);
			}
			for (String lang : ResourceUtils.getProperties(conn, graphUri, VIS.hasLanguage))  {
				locales.add(Locale.forLanguageTag(lang).stripExtensions());
			};
			g.setLanguages(locales);
			String editable = ResourceUtils.getProperty(conn, graphUri, VIS.editable);
			if ( editable!=null) {
				g.setEditable(Boolean.valueOf(editable)); 
			}
			else {
				g.setEditable(true);
			}
			// Network network = getNetwork(conn, project, networkId, locale);
			String label = ResourceUtils.getProperty(conn, graphUri, RDFS.label, g.getLocale());
			String comment = ResourceUtils.getProperty(conn, graphUri, RDFS.comment, g.getLocale());
			g.setLabel(label);
			g.setComment(comment);
			return g;
		}
		throw new RepositoryException(String.format("URI <%s> is not a named graph!", graphUri.stringValue()));
	}
	private InstanceWithData getNetwork(ObjectConnection conn, String networkType, String network, Locale locale, boolean complete) throws RepositoryException {
		//conn.begin();
		NetworkType project = visType.getNetworkType(conn, networkType, locale);
		if ( project != null ) {
			Resource networkTypeUri = conn.getValueFactory().createURI(networkType);
			Resource networkUri = conn.getValueFactory().createURI(network);
			Resource graphUri = getContext(conn, networkUri, project.getType());
			
			// TODO: make network
			Base b = getInstance(conn, project.getType(), graphUri, locale, graphUri, networkTypeUri);
			return getNetwork(conn, project, b, networkUri, locale, complete);
								
		}
		return null;
		
	}
	private InstanceWithData getNetwork(RepositoryConnection conn, NetworkType project, Base graph, Resource networkId, Locale locale, boolean complete) throws RepositoryException {
		if ( ResourceUtils.isSubject(conn, networkId)) {
			InstanceWithData network = new InstanceWithData();
			// set the id 
			network.setId(networkId.stringValue());
			network.setNamedGraph(graph.getId());
			// keep the network type (for convenience)
			// check for label & comment
			network.setLabel(ResourceUtils.getProperty(conn, networkId, project.getNetworkNode().getLabelProperty(), locale));
			network.setComment(ResourceUtils.getProperty(conn, networkId, project.getNetworkNode().getCommentProperty(), locale));
			// set the node type  
			NodeType nt = project.getNetworkNode();
			network.setInstanceType(nt);
			// set editable to the graph's editable setting
			network.setEditable(graph.isEditable());
			// handle property values & types
			Collection<Property> values = new ArrayList<>();
			PropertyType pt = nt.getPropertyType(project.getLabelProperty(), RDFS.label);
			if ( pt != null ) {
				// create the property for the network's label
				Property value = new Property();
				value.setId(network.getId());
				value.setLabel(network.getLabel());
				value.addValue(network.getLabel(), network.getLocale());
				value.setLocale(network.getLocale());
				value.setEditable(network.isEditable());
				//
				value.setNamedGraph(network.getNamedGraph());
				//value.setNetwork(network.getId());
//				value.setNodeType(nt.getId());
				value.setInstanceType(pt);
				value.setLocale(locale);
				values.add(value);
				
			}
			pt = nt.getPropertyType(project.getCommentProperty(), RDFS.comment);
			if ( pt != null ) {
				Property value = new Property();
				value.setId(network.getId());
				value.setLabel(network.getComment());
				value.addValue(network.getComment(), network.getLocale());
				value.setLocale(network.getLocale());
				value.setEditable(network.isEditable());
				//
				value.setNamedGraph(network.getNamedGraph());
				value.setInstanceType(pt);
				value.setLocale(locale);
				values.add(value);
				
			}

			network.setProperties(values);
			// query for the nodes in a network
			Set<NodeItem> nodes = getNodes(conn, project, graph, network, locale, complete);
			network.setNodes(nodes);
			Set<EdgeItem> edges = getEdges(conn, project, graph, network, locale, complete);
			network.setEdges(edges);
			return network;
			
		}
		throw new RepositoryException("not found");
	}
	private Graph getGraph(RepositoryConnection conn, NetworkType project, Resource graphRes, Locale locale ) throws RepositoryException {
		Set<GraphItem> items = getGraphs(conn, project, locale, "<"+graphRes.stringValue()+">");
		if ( items.size() > 0) {
			GraphItem i = items.iterator().next();
			Graph g = getGraph(conn, graphRes, locale);
			g.setCount(i.getNetworkCount());
			return g;
		}
		throw new RepositoryException(String.format("Graph <%s> does not exist or has no netork of type <%s>", graphRes.stringValue(), project.getType()));
	}
	private Set<GraphItem> getGraphs(RepositoryConnection conn, NetworkType project, Locale locale, String ... graphFormatted) throws RepositoryException {
		Resource networkType_URI = conn.getValueFactory().createURI(project.getType());
		try {
			ArrayList<String> types = new ArrayList<>();
			types.add(String.format("<%s>", project.getType()));
			types.add(String.format("<%s>", VIS.Graph));
			String graphFilter = "";
			if ( graphFormatted != null && graphFormatted.length > 0) {
				// Graph URI's must be provided as formatted uri's, e.g. "<http://uri.org>"
				graphFilter =  "FILTER( ?uri IN ("+ StringUtils.join(graphFormatted, ",") + ")) .";
			}
			// TODO: add union to find graphs without any network
			/*
	UNION { GRAPH ?uri 
		{
			?uri rdf:type <http://www.salzburgresearch.at/vis#Graph> .
	    	OPTIONAL {  
	      		?uri rdfs:label  ?title  .  
	      		FILTER(lang(?title)='en') .  
	      		OPTIONAL 
	        		?uri rdfs:comment  ?comment  .    
	        		FILTER(lang(?comment)='en') .        
	      		}
	    	}
		}  
	}
			 */
			TupleQuery tQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, 
				"SELECT DISTINCT ?uri ?title ?comment (COUNT(?_uri) as ?count) "
						+ "WHERE { "
						+ "  { GRAPH ?uri "
						+ "    {"
						+ "      ?uri a <" + VIS.Graph +"> . "
						+ "      OPTIONAL {"
						+ "        ?_uri ?rdfType ?networkType ."
						+ "      } "
						+ graphFilter
						+ "      OPTIONAL {"
						+ "        ?uri ?rdfsLabel ?title  . "
						+ "        FILTER(lang(?title)='"+locale.getLanguage()+"') . "
						+ "        OPTIONAL { "
						+ "          ?uri ?rdfsComment ?comment  . "
						+ "          FILTER(lang(?comment)='"+locale.getLanguage()+"') . "
						+ "        }"
						+ "      }"
						+ "    }"
						+ "  }"
						+ "  UNION "
						+ "  { GRAPH ?uri "
						+ "    {"
						+ "      ?_uri ?rdfType ?networkType ."
						+ graphFilter
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
						+ "} GROUP BY ?uri ?title ?comment ");
			//		tQuery.setBinding("linkToNetwork", linkToNetwork_URI);
			tQuery.setBinding("networkType", networkType_URI);
			tQuery.setBinding("rdfsLabel", RDFS.label_URI);
			tQuery.setBinding("rdfsComment", RDFS.comment_URI);
			tQuery.setBinding("rdfType", RDF.type_URI);
			// filter for provided instance
			// 
			TupleQueryResult result = tQuery.evaluate();
			Set<GraphItem> graphList = new HashSet<>();
			while ( result.hasNext()) {
				BindingSet bs = result.next();
				GraphItem g = new GraphItem();
				if ( bs.hasBinding("uri")) {
					g.setId(bs.getValue("uri").stringValue());
				}
				if ( bs.hasBinding("title")){
					g.setLabel(bs.getValue("title").stringValue());
				}
				else {
				    // use the id as label
				    g.setLabel(g.getId());
				}
				if ( bs.hasBinding("comment")){
					g.setComment(bs.getValue("comment").stringValue());
				}
				if ( bs.hasBinding("count")) {
					Integer v = new Integer(bs.getValue("count").stringValue());
					g.setCount(v);
				}
				g.setEditable(project.isEditable());
				g.setType(project.getId());
				g.setLocale(locale);
				graphList.add(g);
			}
			return graphList;

		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		}

	}


	private Set<NetworkItem> getNetworks(RepositoryConnection conn, NetworkType project, Graph graph, Locale locale, boolean countAll) throws RepositoryException {
		Resource networkType_URI = conn.getValueFactory().createURI(project.getType());
		Resource graph_URI = null;
		String graphSparql = "?graph";
		
		if ( graph != null ) {
			graph_URI = conn.getValueFactory().createURI(graph.getId());
			graphSparql = "";
		}
	
		try {
			String filter = "";
			if ( countAll ) {
				filter =  "      OPTIONAL {"
						+ "        ?connected ?edgeType ?uri . "
						+ "	       FILTER( ?edgeType IN ("+ StringUtils.join(project.getEdgeTargets(), ",") + ")) . "
						+ "      }";
			}
			else {
				filter =  "      OPTIONAL {"
						+ "        ?uri ?edgeType ?connected . "
						+ "	       FILTER( ?edgeType IN ("+ StringUtils.join(project.getEdgeTargets(project.getType()), ",") + ")) . "
						+ "      }";
			}

			TupleQuery tQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, 
				"SELECT DISTINCT "+graphSparql+" ?uri ?title ?comment (COUNT(?connected) as ?count) "
						+ "WHERE "
						+ "{ "
						+ "  { GRAPH ?graph { "
						+ "      ?uri ?rdfType ?networkType . "
						+        filter 		// insert either filter for direct connection or all
						+ "      OPTIONAL {"
						+ "        ?uri "+ project.getNetworkNode().getLabelUriFormatted()+" ?title  . "
						+ "        FILTER(lang(?title)='"+locale.getLanguage()+"') . "
						+ "        OPTIONAL { "
						+ "          ?uri "+ project.getNetworkNode().getCommentUriFormatted() +" ?comment  . "
						+ "          FILTER(lang(?comment)='"+locale.getLanguage()+"') . "
						+ "        }"
						+ "      } "
						+ "    } "
						+ "  } "
						+ "} GROUP BY "+graphSparql+" ?uri ?title ?comment ");
			tQuery.setBinding("networkType", networkType_URI);
			tQuery.setBinding("rdfType", RDF.type_URI);
			// filter for provided instance
			if ( graph_URI != null ) {
				tQuery.setBinding("graph", graph_URI);
			}
			// 
			Set<NetworkItem> graphs = new HashSet<>();
			TupleQueryResult result = tQuery.evaluate();
			while ( result.hasNext()) {
				BindingSet bs = result.next();
				NetworkItem g = new NetworkItem();
				g.setType(project.getId());
				if ( bs.hasBinding("uri")) {
					g.setId(bs.getValue("uri").stringValue());
				}
				if ( bs.hasBinding("title")){
					g.setLabel(bs.getValue("title").stringValue());
				}
				else {
				    g.setLabel(g.getId());
				}
				if ( bs.hasBinding("comment")){
					g.setComment(bs.getValue("comment").stringValue());
				}
				if ( bs.hasBinding("count")) {
					Integer v = new Integer(bs.getValue("count").stringValue());
					g.setCount(v);
				}
				if ( bs.hasBinding("graph")) {
					//
					g.setNamedGraph(bs.getValue("graph").stringValue());
				}
				else {
					if ( graph_URI!=null) {
						g.setNamedGraph(graph_URI.stringValue());
					}
				}
				g.setLocale(locale);
				if ( graph!=null) {
					g.setEditable(graph.isEditable());
				}
				else {
					g.setEditable(project.isEditable());
				}
				graphs.add(g);
			}
			return graphs;

		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		}
	}
	private void setLastModified(RepositoryConnection conn, Resource instanceUri, Date date, URI graphUri) throws RepositoryException {
		RepositoryResult<Statement> oldModified = conn.getStatements(instanceUri, DCTERMS.modified_URI, null, true, graphUri);
		while (oldModified.hasNext()) {
			conn.remove(oldModified.next());
		}
		Literal lit = conn.getValueFactory().createLiteral(date);
		Statement created = conn.getValueFactory().createStatement(instanceUri, DCTERMS.modified_URI, lit, graphUri);
		conn.add(created);

	}
	private void setCreatedDate(RepositoryConnection conn, Resource instanceUri, Date date, URI graphUri) throws RepositoryException {
		Literal lit = conn.getValueFactory().createLiteral(date);
		Statement created = conn.getValueFactory().createStatement(instanceUri, DCTERMS.created_URI, lit, graphUri);
		conn.add(created);

	}

	/**
	 * Helper method for obtaining the GRAPH (or context) URI 
	 * of the provided instance uri. In fact the 
	 * uri is retrieved by inspecting the triple <p><i>instanceURI rdf:type targetTypeURI</i></p>
	 * @param conn
	 * @param instanceURI
	 * @param targetTypeURI
	 * @return
	 * @throws RepositoryException in case of a repository failure
	 * @throws IllegalStateException in case the resource is not of the requested rdf:type
	 */
	private Resource getContext(RepositoryConnection conn, Resource instanceUri, String targetType) throws RepositoryException {
		Resource targetTypeUri = conn.getValueFactory().createURI(targetType);
		RepositoryResult<Statement> targetTypeStmt = conn.getStatements(instanceUri, RDF.type_URI, targetTypeUri, true);
		if ( targetTypeStmt.hasNext() ) {
			Statement next = targetTypeStmt.next();
			return next.getContext();
		}
		throw new IllegalStateException(
				String.format("Instance: <%s> does not have the proper type <%s> assigned!", instanceUri.stringValue(), targetType));
	}
}
