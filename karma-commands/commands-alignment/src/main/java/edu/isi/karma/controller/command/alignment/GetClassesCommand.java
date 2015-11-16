package edu.isi.karma.controller.command.alignment;

import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.command.CommandType;
import edu.isi.karma.controller.command.WorksheetCommand;
import edu.isi.karma.controller.command.termpicker.TermPickerRecommendationForNodes;
import edu.isi.karma.controller.update.AbstractUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.modeling.alignment.Alignment;
import edu.isi.karma.modeling.alignment.AlignmentManager;
import edu.isi.karma.modeling.ontology.OntologyManager;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.rep.alignment.*;
import edu.isi.karma.view.VWorkspace;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class GetClassesCommand extends WorksheetCommand
{

	private final String nodeId;
	private final String alignmentId;
	final private INTERNAL_NODES_RANGE range;

	public enum INTERNAL_NODES_RANGE
	{
		allClasses, classesInModel, classesWithProperty, allClassesRaw
	}

	private enum JsonKeys
	{
		updateType, nodeLabel, nodeId, nodes, nodeUri
	}

	private String propertyURI;

	private static Logger logger = LoggerFactory.getLogger( GetClassesCommand.class.getSimpleName() );

	protected GetClassesCommand(
			String id,
			String model,
			String worksheetId,
			INTERNAL_NODES_RANGE range,
			String propertyURI,
			String nodeId,
			String alignmentId )
	{
		super( id, model, worksheetId );
		this.range = range;
		this.propertyURI = propertyURI;
		this.nodeId = nodeId;
		this.alignmentId = alignmentId;
	}

	@Override
	public String getCommandName()
	{
		return GetClassesCommand.class.getSimpleName();
	}

	@Override
	public String getTitle()
	{
		return "Get Classes";
	}

	@Override
	public String getDescription()
	{
		return null;
	}

	@Override
	public CommandType getCommandType()
	{
		return CommandType.notInHistory;
	}

	@Override
	public UpdateContainer doIt( Workspace workspace ) throws CommandException
	{
		final Set<LabeledLink> incomingLinks;
		final Set<LabeledLink> outgoingLinks;
		final Alignment alignment;
		if ( alignmentId != null )
		{
			alignment = AlignmentManager.Instance().getAlignment( alignmentId );
			if ( this.isExecutedInBatch() )
			{
				incomingLinks = alignment.getCurrentIncomingLinksToNode( nodeId );
				outgoingLinks = alignment.getCurrentOutgoingLinksToNode( nodeId );
			}
			else
			{
				incomingLinks = alignment.getIncomingLinksInTree( nodeId );
				outgoingLinks = alignment.getOutgoingLinksInTree( nodeId );
			}
		}
		else
		{
			incomingLinks = null;
			outgoingLinks = null;
			alignment = null;
		}

		Map<Node, Boolean> nodeSet = null;
		if ( range == INTERNAL_NODES_RANGE.classesInModel )
		{
			nodeSet = getClassesInModel( workspace );
		}
		else if ( range == INTERNAL_NODES_RANGE.allClasses )
		{
			nodeSet = getAllClasses( workspace );
		}
		else if ( range == INTERNAL_NODES_RANGE.classesWithProperty )
		{
			nodeSet = getClassesWithProperty( workspace, propertyURI );
		}
		else if ( range == INTERNAL_NODES_RANGE.allClassesRaw )
		{
			final OntologyManager ontMgr = workspace.getOntologyManager();
			final HashMap<String, Label> allClasses = ontMgr.getClasses();
			UpdateContainer upd = new UpdateContainer( new AbstractUpdate()
			{
				@Override
				public void generateJson(
						String prefix, PrintWriter pw,
						VWorkspace vWorkspace )
				{
					JSONArray nodesArray = new JSONArray();
					JSONObject obj = new JSONObject();
					for ( Entry<String, Label> entry : allClasses.entrySet() )
					{
						JSONObject nodeObj = new JSONObject();
						Label label = entry.getValue();
						nodeObj.put( JsonKeys.nodeLabel.name(), label.getDisplayName() );
						nodeObj.put( JsonKeys.nodeId.name(), label.getUri() );
						nodeObj.put( JsonKeys.nodeUri.name(), label.getUri() );
						nodesArray.put( nodeObj );
					}
					obj.put( JsonKeys.nodes.name(), nodesArray );
					pw.println( obj.toString() );
				}
			} );
			return upd;
		}

		if ( nodeSet == null )
		{
			nodeSet = new HashMap<Node, Boolean>();
		}
		final Map<Node, Boolean> finalNodeSet = nodeSet;
		final Map<Node, Boolean> finalNodeSetForTermPicker = getAllClasses( workspace );

		UpdateContainer upd = new UpdateContainer( new AbstractUpdate()
		{
			@Override
			public void generateJson(
					String prefix, PrintWriter pw,
					VWorkspace vWorkspace )
			{
				//TERMPICKER RECOMMENDATIOS === BEGIN
				TermPickerRecommendationForNodes nodeRecommendations = null;
				if ( incomingLinks != null && outgoingLinks != null && alignment != null )
				{
					HashSet<String> incomingProperties = new HashSet<>();
					HashSet<String> outgoingProperties = new HashSet<>();
					String uriProperty = "http://isi.edu/integration/karma/dev#classLink";
					for ( LabeledLink incomingLink : incomingLinks )
					{
						//do not include rdfs:label, uri, and url
						if ( !(incomingLink.equals( uriProperty )) )
						{
							incomingProperties.add( incomingLink.getUri() );
						}
					}
					for ( LabeledLink outgoingLink : outgoingLinks )
					{
						if ( !(outgoingLink.equals( uriProperty )) )
						{
							outgoingProperties.add( outgoingLink.getUri() );
						}

					}
					String incomingPropertyString = getPropertyString( incomingProperties );
					String outgoingPropertyString = getPropertyString( outgoingProperties );

					Node nodeOfRdfType = alignment.getNodeById( nodeId );
					String rdfType = nodeOfRdfType.getUri();

					nodeRecommendations = new TermPickerRecommendationForNodes( rdfType,
							incomingPropertyString,
							outgoingPropertyString );
				}
				//TERMPICKER RECOMMENDATIONS === END

				JSONObject obj = new JSONObject();
				JSONArray nodesArray = new JSONArray();

				try
				{
					obj.put( JsonKeys.updateType.name(), "InternalNodesList" );

					Map<Node, Boolean> nodeSetToProcess;
					if ( alignment != null )
					{
						nodeSetToProcess = finalNodeSetForTermPicker;
					}
					else
					{
						nodeSetToProcess = finalNodeSet;
					}

					Map<Node, Boolean> nodeSetOfRecommendedClasses = new HashMap<>();
					for ( Entry<Node, Boolean> entry : nodeSetToProcess.entrySet() )
					{
						Node node = entry.getKey();
						/*if ( !(node instanceof InternalNode) )
						{
							continue;
						}*/
						if ( nodeRecommendations != null && !nodeRecommendations.getRdfTypeRecommendations().isEmpty() )
						{
							if ( !nodeRecommendations.getRdfTypeRecommendations().contains( node.getUri() ) )
							{
								continue;
							}
						}
						nodeSetOfRecommendedClasses.put( entry.getKey(), entry.getValue() );
					}

					if ( nodeRecommendations != null && !nodeRecommendations.getRdfTypeRecommendations().isEmpty() )
					{
						for ( String rdfType : nodeRecommendations.getRdfTypeRecommendations() )
						{
							for ( Entry<Node, Boolean> entry : nodeSetOfRecommendedClasses.entrySet() )
							{
								if ( entry.getKey().getUri().equals( rdfType ) )
								{
									Node node = entry.getKey();

									JSONObject nodeObj = new JSONObject();
									String nodeLabelStr = node.getDisplayId();

									Label nodeLabel = node.getLabel();
									if ( nodeLabel.getUri() != null && nodeLabel.getNs() != null
											&& nodeLabel.getUri().equalsIgnoreCase( nodeLabel.getNs() ) )
									{
										nodeLabelStr = node.getId();
									}
									else if ( nodeLabel.getPrefix() == null && nodeLabel.getUri() != null )
									{
										nodeLabelStr = nodeLabel.getUri() + "/" + nodeLabelStr;
									}
									if ( entry.getValue().booleanValue() == false )
										nodeLabelStr += " (add)";
									nodeObj.put( JsonKeys.nodeLabel.name(), nodeLabelStr );
									nodeObj.put( JsonKeys.nodeId.name(), node.getId() );
									nodeObj.put( JsonKeys.nodeUri.name(), nodeLabel.getUri() );
									nodesArray.put( nodeObj );
								}
							}
						}
					}
					else
					{
						for ( Entry<Node, Boolean> entry : nodeSetOfRecommendedClasses.entrySet() )
						{
							Node node = entry.getKey();

							JSONObject nodeObj = new JSONObject();
							String nodeLabelStr = node.getDisplayId();

							Label nodeLabel = node.getLabel();
							if ( nodeLabel.getUri() != null && nodeLabel.getNs() != null
									&& nodeLabel.getUri().equalsIgnoreCase( nodeLabel.getNs() ) )
							{
								nodeLabelStr = node.getId();
							}
							else if ( nodeLabel.getPrefix() == null && nodeLabel.getUri() != null )
							{
								nodeLabelStr = nodeLabel.getUri() + "/" + nodeLabelStr;
							}
							if ( entry.getValue().booleanValue() == false )
								nodeLabelStr += " (add)";
							nodeObj.put( JsonKeys.nodeLabel.name(), nodeLabelStr );
							nodeObj.put( JsonKeys.nodeId.name(), node.getId() );
							nodeObj.put( JsonKeys.nodeUri.name(), nodeLabel.getUri() );
							nodesArray.put( nodeObj );
						}
					}

					obj.put( JsonKeys.nodes.name(), nodesArray );
					pw.println( obj.toString() );
				}

				catch (
						JSONException e
						)

				{
					e.printStackTrace();
				}
			}
		}

		);
		return upd;
	}

	private String getPropertyString( HashSet<String> properties )
	{
		StringBuilder propertyStringBuilder = new StringBuilder();
		String space = " ";

		for ( String property : properties )
		{
			propertyStringBuilder.append( property );
			propertyStringBuilder.append( space );
		}

		String propertyString = propertyStringBuilder.toString().trim();

		return propertyString;
	}

	private Map<Node, Boolean> getAllClasses( Workspace workspace )
	{
		final OntologyManager ontMgr = workspace.getOntologyManager();
		HashMap<String, Label> allClasses = ontMgr.getClasses();

		logger.info( "Got " + allClasses.size() + " classes from OntologyManager" );

		Set<Label> nodeLabels = new HashSet<>();
		nodeLabels.addAll( allClasses.values() );
		return getNodesUsingAlignment( workspace, nodeLabels );
	}

	private Map<Node, Boolean> getClassesInModel( Workspace workspace )
	{
		final Alignment alignment = AlignmentManager.Instance().getAlignment(
				workspace.getId(), worksheetId );
		Map<Node, Boolean> nodeSet = new HashMap<Node, Boolean>();
		Set<Node> treeNodes = alignment.getSteinerTree().vertexSet();
		if ( treeNodes != null )
		{
			for ( Node n : treeNodes )
			{
				nodeSet.put( n, true );
			}
		}
		return nodeSet;
	}

	private Map<Node, Boolean> getClassesWithProperty( Workspace workspace, String propertyURI )
	{
		final OntologyManager ontMgr = workspace.getOntologyManager();
		final HashSet<String> domains = ontMgr.getDomainsOfProperty(
				propertyURI, true );
		if ( domains == null || domains.size() == 0 )
		{
			return null;
		}

		Set<Label> nodeLabels = new HashSet<>();
		for ( String domain : domains )
		{
			Label domainURI = ontMgr.getUriLabel( domain );
			if ( domainURI == null )
				continue;
			nodeLabels.add( domainURI );
		}

		return getNodesUsingAlignment( workspace, nodeLabels );
	}

	private Map<Node, Boolean> getNodesUsingAlignment( Workspace workspace, Set<Label> nodeLabels )
	{
		Map<Node, Boolean> nodeSet = new HashMap<Node, Boolean>();

		final Alignment alignment = AlignmentManager.Instance().getAlignment(
				workspace.getId(), worksheetId );

		final Set<String> steinerTreeNodeIds = new HashSet<String>();

		if ( alignment != null && !alignment.isEmpty() )
		{
			for ( Node node : alignment.getSteinerTree().vertexSet() )
			{
				if ( node.getType() == NodeType.InternalNode )
				{
					steinerTreeNodeIds.add( node.getId() );
				}
			}
		}
		for ( Label nodeLabel : nodeLabels )
		{
			String nodeUri = nodeLabel.getUri();

			int graphLastIndex = -1;
			if ( alignment != null )
			{
				graphLastIndex = alignment.getLastIndexOfNodeUri( nodeUri );
			}
			String nodeId;
			// If the node exists in graph but not in tree then use the graph node id
			if ( graphLastIndex != -1 )
			{
				int i = 1;
				for (; i <= graphLastIndex && steinerTreeNodeIds.contains( nodeUri + i ); i++ )
					;
				nodeId = nodeUri + i;
			}
			else
			{
				nodeId = nodeUri + "1";
			}

			boolean alreadyInTheModel = false;
			if ( steinerTreeNodeIds.contains( nodeId ) )
				alreadyInTheModel = true;
			InternalNode node = new InternalNode( nodeId, nodeLabel );
			nodeSet.put( node, alreadyInTheModel );

			// Populate the graph nodes also
			if ( alignment != null )
			{
				Set<Node> graphNodes = alignment.getNodesByUri( nodeUri );
				if ( graphNodes != null && graphNodes.size() != 0 )
				{
					for ( Node graphNode : graphNodes )
					{
						if ( steinerTreeNodeIds.contains( graphNode.getId() ) )
						{
							nodeSet.put( graphNode, true );
						}
					}
				}
			}

		}

		return nodeSet;
	}

	@Override
	public UpdateContainer undoIt( Workspace workspace )
	{
		return null;
	}

}
