package edu.isi.karma.controller.command.termpicker;

import edu.isi.karma.controller.command.termpicker.helper.StringDoubleComparator;
import edu.isi.karma.controller.command.termpicker.helper.StringIntegerComparator;
import edu.isi.karma.controller.command.termpicker.helper.TypeGetterCase;
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
		int whichCase = TypeGetterCase.getCase( incomingPropertyString, outgoingPropertyString );

		if ( whichCase == 1 ) // only incoming properties
		{
			TermPickerRecommendations termPickerRecsInc = new TermPickerRecommendations( "", incomingPropertyString, "" );
			JSONObject incomingPropertyReturnObject = new JSONObject( termPickerRecsInc.getJsonObjectRecommendation() );
			HashMap<String, Integer> types = fillPropertyTypeMap( incomingPropertyReturnObject, "listOfOtsRecommendationsFeatures" );
			List<String> rankedTypes = rankMapByValueInteger( types );
			for ( String rdfType : rankedTypes )
			{
				this.termSlpUsageMap.put( rdfType, (double) types.get( rdfType ) );
				this.rdfTypeRecommendations.add( rdfType );
			}

		}
		else if ( whichCase == 2 ) // only outgoing
		{
			TermPickerRecommendations termPickerRecsOutgoing = new TermPickerRecommendations( "", outgoingPropertyString, "" );
			JSONObject outgoingPropertyReturnObject = new JSONObject( termPickerRecsOutgoing.getJsonObjectRecommendation() );
			HashMap<String, Integer> types = fillPropertyTypeMap( outgoingPropertyReturnObject, "listOfStsRecommendationFeatures" );
			List<String> rankedTypes = rankMapByValueInteger( types );
			for ( String rdfType : rankedTypes )
			{
				this.termSlpUsageMap.put( rdfType, (double) types.get( rdfType ) );
				this.rdfTypeRecommendations.add( rdfType );
			}
		}
		else if ( whichCase == 3 ) // both incoming and outgoing
		{
			TermPickerRecommendations termPickerRecsInc = new TermPickerRecommendations( "", incomingPropertyString, "" );
			JSONObject incomingPropertyReturnObject = new JSONObject( termPickerRecsInc.getJsonObjectRecommendation() );
			HashMap<String, Integer> incomingPropertyTypes = fillPropertyTypeMap( incomingPropertyReturnObject,
					"listOfOtsRecommendationsFeatures" );

			TermPickerRecommendations termPickerRecsOutg = new TermPickerRecommendations( "", outgoingPropertyString, "" );
			JSONObject outgoingPropertyReturnObject = new JSONObject( termPickerRecsOutg.getJsonObjectRecommendation() );
			HashMap<String, Integer> outgoingPropertyTypes = fillPropertyTypeMap( outgoingPropertyReturnObject,
					"listOfStsRecommendationFeatures" );

			this.termSlpUsageMap = getCommonTermInRankedOrder( incomingPropertyTypes, outgoingPropertyTypes );
			this.rdfTypeRecommendations = reRankCommonRdfTypes( termSlpUsageMap );
		}
		else
		{
			//TODO: implement
		}

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

	private List<String> rankMapByValueInteger( HashMap<String, Integer> unsortedTypes )
	{
		List<String> reRankedList = new ArrayList<>();
		TreeMap<String, Integer> treeMapOfTerms = new TreeMap<>();
		treeMapOfTerms.putAll( unsortedTypes );

		StringIntegerComparator bvc = new StringIntegerComparator( treeMapOfTerms );
		TreeMap<String, Integer> reRankedMap = new TreeMap<String, Integer>( bvc );
		reRankedMap.putAll( unsortedTypes );

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
