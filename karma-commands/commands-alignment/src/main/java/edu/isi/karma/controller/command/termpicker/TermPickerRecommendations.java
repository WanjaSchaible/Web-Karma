package edu.isi.karma.controller.command.termpicker;

import edu.isi.karma.controller.update.SemanticTypesUpdate;
import edu.isi.karma.modeling.ontology.OntologyManager;
import edu.isi.karma.rep.alignment.Label;
import edu.isi.karma.rep.alignment.LabeledLink;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wanja on 16.10.15.
 */
public class TermPickerRecommendations
{
	String classURI;

	String sbjType;
	String property;
	String objType;
	String recMethod;

	List<String> stsRecommendations = new ArrayList<String>();
	List<String> psRecommendations = new ArrayList<String>();
	List<String> otsRecommendations = new ArrayList<String>();

	public TermPickerRecommendations( String classUri )
	{
		this.sbjType = classUri;
		this.classURI = classUri;
		try
		{
			getTermPickerRecommendations();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public TermPickerRecommendations( String domainURI, String rangeURI )
	{
		sbjType = domainURI;
		objType = rangeURI;
	}

	//TODO: refactor so it will also provide recommendations for RDF types and properties between RDF types
	public void getTermPickerRecommendations() throws IOException
	{
		StringBuilder urlBuilder = new StringBuilder();
		String parameterStart = "?";
		String parameterJoin = "&";

		urlBuilder.append( "http://kdsrv01.informatik.uni-kiel.de:8081/pickerapi/recommend" );
		urlBuilder.append( parameterStart );

		List<String> arguments = new ArrayList<String>();

		if ( sbjType != null && !sbjType.equals( "" ) )
		{
			arguments.add( "sbjType=" + sbjType );
		}
		if ( objType != null && !objType.equals( "" ) )
		{
			arguments.add( "objType=" + objType );
		}
		if ( property != null && !property.equals( "" ) )
		{
			arguments.add( "prop=" + property );
		}
		if ( recMethod != null )
		{
			arguments.add( "recMethod" + recMethod );
		}

		for ( String argument : arguments )
		{
			urlBuilder.append( argument );
			if ( arguments.indexOf( argument ) != arguments.size() - 1 )
			{
				urlBuilder.append( parameterJoin );
			}
		}

		InputStream is = new URL( urlBuilder.toString() ).openStream();
		try
		{
			BufferedReader rd = new BufferedReader( new InputStreamReader( is, Charset.forName( "UTF-8" ) ) );
			String jsonText = readAll( rd );
			if ( !jsonText.equals( "" ) )
			{
				JSONObject json = new JSONObject( jsonText );

				JSONArray arrOfProps = json.getJSONArray( "listOfPsRecommendations" );
				if ( arrOfProps != null )
				{
					int len = arrOfProps.length();
					for ( int i = 0; i < len; i++ )
					{
						psRecommendations.add( arrOfProps.get( i ).toString() );
					}
				}

				JSONArray arrOfSbjTypes = json.getJSONArray( "listOfStsRecommendations" );
				if ( arrOfSbjTypes != null )
				{
					int len = arrOfSbjTypes.length();
					for ( int i = 0; i < len; i++ )
					{
						stsRecommendations.add( arrOfSbjTypes.get( i ).toString() );
					}
				}

				JSONArray arrOfObjTypes = json.getJSONArray( "listOfOtsRecommendations" );
				if ( arrOfObjTypes != null )
				{
					int len = arrOfObjTypes.length();
					for ( int i = 0; i < len; i++ )
					{
						otsRecommendations.add( arrOfObjTypes.get( i ).toString() );
					}
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			is.close();
		}
	}

	public JSONObject appendTermPickerRecommendations( OntologyManager ontMgr, int maxLabels )
	{
		JSONObject obj = new JSONObject();
		JSONArray arr = new JSONArray();

		Label domainURI = ontMgr.getUriLabel( classURI );

		for ( String psRecommendation : psRecommendations )
		{
			Label typeURI = ontMgr.getUriLabel( psRecommendation );

			double probability = 0.991234;
			String clazzLocalNameWithPrefix = domainURI.getDisplayName(); //TODO: move outside the for loop

			if ( domainURI.getPrefix() == null )
			{
				clazzLocalNameWithPrefix = domainURI.getUri() + "/" + domainURI.getLocalName();
			}

			if ( typeURI != null )
			{
				insertSemanticTypeSuggestion( arr, clazzLocalNameWithPrefix, this.classURI, this.classURI,
						typeURI.getDisplayName(), psRecommendation, probability );
			}

			if ( arr.length() == maxLabels )
			{
				break;
			}

		}

		obj.put( "Labels", arr );
		return obj;
	}

	private void insertSemanticTypeSuggestion(
			JSONArray arr, String domainDisplayLabel,
			String domainUri, String domainId, String typeDisplayLabel, String type, double probability ) throws JSONException
	{
		JSONObject typeObj = new JSONObject();
		typeObj.put( SemanticTypesUpdate.JsonKeys.DisplayDomainLabel.name(), domainDisplayLabel );
		typeObj.put( SemanticTypesUpdate.JsonKeys.DomainUri.name(), domainUri );
		typeObj.put( SemanticTypesUpdate.JsonKeys.DomainId.name(), domainId );
		typeObj.put( SemanticTypesUpdate.JsonKeys.DisplayLabel.name(), typeDisplayLabel );
		typeObj.put( SemanticTypesUpdate.JsonKeys.FullType.name(), type );
		typeObj.put( "Probability", probability );
		arr.put( typeObj );
	}

	private static String readAll( Reader rd ) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1)
		{
			sb.append( (char) cp );
		}
		return sb.toString();
	}

	public String getClassURI()
	{
		return classURI;
	}

	public String getSbjType()
	{
		return sbjType;
	}

	public String getProperty()
	{
		return property;
	}

	public String getObjType()
	{
		return objType;
	}

	public String getRecMethod()
	{
		return recMethod;
	}

	public List<String> getStsRecommendations()
	{
		return stsRecommendations;
	}

	public List<String> getPsRecommendations()
	{
		return psRecommendations;
	}

	public List<String> getOtsRecommendations()
	{
		return otsRecommendations;
	}

	public LabeledLink getLabeledProperty( String psRecommendation )
	{
		LabeledLink labeledLink = null;
		return labeledLink;
	}
}
