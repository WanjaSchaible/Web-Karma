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
	String recMethod; //ar for no Recs, "l2r" for l2r, and nothing for Association Rules

	//TODO: make each instance go on a different port: 8080 - L2R, 8081 - AR, 8082, No Recs

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

	public TermPickerRecommendations( String sbjType, String property, String objType )
	{
		this.sbjType = sbjType;
		this.property = property;
		this.objType = objType;
	}

	public void getTermPickerRecommendations() throws IOException
	{
		String jsonText = getJsonObjectRecommendation();

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

	private String encodeUri( String vocabTerms )
	{
		String encodedUri = vocabTerms.replace( " ", "%20" );
		encodedUri = encodedUri.replace( "#", "%23" );
		return encodedUri;
	}

	public JSONObject appendTermPickerRecommendations( OntologyManager ontMgr, int maxLabels )
	{
		JSONObject obj = new JSONObject();
		JSONArray arr = new JSONArray();

		Label domainURI = ontMgr.getUriLabel( classURI );

		String clazzLocalNameWithPrefix = domainURI.getDisplayName();
		for ( String psRecommendation : psRecommendations )
		{
			Label typeURI = ontMgr.getUriLabel( psRecommendation );

			double probability = 0.991234;

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

	public String getJsonObjectRecommendation()
	{
		StringBuilder urlBuilder = new StringBuilder();
		String parameterStart = "?";
		String parameterJoin = "&";

		urlBuilder.append( "http://kdsrv01.informatik.uni-kiel.de:8080/pickerapi/recommend" );
		urlBuilder.append( parameterStart );

		List<String> arguments = new ArrayList<String>();

		if ( sbjType != null && !sbjType.equals( "" ) )
		{
			arguments.add( "sbjType=" + encodeUri( sbjType ) );
		}
		if ( objType != null && !objType.equals( "" ) )
		{
			arguments.add( "objType=" + encodeUri( objType ) );
		}
		if ( property != null && !property.equals( "" ) )
		{
			arguments.add( "prop=" + encodeUri( property ) );
		}
		if ( recMethod != null )
		{
			arguments.add( "recMethod=" + recMethod );
		}

		for ( String argument : arguments )
		{
			urlBuilder.append( argument );
			if ( arguments.indexOf( argument ) != arguments.size() - 1 )
			{
				urlBuilder.append( parameterJoin );
			}
		}

		String jsonText = null;
		try
		{
			InputStream is = new URL( urlBuilder.toString() ).openStream();
			BufferedReader rd = new BufferedReader( new InputStreamReader( is, Charset.forName( "UTF-8" ) ) );
			jsonText = readAll( rd );

			is.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return jsonText;
	}
}
