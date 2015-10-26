package edu.isi.karma.controller.command.termpicker;

import org.junit.Test;

/**
 * Created by Wanja on 22.10.15.
 */
public class TermPickerRecommendationTest
{
	String classUri = "http://xmlns.com/foaf/0.1/Person";

	@Test
	public void testTermPickerRecommendationsProperty() throws Exception
	{
		TermPickerRecommendations termPickerRecommendations = new TermPickerRecommendations( classUri );
		termPickerRecommendations.getPsRecommendations();


	}
}
