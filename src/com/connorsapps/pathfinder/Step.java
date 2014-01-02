package com.connorsapps.pathfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 * One step of a route to a destination. Includes time, distance, and map overlay
 * info, all where applicable.
 * @author Connor Cormier
 *
 */
public class Step
{
	public static final String ADDRESS_DELIMITER = "\\\\\\\\TO////";
	//TODO adjust epsilon
	public static final double EPSILON = 1.0 / 111000;
	private final LatLng start, end;
	private final int meters;
	private final long duration;
	private final String description;
	private final String polyLine;
	private final boolean isOverview;
	
	/**
	 * Construct an immuatble step object to hold information about a piece of the route, does NOT have a polyLine associated with it.
	 * @param start
	 * @param end
	 * @param meters
	 * @param duration
	 * @param description
	 */
	public Step(LatLng start, LatLng end, int meters, long duration, String description, boolean isOverview)
	{
		this.start = start;
		this.end = end;
		this.meters = meters;
		this.duration = duration;
		this.description = description;
		this.polyLine = null;
		this.isOverview = isOverview;
	}
	

	
	/**
	 * Create an immuatble step object to hold information about this leg of the route. Does have a polyLine associated with it.
	 * @param start
	 * @param end
	 * @param meters
	 * @param duration
	 * @param description
	 * @param polyLine
	 */
	public Step(LatLng start, LatLng end, int meters, long duration, String description, String polyLine, boolean isOverview)
	{
		this.start = start;
		this.end = end;
		this.meters = meters;
		this.duration = duration;
		this.description = description;
		this.polyLine = polyLine;
		this.isOverview = isOverview;
	}
	
	
	public LatLng getStart()
	{
		return start;
	}


	public LatLng getEnd()
	{
		return end;
	}


	public int getMeters()
	{
		return meters;
	}


	public long getDuration()
	{
		return duration;
	}
	
	public String getDescription()
	{
		return description;
	}

	public boolean isOverview()
	{
		return isOverview;
	}
	
	public String toString()
	{
		return description + "\n\t" + duration + " milliseconds\n\t" + meters + " meters";
	}

	public String getPolyLine()
	{
		return polyLine;
	}
	
	/**
	 * If this is an overview with a properly formatted description, get the start address
	 * @return
	 */
	public String getStartAddress()
	{
		if (!this.isOverview() || !getDescription().contains(ADDRESS_DELIMITER))
			throw new RuntimeException("Not an overview");

		return getDescription().split(ADDRESS_DELIMITER)[0].trim();
	}
	
	/**
	 * If this is an overview with a properly formatted description, get the end address
	 * @return
	 */
	public String getEndAddress()
	{
		if (!this.isOverview() || !getDescription().contains(ADDRESS_DELIMITER))
			throw new RuntimeException("Not an overview");

		return getDescription().split(ADDRESS_DELIMITER)[1].trim();
	}
}
