package edu.isi.karma.controller.command.termpicker.helper;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * Created by Wanja on 16.11.15.
 */
public class StringIntegerComparator implements Comparator
{
	TreeMap<String, Integer> base;

	public StringIntegerComparator( TreeMap<String, Integer> map )
	{
		this.base = map;
	}

	@Override public int compare( Object o1, Object o2 )
	{
		if ( base.get( o1 ) >= base.get( o2 ) )
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}
}
