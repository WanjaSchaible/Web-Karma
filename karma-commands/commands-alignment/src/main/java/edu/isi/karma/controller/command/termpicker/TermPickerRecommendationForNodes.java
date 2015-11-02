package edu.isi.karma.controller.command.termpicker;

import edu.isi.karma.controller.command.termpicker.helper.StringDoubleComparator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by Wanja on 29.10.15.
 */
public class TermPickerRecommendationForNodes
{
	String rdfType;
	String incomingPropertyString;
	String outgoingPropertyString;

	List<String> rdfTypeRecommendations = new ArrayList<>();

	HashMap<String, Double> termRankScoreMap = new HashMap<>();
	HashMap<String, Double> termSlpUsageMap = new HashMap<>();

	public TermPickerRecommendationForNodes( String rdfType, String incomingPropertyString, String outgoingPropertyString )
	{
		this.rdfType = rdfType;
		this.incomingPropertyString = incomingPropertyString;
		this.outgoingPropertyString = outgoingPropertyString;

		performRecommendationProcess();
	}

	private void performRecommendationProcess()
	{
		TermPickerRecommendations termPickerRecsInc = new TermPickerRecommendations( "", incomingPropertyString, rdfType );
		TermPickerRecommendations termPickerRecsOutg = new TermPickerRecommendations( rdfType, outgoingPropertyString, "" );

		JSONObject incomingPropertyReturnObject = new JSONObject( termPickerRecsInc.getJsonObjectRecommendation() );
		JSONObject outgoingPropertyReturnObject = new JSONObject( termPickerRecsOutg.getJsonObjectRecommendation() );

		//TODO: do it for Learning To Rank Also
		HashMap<String, Integer> incomingPropertyTypes = fillPropertyTypeMap( incomingPropertyReturnObject,
				"listOfOtsRecommendationsFeatures" );
		HashMap<String, Integer> outgoingPropertyTypes = fillPropertyTypeMap( outgoingPropertyReturnObject,
				"listOfStsRecommendationFeatures" );
		this.termSlpUsageMap = getCommonTermInRankedOrder( incomingPropertyTypes, outgoingPropertyTypes );
		this.rdfTypeRecommendations = reRankCommonRdfTypes( termSlpUsageMap );
	}

	private HashMap<String, Integer> fillPropertyTypeMap( JSONObject propertyReturnObject, String listOfFeatures )
	{
		HashMap<String, Integer> propertyTypes = new HashMap<>();

		JSONArray arrOfRdfTypes = propertyReturnObject.getJSONArray( listOfFeatures );
		if ( arrOfRdfTypes != null )
		{
			for ( int i = 0; i < arrOfRdfTypes.length(); i++ )
			{
				JSONObject object = arrOfRdfTypes.getJSONObject( i );
				String term = object.getString( "term" );
				int slpCount = object.getInt( "slps" );
				propertyTypes.put( term, slpCount );
			}
		}

		return propertyTypes;
	}

	private HashMap<String, Double> getCommonTermInRankedOrder(
			HashMap<String, Integer> incomingPropertyTypes,
			HashMap<String, Integer> outgoingPropertyTypes )
	{
		HashMap<String, Double> termSlpUsageMap = new HashMap<>();

		for ( Map.Entry<String, Integer> incomingEntry : incomingPropertyTypes.entrySet() )
		{
			if ( outgoingPropertyTypes.containsKey( incomingEntry.getKey() ) )
			{
				double newSlpCnt = ((double) incomingEntry.getValue() + outgoingPropertyTypes.get( incomingEntry.getKey() )) / 2;
				termSlpUsageMap.put( incomingEntry.getKey(), newSlpCnt );
			}
		}

		return termSlpUsageMap;
	}

	private List<String> reRankCommonRdfTypes( HashMap<String, Double> termSlpUsageMap )
	{
		List<String> reRankedList = new ArrayList<>();
		TreeMap<String, Double> treeMapOfTerms = new TreeMap<>();
		treeMapOfTerms.putAll( termSlpUsageMap );

		StringDoubleComparator bvc = new StringDoubleComparator( treeMapOfTerms );
		TreeMap<String, Double> reRankedMap = new TreeMap<String, Double>( bvc );
		reRankedMap.putAll( termSlpUsageMap );

		reRankedList.addAll( reRankedMap.keySet() );

		return reRankedList;
	}

	public String getRdfType()
	{
		return rdfType;
	}

	public String getIncomingPropertyString()
	{
		return incomingPropertyString;
	}

	public String getOutgoingPropertyString()
	{
		return outgoingPropertyString;
	}

	public List<String> getRdfTypeRecommendations()
	{
		return rdfTypeRecommendations;
	}

	public HashMap<String, Double> getTermRankScoreMap()
	{
		return termRankScoreMap;
	}

	public HashMap<String, Double> getTermSlpUsageMap()
	{
		return termSlpUsageMap;
	}
}
