package edu.isi.karma.controller.command.termpicker;

import org.junit.Test;

/**
 * Created by Wanja on 29.10.15.
 */
public class TermPickerRecommendationForNodesTest
{
	String node = "http://xmlns.com/foaf/0.1/Person";
	String incomingProperty = "http://xmlns.com/foaf/0.1/knows";
	String outgoingProperty = "http://xmlns.com/foaf/0.1/name";

	@Test
	public void testName() throws Exception
	{
		TermPickerRecommendationForNodes termPickerRecommendationForNodes = new TermPickerRecommendationForNodes( node, incomingProperty,
				outgoingProperty );

	}
}
