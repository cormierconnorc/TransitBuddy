package com.connorsapps.pathfinder;

import java.util.Comparator;

import com.google.android.gms.maps.model.LatLng;

/**
 * Class to sort by distance to a reference point, smallest first
 * @author Connor Cormier
 *
 */
public class StopDistanceComparator implements Comparator<Stop>
{
	private final LatLng reference;
	
	/**
	 * @param reference Stop to sort the other stops with respect to.
	 */
	public StopDistanceComparator(LatLng reference)
	{
		this.reference = reference;
	}
	
	@Override
	public int compare(Stop a, Stop b)
	{
		Double distanceToA = distanceBetween(a.getLocation(), reference);
		Double distanceToB = distanceBetween(b.getLocation(), reference);
		//Sort by distance to reference, smallest first
		return distanceToA.compareTo(distanceToB);
	}
	
	private double distanceBetween(LatLng a, LatLng b)
	{
		return Math.sqrt(Math.pow(a.latitude - b.latitude, 2) + Math.pow(a.longitude - b.longitude, 2));
	}

}
