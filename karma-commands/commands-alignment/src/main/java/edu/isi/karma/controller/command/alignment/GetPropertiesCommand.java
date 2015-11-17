package edu.isi.karma.controller.command.alignment;

import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.command.CommandType;
import edu.isi.karma.controller.command.WorksheetCommand;
import edu.isi.karma.controller.command.termpicker.TermPickerRecommendations;
import edu.isi.karma.controller.update.AbstractUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.modeling.alignment.Alignment;
import edu.isi.karma.modeling.alignment.AlignmentManager;
import edu.isi.karma.modeling.ontology.OntologyManager;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.rep.alignment.*;
import edu.isi.karma.view.VWorkspace;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class GetPropertiesCommand extends WorksheetCommand
{

	final private INTERNAL_PROP_RANGE propertiesRange;

	public enum INTERNAL_PROP_RANGE
	{
		allDataProperties, allObjectProperties, allDataAndObjectProperties, existingProperties, dataPropertiesForClass, propertiesWithDomainRange
	}

	private enum JsonKeys
	{
		updateType, label, id, properties, uri, type
	}

	private String classURI, domainURI, rangeURI;

	private static Logger logger = LoggerFactory.getLogger( GetPropertiesCommand.class.getSimpleName() );

	protected GetPropertiesCommand(
			String id,
			String model,
			String worksheetId,
			INTERNAL_PROP_RANGE propertiesRange,
			String classURI,
			String domainURI,
			String rangeURI )
	{
		super( id, model, worksheetId );
		this.propertiesRange = propertiesRange;
		this.classURI = classURI;
		this.domainURI = domainURI;
		this.rangeURI = rangeURI;
	}

	@Override
	public String getCommandName()
	{
		return GetClassesCommand.class.getSimpleName();
	}

	@Override
	public String getTitle()
	{
		return "Get Properties";
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
		final OntologyManager ontMgr = workspace.getOntologyManager();
		final List<LabeledLink> properties = new ArrayList<>();

		logger.debug( "GetPropertiesCommand:" + propertiesRange + ":" + classURI + "," + domainURI + ", " + rangeURI );

		if ( propertiesRange == INTERNAL_PROP_RANGE.allObjectProperties )
		{
			HashMap<String, Label> linkList = ontMgr.getObjectProperties();
			if ( linkList != null )
			{
				for ( Label label : linkList.values() )
				{
					properties.add( new ObjectPropertyLink( label.getUri(), label, ObjectPropertyType.None ) );
				}
			}
		}
		else if ( propertiesRange == INTERNAL_PROP_RANGE.allDataProperties )
		{
			HashMap<String, Label> linkList = ontMgr.getDataProperties();
			for ( Label label : linkList.values() )
			{
				properties.add( new DataPropertyLink( label.getUri(), label ) );
			}
		}
		else if ( propertiesRange == INTERNAL_PROP_RANGE.allDataAndObjectProperties )
		{
			HashMap<String, Label> linkList = ontMgr.getDataProperties();
			for ( Label label : linkList.values() )
			{
				properties.add( new DataPropertyLink( label.getUri(), label ) );
			}
			HashMap<String, Label> objectLinkList = ontMgr.getObjectProperties();
			for ( Label label : objectLinkList.values() )
			{
				if ( !linkList.containsValue( label ) )
					properties.add( new ObjectPropertyLink( label.getUri(), label, ObjectPropertyType.None ) );
			}
		}
		else if ( propertiesRange == INTERNAL_PROP_RANGE.propertiesWithDomainRange )
		{
			//TODO: If no recommendations mode, then let domain and range Recommendations take place
			//Map<String, Label> linkList = ontMgr.getObjectPropertiesByDomainRange( domainURI, rangeURI, true );
			Map<String, Label> linkList = new HashMap<>();
			for ( Label label : linkList.values() )
			{
				properties.add( new DataPropertyLink( label.getUri(), label ) );
			}
		}
		else if ( propertiesRange == INTERNAL_PROP_RANGE.dataPropertiesForClass )
		{
			Map<String, Label> linkList = ontMgr.getDataPropertiesByDomain( classURI, true );
			for ( Label label : linkList.values() )
			{
				properties.add( new DataPropertyLink( label.getUri(), label ) );
			}
		}
		else if ( propertiesRange == INTERNAL_PROP_RANGE.existingProperties )
		{
			Alignment alignment = AlignmentManager.Instance().getAlignment( workspace.getId(), worksheetId );
			Set<String> steinerTreeNodeIds = new HashSet<String>();
			if ( alignment != null && !alignment.isEmpty() )
			{
				DirectedWeightedMultigraph<Node, LabeledLink> steinerTree = alignment.getSteinerTree();
				for ( Node node : steinerTree.vertexSet() )
				{
					if ( node.getType() == NodeType.InternalNode )
					{
						steinerTreeNodeIds.add( node.getId() );
					}
				}

				List<LabeledLink> specializedLinks = new ArrayList<LabeledLink>();
				Set<LabeledLink> temp = null;
				temp = alignment.getLinksByType( LinkType.DataPropertyLink );
				if ( temp != null )
					specializedLinks.addAll( temp );
				for ( LabeledLink link : steinerTree.edgeSet() )
					if ( link instanceof ObjectPropertyLink )
						specializedLinks.add( link );

				// Store the data property links for specialized edge link options
				properties.addAll( specializedLinks );
			}
		}

		logger.debug( "Got back " + properties.size() + " results" );
		final List<LabeledLink> finalProperties = properties;

		UpdateContainer upd = new UpdateContainer( new AbstractUpdate()
		{
			@Override
			public void generateJson(
					String prefix, PrintWriter pw,
					VWorkspace vWorkspace )
			{
				JSONObject obj = new JSONObject();
				JSONArray resultArray = new JSONArray();

				if ( properties.size() == 0 )
				{
					// Get recommendations from TermPicker ==== BEGIN
					final TermPickerRecommendations termPickerRecommendations = new TermPickerRecommendations( domainURI, rangeURI );
					try
					{
						termPickerRecommendations.getTermPickerRecommendations();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					// Get recommendations from TermPicker ==== END

					for ( String psRecommendation : termPickerRecommendations.getPsRecommendations() )
					{
						Label recommendedPropertyLabel = ontMgr.getUriLabel( psRecommendation );
						if ( recommendedPropertyLabel != null )
						{
							finalProperties.add( new ObjectPropertyLink( recommendedPropertyLabel.getUri(), recommendedPropertyLabel,
									ObjectPropertyType.None ) );
						}
					}
				}

				try
				{
					obj.put( JsonKeys.updateType.name(), "PropertyList" );

					for ( LabeledLink link : finalProperties )
					{
						Label linkLabel = link.getLabel();
						String edgeLabelStr = linkLabel.getDisplayName();
						JSONObject edgeObj = new JSONObject();
						if ( linkLabel.getUri() != null && linkLabel.getNs() != null
								&& linkLabel.getUri().equalsIgnoreCase( linkLabel.getNs() ) )
						{
							edgeLabelStr = linkLabel.getUri();
						}

						edgeObj.put( JsonKeys.label.name(), edgeLabelStr );
						edgeObj.put( JsonKeys.uri.name(), linkLabel.getUri() );
						edgeObj.put( JsonKeys.id.name(), link.getId() );

						if ( link instanceof ObjectPropertyLink )
							edgeObj.put( JsonKeys.type.name(), "objectProperty" );
						else
							edgeObj.put( JsonKeys.type.name(), "dataProperty" );

						resultArray.put( edgeObj );
					}

					obj.put( JsonKeys.properties.name(), resultArray );
					pw.println( obj.toString() );
				}
				catch (Exception e)
				{
					logger.error( "Exception:", e );
					e.printStackTrace();
				}
			}
		} );
		return upd;
	}

	@Override
	public UpdateContainer undoIt( Workspace workspace )
	{
		// TODO Auto-generated method stub
		return null;
	}

}
