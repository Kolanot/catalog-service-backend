package org.apache.marmotta.knowledge.vis.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.marmotta.knowledge.vis.ns.RDFS;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NetworkType extends TypeWithPropertyTypes {

//	private Set<Locale> languages;
	private Set<NodeType> nodeTypes;
	private Set<EdgeType> edgeTypes;
	private transient String nodeInNetworkUri;
	private transient Set<String> toChildren;
	
	@XmlTransient
	public EdgeType getEdgeType(String rdfType) {
		for (EdgeType nt : edgeTypes) {
			if (nt.getType()!=null && nt.getType().equals(rdfType)) {
				return nt;
			}
		}
		throw new IllegalStateException(String.format("EdgeType for target <%s> not found!", rdfType));
	}
	@XmlTransient
	public EdgeType getEdgeTypeById(String id) {
		for (EdgeType nt : edgeTypes) {
			if (nt.getId()!=null && nt.getId().equals(id)) {
				return nt;
			}
		}
		throw new IllegalStateException(String.format("EdgeType for target <%s> not found!", id));
	}
	@XmlTransient
	public NodeType getNodeType(String targetType) {
		for (NodeType nt : nodeTypes) {
			if ( nt.getType().equals(targetType)) {
				return nt;
			}
		}
		throw new IllegalStateException(String.format("NodeType for target <%s> not found!", targetType));
	}
	@XmlTransient
	public NodeType getNodeTypeById(String id) {
		for (NodeType nt : nodeTypes) {
			if (nt.getId()!=null && nt.getId().equals(id)) {
				return nt;
			}
		}
		throw new IllegalStateException(String.format("NodeType for target <%s> not found!", id));
	}
	@XmlTransient
	public NodeType getNetworkNode() {
		for (NodeType nt : nodeTypes) {
			if ( nt.getType().equals(this.getType())) {
				return nt;
			}
		}
		// when no network node found - create a
		// default network node (based on the network-type) 
		// and with default properties for RDFS.label and RDFS.comment
		NodeType nt = new NodeType();
		nt.setId(getId());
		nt.setLabel(getLabel());
		nt.setComment(getComment());
		nt.setEditable(true);
		nt.setType(getType());
		PropertyType labelType = nt.getPropertyType(getLabelProperty(), RDFS.label);
		nt.addPropertyType(labelType);
		nt.setLabelProperty(labelType.getType());
		PropertyType commentType = nt.getPropertyType(getCommentProperty(), RDFS.comment);
		nt.addPropertyType(commentType);
		nt.setCommentProperty(commentType.getType());
		return nt;
	}
	@XmlTransient
	public Set<String> getNodeTargetsFormatted() {
		return getNodeTargetsFormatted(true);
	}
	/**
	 * Retrieve the uri-formatted node types
	 * @param includeNetworkNode
	 * @return
	 */
	@XmlTransient
	public Set<String> getNodeTargetsFormatted(boolean includeNetworkNode) {
		final Set<String> nt = new HashSet<>();
		getNodeTargets(includeNetworkNode).forEach(new Consumer<String>() {
			public void accept(String t) {
				nt.add(String.format("<%s>", t));
			}
		});
		return nt;
	}
	@XmlTransient
	public Set<String> getNodeTargets() {
		return getNodeTargets(true);
	}
	@XmlTransient
	public Set<String> getNodeTargets(final boolean includeNetworkNode) {
		Set<String> targets = new HashSet<>();
		for ( NodeType nt : nodeTypes ) {
			if (includeNetworkNode || !( nt.getType().equals(getType()))) {
				targets.add(nt.getType());
			}
		}
		return targets;
	}
	@XmlTransient
	public Set<String> getEdgeTargets() {
		Set<String> targets = new HashSet<>();
		for ( EdgeType nt : edgeTypes ) {
			targets.add(String.format("<%s>", nt.getType()));
		}
		return targets;
	}
	/**
	 * Get all edgeTarges where either the source node 
	 * or the target node is of the given type
	 * @param forTargetType
	 * @return
	 */
	@XmlTransient
	public Set<String> getEdgeTargets(String forTargetType) {
		Set<String> targets = new HashSet<>();
		for ( EdgeType nt : edgeTypes ) {
			if ( nt.hasSourceType(forTargetType) ) {
				targets.add(String.format("<%s>", nt.getType()));
			}
			else if ( nt.hasTargetType(forTargetType)) {
				targets.add(String.format("<%s>", nt.getType()));
			}
		}
		return targets;
	}
	public Set<String> getEdgeTypesForSourceType(String sourceType) {
		Set<String> targets = new HashSet<>();
		for ( EdgeType nt : edgeTypes ) {
			if ( nt.hasSourceType(sourceType)) {
				targets.add(String.format("<%s>", nt.getType()));
			}
		}
		return targets;
	}
	public Set<String> getEdgeTypesForTargetType(String targetType) {
		Set<String> targets = new HashSet<>();
		for ( EdgeType nt : edgeTypes ) {
			if ( nt.hasTargetType(targetType)) {
				targets.add(String.format("<%s>", nt.getType()));
			}
		}
		if (getType().equals(targetType) && nodeInNetworkUri!=null ) {
			targets.add(String.format("<%s>",nodeInNetworkUri));
		}
		return targets;
	}

	/**
	 * @return the containmentUri
	 */
	@XmlTransient
	public String getNodeInNetworkUri() {
		return nodeInNetworkUri;
	}
	@JsonProperty("scope")
	public String getNetworkScope() {
	    return nodeInNetworkUri != null ? "network" : "graph";
	}
	/**
	 * @param containmentUri the containmentUri to set
	 */
	public void setNodeInNetworkUri(String containmentUri) {
		this.nodeInNetworkUri = containmentUri;
	}

	@XmlTransient
	/**
	 * @return the nodeTypes
	 */
    public Set<NodeType> getNodeTypes() {
		return nodeTypes;
	}
    /**
     * return the node types as map with the {@link NodeType#getId()} as 
     * key element 
     * @return
     */
	@JsonProperty(value="nodeType")
    public Map<String, NodeType> getNodeTypeMap() {
        Map<String, NodeType> map = new HashMap<String, NodeType>();
        for (NodeType item : nodeTypes) {
            map.put(item.getId(), item);
        }
        return map;
    }

	/**
	 * @param nodeTypes the nodeTypes to set
	 */
	public void setNodeTypes(Set<NodeType> nodeTypes) {
		this.nodeTypes = nodeTypes;
	}
	@XmlTransient
	/**
	 * @return the edgeTypes
	 */
	public Set<EdgeType> getEdgeTypes() {
		return edgeTypes;
	}
	/**
	 * Retrieve the edgeType's as a map with {@link EdgeType#getId()}
	 * as the key
	 * @return
	 */
	@JsonProperty(value="edgeType")
    public Map<String, EdgeType> getEdgeTypeMap() {
        Map<String, EdgeType> map = new HashMap<String, EdgeType>();
        for (EdgeType item : edgeTypes) {
            map.put(item.getId(), item);
        }
        return map;
    }
	/**
	 * @param edgeTypes the edgeTypes to set
	 */
	public void setEdgeTypes(Set<EdgeType> edgeTypes) {
		this.edgeTypes = edgeTypes;
	}
	/**
	 * @return the filterNodeToNetworkAssigned
	 */
	@XmlTransient
	public Set<String> getToChildren() {
		if ( toChildren == null ) {
			toChildren = new HashSet<>();
		}
		return toChildren;
	}
	@XmlTransient
	public Set<String> getToChildrenFormatted() {
		Set<String> targets = new HashSet<>();
		if ( toChildren != null ) {
			for ( String nt : toChildren ) {
				targets.add(String.format("<%s>", nt));
			}
		}
		return targets;
	}

	/**
	 * @param toChildren the properties denoting the top-down hierarchy to set
	 */
	public void setToChildren(Set<String> toChildren) {
		this.toChildren = toChildren;
	}
	/**
	 * @return the filterNodeToNetworkUnassigned
	 */
}
