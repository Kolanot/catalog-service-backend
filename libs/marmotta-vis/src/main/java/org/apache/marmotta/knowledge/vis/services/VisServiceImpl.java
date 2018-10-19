package org.apache.marmotta.knowledge.vis.services;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.marmotta.knowledge.vis.api.InstanceService;
import org.apache.marmotta.knowledge.vis.api.TypeService;
import org.apache.marmotta.knowledge.vis.api.VisService;
import org.apache.marmotta.knowledge.vis.dao.BaseItem;
import org.apache.marmotta.knowledge.vis.dao.Edge;
import org.apache.marmotta.knowledge.vis.dao.EdgeType;
import org.apache.marmotta.knowledge.vis.dao.Graph;
import org.apache.marmotta.knowledge.vis.dao.GraphItem;
import org.apache.marmotta.knowledge.vis.dao.InstanceWithData;
import org.apache.marmotta.knowledge.vis.dao.InstanceWithProperties;
import org.apache.marmotta.knowledge.vis.dao.NetworkItem;
import org.apache.marmotta.knowledge.vis.dao.NetworkType;
import org.apache.marmotta.knowledge.vis.dao.NodeType;
import org.apache.marmotta.knowledge.vis.dao.Property;
import org.apache.marmotta.knowledge.vis.dao.PropertyType;
import org.openrdf.repository.RepositoryException;

@ApplicationScoped
public class VisServiceImpl implements VisService {
	@Inject
	TypeService visType;
	@Inject
	InstanceService visInstance;
	/**
	 * Retrieve a list of all project definitions which may be used by the 
	 * user interface
	 * @return
	 */
	@Override
	public Set<BaseItem> getNetworkTypes(@NotNull Locale locale) {
		try {
			return visType.getNetworkTypes(locale);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public NetworkType getNetworkType(@NotNull String networkType, @NotNull Locale locale) {
		try {
			return visType.getNetworkType(networkType, locale);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	/**
	 * Load the distinct types for the project
	 * @param networkType
	 * @return
	 */
	@Override
	public Collection<NodeType> getNodeTypes(String networkType, Locale locale) {
		try {
			NetworkType p = visType.getNetworkType(networkType, locale);
			return p.getNodeTypes();
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public NodeType getNodeType(String project, String nodeTypeInstance, Locale locale) {
		try {
			return visType.getNodeType(project, nodeTypeInstance, locale);
			
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public InstanceWithProperties getNodeTemplate(String networkType, String nodeTypeUri, Locale locale) {
		try {
			NetworkType nw = visType.getNetworkType(networkType, locale);
			if ( nw != null) {
				NodeType nt = nw.getNodeTypeById(nodeTypeUri);
				if ( nt!= null) {
					String label = null;
					String comment = null;
					InstanceWithProperties node = new InstanceWithProperties();
					node.setId(null);
					node.setLabel(label);
					node.setComment(comment);
					node.setLocale(nt.getLocale());
					node.setEditable(nt.isEditable());
					node.setInstanceType(nt);
					Set<Property> props = new HashSet<>();
					for ( PropertyType pt : nt.getPropertyTypes().values()) {
						props.add(pt.asProperty(null,locale));
					}
					// process the values
					node.setProperties(props);
					return node;
				}
			}
			throw new IllegalArgumentException(String.format("Provided type URI's %s, %s are of type valid!", networkType, nodeTypeUri));
			
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}

	}
	@Override
	public InstanceWithProperties getNode(String networkType, String nodeTypeUri, String nodeInstance, Locale locale, boolean complete) {
		try {
			return visInstance.getNode(networkType, nodeTypeUri, nodeInstance, locale, complete);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public InstanceWithProperties setNode(String config, String nodeTypeUri, String graph, String network, InstanceWithProperties node) {
		try {
			return visInstance.setNode(config, graph, network, node);			
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}

	}
	/**
	 * Load the distinct edge-types for the project
	 * @param project
	 * @return
	 */
	@Override
	public Collection<EdgeType> getEdgeTypes(String networkType, Locale locale) {
		try {
			NetworkType p = visType.getNetworkType(networkType, locale);
			return p.getEdgeTypes();
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
			
	}
	/**
	 * Load the distinct property-types for the project
	 * @param project
	 * @return
	 */
	@Override
	public Collection<PropertyType> getPropertyTypes(String networkType, Locale locale) {
		try {
			NetworkType p = visType.getNetworkType(networkType, locale);
			return p.getPropertyTypes().values();
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public EdgeType getEdgeType(String project, String edgeType, Locale locale) {
		try {
			NetworkType p = visType.getNetworkType(project, locale);
			return p.getEdgeTypeById(edgeType);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public Graph getGraph(String graph, Locale locale) {
		try {
			return visInstance.getGraph(graph, locale);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	
	public InstanceWithData getGraphData(String forProject, String graph, Locale locale, boolean countAll) {
		try {
			return visInstance.getGraphData(forProject, graph, locale, countAll);
			
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public Set<GraphItem> getGraphs(String forProject, Locale locale) {
		try {
			return visInstance.getGraphs(forProject, locale);
			
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}

		// 
	}
	
	@Override
	public InstanceWithProperties getNetwork(String networkType, String network, Locale locale, boolean complete) {
		try {
			return visInstance.getNetwork(networkType, network, locale, complete);	
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}

	}
	@Override
	public boolean deleteGraph(String graph) {
		try {
			Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
			return visInstance.deleteGraph(graph, locale);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
	
		}

	}
	@Override
	public boolean deleteNetwork(String networkType, String network) {
		try {
			Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
			return visInstance.deleteNetwork(networkType, network, locale);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
	
		}
		
	}
	@Override
	public boolean deleteNode(String networkType, String nodeType, String node) {
		try {
			Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
			return visInstance.deleteNode(networkType, nodeType, node, locale);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public Property updateProperty(String networkType, String nodeType,  String instance, Property property) {
		try {
			return visInstance.setProperty(networkType, nodeType, instance, property);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}

	}
	@Override
	public Set<Property> getProperties(String networkType, String nodeType, String node, Locale locale) {
		try {
			return visInstance.getProperties(networkType, nodeType, node, locale);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	@Override
	public boolean deleteProperty(String networkType, String nodeType,  String instance, Property property) {
		try {
			return visInstance.deleteProperty(networkType, nodeType, instance, property);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}

	}

	
	@Override
	public Graph setGraph(Graph network, Locale locale) {
		try {
			return visInstance.setGraph(network, locale);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
	
		}
		
	}

	@Override
	public InstanceWithProperties setNetwork(String forProject, InstanceWithProperties network) {
		try {
			return visInstance.setNetwork(forProject, network);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
	
		}
		
	}
	@Override
	public Set<NetworkItem> getNetworks(String networkType, String graph, Locale locale, boolean countAll) {
		try {
			return visInstance.getNetworks(networkType, graph, locale, countAll);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}

		// 
	}
	@Override
	public boolean removeEdge(String networkType, String edgeType, Edge newEdge) {
		try {
			return visInstance.setEdge(networkType, edgeType, newEdge, true);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	@Override
	public boolean addEdge(String networkType, String edgeType, Edge newEdge) {
		try {
			return visInstance.setEdge(networkType,edgeType, newEdge, false);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException("TODO: use proper exception");
		}
	}
}
