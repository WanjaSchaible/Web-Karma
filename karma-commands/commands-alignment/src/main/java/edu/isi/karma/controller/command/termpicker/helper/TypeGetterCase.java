package edu.isi.karma.controller.command.termpicker.helper;

/**
 * Created by Wanja on 15.11.15.
 */
public class TypeGetterCase
{
	public static int getCase( String incomingPropertyString, String outgoingPropertyString )
	{
		/*
		 0, if no one of these is filled with any properties
		 1, if there are only incoming properties
		 2, if there are only outgoing properties
		 3, if both contain properties
		  */

		boolean incomingEmpty;
		if ( incomingPropertyString == null || incomingPropertyString.equals( "" ) )
		{
			incomingEmpty = true;
		}
		else
		{
			incomingEmpty = false;
		}

		boolean outgoingEmpty;
		if ( outgoingPropertyString == null || outgoingPropertyString.equals( "" ) )
		{
			outgoingEmpty = true;
		}
		else
		{
			outgoingEmpty = false;
		}

		if ( !incomingEmpty && !outgoingEmpty )
		{
			return 3;
		}
		else if ( incomingEmpty && !outgoingEmpty )
		{
			return 2;
		}
		else if ( !incomingEmpty && outgoingEmpty )
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
}
