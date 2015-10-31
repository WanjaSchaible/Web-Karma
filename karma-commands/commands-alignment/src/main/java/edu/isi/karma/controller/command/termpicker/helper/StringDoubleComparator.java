package edu.isi.karma.controller.command.termpicker.helper;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * Created by Wanja on 31.10.15.
 */
public class StringDoubleComparator implements Comparator
{
	TreeMap<String, Double> base;

	public StringDoubleComparator( TreeMap<String, Double> map )
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
