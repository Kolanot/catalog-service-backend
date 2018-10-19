package org.apache.marmotta.knowledge.vis.webservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.marmotta.knowledge.vis.api.VisService;
import org.apache.marmotta.knowledge.vis.dao.BaseItem;
import org.apache.marmotta.knowledge.vis.dao.Edge;
import org.apache.marmotta.knowledge.vis.dao.EdgeType;
import org.apache.marmotta.knowledge.vis.dao.Graph;
import org.apache.marmotta.knowledge.vis.dao.GraphItem;
import org.apache.marmotta.knowledge.vis.dao.InstanceWithData;
import org.apache.marmotta.knowledge.vis.dao.InstanceWithProperties;
import org.apache.marmotta.knowledge.vis.dao.ListItem;
import org.apache.marmotta.knowledge.vis.dao.NetworkItem;
import org.apache.marmotta.knowledge.vis.dao.NetworkType;
import org.apache.marmotta.knowledge.vis.dao.NodeType;
import org.apache.marmotta.knowledge.vis.dao.Property;
import org.apache.marmotta.knowledge.vis.dao.PropertyType;

@Path("/vis")
public class VisWebService {

	@Inject
	private VisService vis;

	/**
	 * Load all the graphs having visualizable objects
	 * 
	 * @param identifier
	 * @param language
	 * @return
	 */
	@Path("/types")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNetworkTypes(@QueryParam("lang") String language) {
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		Set<BaseItem> projects = vis.getNetworkTypes(locale);
		List<ListItem> list = new ArrayList<>();
		for ( BaseItem nt : projects) {
			if (nt.isEditable())
				list.add(new ListItem(nt));
		}
		return Response.ok().entity(list).build();
	}
	/**
	 * Load all the graphs having visualizable objects
	 * 
	 * @param identifier
	 * @param language
	 * @return
	 */
	@Path("/type")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNetworkType(@QueryParam("id") String identifier, @QueryParam("lang") String language) {
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (identifier == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the configuration missing!", "id")).build();
		}

		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		NetworkType p = vis.getNetworkType(identifier, locale);
		return Response.ok().entity(p).build();
	}

	/**
	 * Retrieve the node types for a graph configuration
	 * 
	 * @param configuration
	 *            The configuration holding a graph definition
	 * @param type
	 *            The node type inside the graph definition, <code>null</code>
	 *            for all node types
	 * @param language
	 *            The language to retrieve the labels
	 * @return
	 */
	@Path("/type/nodeTypes")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNodeTypes(@QueryParam("id") String configuration, // the
																					// configuration
																					// for
																					// the
																					// graph
			@QueryParam("lang") String language) { // the (preferred) locale
		if (configuration == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the configuration missing!", "id")).build();
		}
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		Collection<NodeType> graphs = vis.getNodeTypes(configuration, locale);
		return Response.ok().entity(graphs).build();
	}

	/**
	 * Load all the graphs having visualizable objects
	 * 
	 * @param configuration
	 *            The configuration holding a graph definition
	 * @param type
	 *            The edge type inside the graph definition, <code>null</code>
	 *            for all edge types
	 * @param language
	 *            The language to retrieve the labels
	 * @return
	 */
	@Path("/type/edgeTypes")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEdgeTypes(@QueryParam("id") String configuration, // the
																					// configuration
																					// for
																					// the
																					// graph
//			@QueryParam("id") String type, // the node type inside the
//											// configuration or null for all
			@QueryParam("lang") String language) { // the (preferred) locale
		if (configuration == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the configuration missing!", "id")).build();
		}
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		Collection<EdgeType> graphs = vis.getEdgeTypes(configuration, locale);
		return Response.ok().entity(graphs).build();
	}
	@Path("/type/propertyTypes")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPropertyTypes(@QueryParam("id") String configuration, // the
																					// configuration
																					// for
																					// the
																					// graph
//			@QueryParam("id") String type, // the node type inside the
//											// configuration or null for all
			@QueryParam("lang") String language) { // the (preferred) locale
		if (configuration == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the configuration missing!", "id")).build();
		}
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		Collection<PropertyType> graphs = vis.getPropertyTypes(configuration, locale);
		return Response.ok().entity(graphs).build();
	}

	/**
	 * Load all the networks having visualizable objects
	 * 
	 * @param identifier
	 * @param language
	 * @return
	 */
	@Path("/networks")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNetworks(
			@QueryParam("networkType") String configuration, 
			@QueryParam("graph") String graph, 
			@QueryParam("complete") boolean complete, 
			@QueryParam("lang") String language) {
		if (configuration == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the configuration missing!", "networkType")).build();
		}

		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		Set<NetworkItem> networks = vis.getNetworks(configuration, graph, locale, complete);
		return Response.ok().entity(networks).build();
	}
	/**
	 * Load all the networks having visualizable objects
	 * 
	 * @param identifier
	 * @param language
	 * @return
	 */
	@Path("/network")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNetwork(
			@QueryParam("networkType") String configuration, 
			@QueryParam("id") String network, 
			@QueryParam("complete") boolean complete, 
			@QueryParam("lang") String language) {
		if (configuration == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Query-Parameter <%s> outlining the configuration missing!", "networkType")).build();
		}
		if (network == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Query-Parameter <%s> pointing to the requested network is missing!", "id")).build();
		}

		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		if ( complete ) {
		}
		InstanceWithProperties networks = vis.getNetwork(configuration, network, locale, complete);
		return Response.ok().entity(networks).build();
	}

	@Path("/network")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * 
	 * @param project
	 * @param network
	 * @return
	 */
	public Response setNetwork(
			@QueryParam("networkType")String networkType,
			@QueryParam("graph")String graph,
			InstanceWithData network) {
		if (networkType == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "networkType")).build();
		}
		if ( graph != null && network.getGraph() == null ) { 
			network.setGraph(graph);
		}
			
		
		InstanceWithProperties updated = vis.setNetwork(networkType, network);
		return Response.ok().entity(updated).build();
	}
	@Path("/graph/settings")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * 
	 * @param project
	 * @param network
	 * @return
	 */
	public Response setGraphSettings(Graph network) {
//		if ( network.getNetworkType()== null ) {
//			return Response.status(Response.Status.BAD_REQUEST)
//					.entity(String.format("Please provide the property in the payload object: <%s>!", "networkType")).build();
//		}
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (network.getLocale() != null) {
			locale = network.getLocale();
		}

		Graph updated = vis.setGraph(network, locale);
		return Response.ok().entity(updated).build();
	}
	
	@Path("/graph/settings")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGraphSettings(
			@QueryParam("id") String graph, 
			 @QueryParam("lang") String language) {
		if (graph == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "id")).build();
		}
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
	
		Graph res = vis.getGraph(graph, locale);
	
		return Response.ok().entity(res).build();
	}
	@Path("/network")
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	/**
	 * 
	 * @param project
	 * @param network
	 * @return
	 */
	public Response deleteNetwork(
			@QueryParam(value="networkType") String networkType,
			@QueryParam(value="id") String network) {
		if ( networkType== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Please provide the <%s> query parameter!", "networkType")).build();
		}
		if ( network== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Please provide the <%s> query parameter!", "id")).build();
		}
		if ( vis.deleteNetwork(networkType, network) ) {
			return Response.ok().build();
		}
		else {
			return Response.notModified().build();
		}
//		if (network.getNetwork().equals(network.getId())) {
//		}
	}
	@Path("/graph")
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	/**
	 * 
	 * @param project
	 * @param network
	 * @return
	 */
	public Response deleteGraph(@QueryParam(value="graph")String graph) {
		if ( graph== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Please provide the query parameter <%s> denoting the graph to delete", "graph")).build();
		}
		if (vis.deleteGraph(graph)) {
			return Response.ok().build();
		}
		else {
			return Response.notModified().build();
		}
	}
//	public Response deleteGraph(Graph network) {
//		if ( network.getId()== null ) {
//			return Response.status(Response.Status.BAD_REQUEST)
//					.entity(String.format("Please provide the property <%s> configuration in the payload object!", "id")).build();
//		}
//		if (vis.deleteGraph(network)) {
//			return Response.ok().build();
//		}
//		else {
//			return Response.notModified().build();
//		}
////		if (network.getNetwork().equals(network.getId())) {
////		}
//	}

	/**
	 * Load all the graphs having visualizable networks
	 * 
	 * @param identifier
	 * @param language
	 * @return
	 */
	@Path("/graphs")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGraphs(@QueryParam("networkType") String project, @QueryParam("lang") String language) {
		if (project == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the configuration missing!", "networkType")).build();
		}
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		Set<GraphItem> networks = vis.getGraphs(project, locale);

		return Response.ok().entity(networks).build();
	}
	@Path("/graph")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGraphData(
			@QueryParam("networkType") String project, 
			@QueryParam("graph") String graph, 
			@QueryParam("complete") boolean complete, 
			@QueryParam("lang") String language) {
		if (project == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the configuration is missing!", "networkType")).build();
		}
		if (graph == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "graph")).build();
		}
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		InstanceWithData res = vis.getGraphData(project, graph, locale, complete);

		return Response.ok().entity(res).build();
	}
	/**
	 * Load all nodes for a given network & configuration
	 * 
	 * @param identifier
	 * @param language
	 * @return
	 */
	@Path("/network/nodes")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNetworkNodes(
			//@formatter:off
			@QueryParam("networkType") String configuration, 
			@QueryParam("id") String network,
			@QueryParam("complete") boolean complete, 
			@QueryParam("lang") String language) {
			//@formatter:on
		if (configuration == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the configuration missing!", "networkType")).build();
		}
		if (network == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the node type is missing!", "network")).build();
		}

		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		InstanceWithProperties data = vis.getNetwork(configuration, network, locale, complete);
		if (data instanceof InstanceWithData ) {
			return Response.ok().entity(((InstanceWithData) data).getNodeMap()).build();
		}
		return Response.noContent().build();
	}


	/**
	 * Load load all the edges from a given network
	 * 
	 * @param identifier
	 * @param language
	 * @return
	 */
	@Path("/network/edges")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNetworkEdges(
			@QueryParam("networkType") String configuration, 
			@QueryParam("id") String network,
			@QueryParam("complete") boolean complete, 
			@QueryParam("lang") String language) {
		if (configuration == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the configuration missing!", "networkType")).build();
		}
		if (network == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the node type is missing!", "id")).build();
		}

		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		// net.setLabel(label);
		InstanceWithProperties data = vis.getNetwork(configuration, network, locale, complete);
		if (data instanceof InstanceWithData ) {
			return Response.ok().entity(((InstanceWithData) data).getEdges()).build();
		}
		return Response.noContent().build();
	}

	@Path("/node")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNode(
			//@formatter:off
			@QueryParam("networkType") String configuration, 
			@QueryParam("nodeType") String nodeType,
			@QueryParam("id") String nodeInstance, 
			@QueryParam("complete") boolean complete, 
			@QueryParam("lang") String language) {
			//@formatter:on
		if (configuration == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the configuration missing!", "networkType")).build();
		}
		if (nodeType == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the node type is missing!", "nodeType")).build();
		}
		Locale locale = Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}
		if ( nodeInstance == null ) {
			InstanceWithProperties result = vis.getNodeTemplate(configuration, nodeType, locale);
			return Response.ok().entity(result).build();
		}
		InstanceWithProperties data = vis.getNode(configuration, nodeType, nodeInstance, locale, complete);
		return Response.ok().entity(data).build();
	}

	
	@Path("/node")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setNode(
			@QueryParam("networkType") String networkType,
			@QueryParam("nodeType") String nodeType,
			@QueryParam("graph") String graph,
			@QueryParam("network") String network,
			// allow sending "complete" object (including) data
			InstanceWithData node) {	
		if (networkType == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "networkType")).build();
		}
		if (nodeType == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "nodeType")).build();
		}
		if (network == null && graph == null  ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Please provide one of the following query parameters: <%s>, <%s>!", "graph", "network")).build();
		}
		
		InstanceWithProperties newValues = vis.setNode(networkType, nodeType, graph, network, node);
		if ( newValues != null ) {
			return Response.ok().entity(newValues).build();
		}
		return Response.notModified().build();
	}
	@Path("/node")
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteNode(
			@QueryParam("networkType")String networkType,
			@QueryParam("nodeType") String nodeType,
			@QueryParam("id")String id ){
		if (networkType == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "networkType")).build();
		}
		if (nodeType == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "nodeType")).build();
		}
		if (id == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "id")).build();
		}
		if (vis.deleteNode(networkType, nodeType, id) ) {
			return Response.ok().build();
		}
		else {
			return Response.notModified().build();
		}
		
	}
	@Path("/node/properties")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNodeProperties(
			@QueryParam("networkType")String networkType,
			@QueryParam("nodeType") String nodeType,
			@QueryParam("id")String id,
			@QueryParam("lang") String language) {
		if (networkType == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "networkType")).build();
		}
		if (nodeType == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "nodeType")).build();
		}
		if (id == null) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "id")).build();
		}
		Locale locale = null;
		if (language != null) {
			locale = Locale.forLanguageTag(language).stripExtensions();
		}

		Set<Property> props = vis.getProperties(networkType, nodeType, id, locale);
		return Response.status(Response.Status.OK).entity(props).build();
	}
			
		
	
	@Path("/node/property")
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteNodeProperty(
			@QueryParam("networkType")String networkType,
			@QueryParam("nodeType")String nodeType,
			@QueryParam("id")String instance,
			Property property) {
		if ( networkType== null ) {
			return Response.status(Response.Status.METHOD_NOT_ALLOWED)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "networkType")).build();
		}
		if ( nodeType == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "nodeType")).build();
		}
		if (instance == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the graph's uri is missing!", "nodeType")).build();
		}
//		if ( property.getNetwork().equals(property.getId())) {
//			return Response.status(Status.NOT_MODIFIED).build();
//		}
		if (vis.deleteProperty(networkType, nodeType, instance, property) ){
			return Response.ok().build();
		}
		else {
			return Response.notModified().build();
		}
		
	}
	@Path("/node/property")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateNodeProperty(
			@QueryParam("networkType") String networkType, 
			@QueryParam("nodeType") String nodeType, 
			@QueryParam("id") String id, 
			Property property) {
		if ( networkType== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the networks type uri is missing!", "networkType")).build();
		}
		if (nodeType == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the node's type uri is missing!", "nodeType")).build();
		}
		if ( id== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the node instance uri is missing!", "id")).build();
		}
//		if ( property.getNetwork().equals(property.getId())) {
//			return Response.status(Status.NOT_MODIFIED).build();
//		}
		Property updated = vis.updateProperty(networkType, nodeType, id, property);
		return Response.ok(updated).build();
		
	}
	@Path("/edge")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addEdge(
			@QueryParam("networkType") String networkType,
			@QueryParam("edgeType") String edgeType,
			Edge newEdge) {
		if ( networkType== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the node instance uri is missing!", "networkType")).build();		}
		if ( edgeType== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the node instance uri is missing!", "edgeType")).build();		}
//		vis.addEdge(newEdge.getEdgeType(), newEdge);
		if ( vis.addEdge(networkType, edgeType, newEdge) ) {
			return Response.ok().build();
		}
		else {
			return Response.notModified().build();
		}
	}
	@Path("/edge")
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeEdge(
			@QueryParam("networkType") String networkType,
			@QueryParam("edgeType") String edgeType,
			Edge newEdge) {

		if ( newEdge.getFrom()== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Please provide the property in the payload object: <%s>!", "from")).build();
		}
		if ( networkType== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the node instance uri is missing!", "networkType")).build();		}
		if ( edgeType== null ) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("Parameter <%s> outlining the node instance uri is missing!", "edgeType")).build();		}
		vis.removeEdge(networkType, edgeType, newEdge);
		if ( vis.removeEdge(networkType, edgeType, newEdge) ) {
			return Response.ok().build();
		}
		else {
			return Response.notModified().build();
		}
	}

}
