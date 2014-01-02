package com.connorsapps.pathfinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;

public class BusStep extends Step
{
	private final List<List<LatLng>> busSegs;
	private final List<Arrival> busArrivals;
	private final Map<Long, Stop> stopMap;
	private final Arrival first, last;
	private List<LatLng> cachedBusRoute;
	
	/**
	 * Construct an immuatble step object to hold information about a piece of the route, includes a bus route.
	 * @param start
	 * @param end
	 * @param meters
	 * @param duration
	 * @param description
	 */
	public BusStep(LatLng start, LatLng end, int meters, long duration, String description, String polyLine, boolean isOverview, List<List<LatLng>> busRoute, 
			List<Arrival> busArrivals, Map<Long, Stop> stopMap, Arrival first, Arrival last)
	{
		super(start, end, meters, duration, description, polyLine, isOverview);
		this.busSegs = busRoute;
		this.busArrivals = busArrivals;
		this.stopMap = stopMap;
		this.first = first;
		this.last = last;
	}
	
	public List<List<LatLng>> getBusSegs()
	{
		return busSegs;
	}
	
	/**
	 * Get the segments used on this bus route (Note: does not include segments with no arrivals)
	 * @return
	 */
	public List<List<LatLng>> getBusRouteSegs()
	{
		List<List<LatLng>> masterList = new ArrayList<List<LatLng>>();
		
		//Check if the two points fall on the same segment, return that segment if they do.
		SegSection start = this.getClosestSegment(this.getStart()), end = this.getClosestSegment(this.getEnd());
		
		if (start.getSegPos() == end.getSegPos())
		{
			masterList.add(this.buildSegment(start.getSegPos(), start.getPointPos(), end.getPointPos()));
			return masterList;
		}
		
		//Segments containing start and end points respectively
		List<LatLng> startSeg = this.buildSegment(start.getSegPos(), start.getPointPos(), getBusSegs().get(start.getSegPos()).size()),
				endSeg = this.buildSegment(end.getSegPos(), 0, end.getPointPos() + 1);
		
		if (startSeg == null || endSeg == null)
		{
			Log.d("debug", "A segment was null");
			return masterList;
		}
		
		//Add in the segments that have already been verified
		Set<Integer> usedSegs = new HashSet<Integer>();
		usedSegs.add(start.getSegPos());
		usedSegs.add(end.getSegPos());
		
		masterList.add(startSeg);
		masterList.add(endSeg);
		
		tracePath(masterList, usedSegs);
		
		return masterList;
	}
	
	/**
	 * Find the segments that lie along this path and add them to the master list
	 * @param segList A list to hold segments. Include custom start and end segments if applicable
	 * @param usedSegs A set to hold segment indices that have already been used. Use to hold start and end indices if applicable
	 */
	public void tracePath(List<List<LatLng>> segList, Set<Integer> usedSegs)
	{
		//Isolate only the relevant arrivals, then add the nearest segments to the list
		for (Arrival a : busArrivals)
			if (a.getVehicleId() == first.getVehicleId() && a.getTimestamp() >= first.getTimestamp() && a.getTimestamp() <= last.getTimestamp())
			{
				SegSection s = this.getClosestSegment(this.stopMap.get(a.getStopId()).getLocation());
				
				if (!usedSegs.contains(s.getSegPos()))
				{
					segList.add(busSegs.get(s.getSegPos()));
					usedSegs.add(s.getSegPos());
				}
			}
	}
	
	/**
	 * Using the segments this bus travels along, build the path
	 * @return
	 */
	public List<LatLng> getBusRoute()
	{
		//Do not recalculate unnecessarily, use cached copy
		if (cachedBusRoute != null)
			return this.cachedBusRoute;
		
		//Check if the two points fall on the same segment, return that segment if they do.
		SegSection start = this.getClosestSegment(this.getStart()), end = this.getClosestSegment(this.getEnd());
		
		if (start.getSegPos() == end.getSegPos())
			return this.buildSegment(start.getSegPos(), start.getPointPos(), end.getPointPos());
		
		//Segments containing start and end points respectively
		List<LatLng> startSeg = this.buildSegment(start.getSegPos(), start.getPointPos(), getBusSegs().get(start.getSegPos()).size()),
				endSeg = this.buildSegment(end.getSegPos(), 0, end.getPointPos() + 1);
		
		if (startSeg == null || endSeg == null)
		{
			Log.d("debug", "A segment was null");
			cachedBusRoute = new ArrayList<LatLng>();
			return cachedBusRoute;
		}
		
		cachedBusRoute = tracePath(startSeg, endSeg, start.getSegPos());
		
		return cachedBusRoute;
	}
	
	private List<LatLng> tracePath(List<LatLng> startSeg, List<LatLng> endSeg, int startSegPos)
	{
		Set<Integer> memSet = new HashSet<Integer>();
		memSet.add(startSegPos);
		return this.tracePath(startSeg, endSeg, memSet, 0);
	}
	
	private List<LatLng> tracePath(List<LatLng> startSeg, List<LatLng> endSeg, Set<Integer> checkedSegs, int startIndex)
	{
		//FIXME someday
		List<LatLng> segPoints = new ArrayList<LatLng>();
		
		for (int k = startIndex; k < startSeg.size(); k++)
		{
			LatLng point = startSeg.get(k);
			
			Log.i("debug", "Checking point (" + point.latitude + "," + point.longitude + ") with current segment set " + checkedSegs);
			
			segPoints.add(point);
			
			//Check if this point connects to the ending segment. Add the end and return if it does.
			//for (int i = 0; i < endSeg.size(); i++)
			if (samePoint(point, endSeg.get(0)))
			{
				segPoints.addAll(endSeg);
				return segPoints;
			}
			
			//Check if you can look down any other segments from this point
			for (int i = 0; i < getBusSegs().size(); i++)
				if (!checkedSegs.contains(i))
				{					
					//Check each point in the segment up against this one.
					for (int j = 0; j < getBusSegs().get(i).size(); j++)
						if (samePoint(point, getBusSegs().get(i).get(j)))
						{
							//Note: need to include only segpoints AFTER the found point. Also, make sure orientation of segments is correct.
							
							//Add this segment to the mem map. Do not allow it to be checked again.
							checkedSegs.add(i);
							
							Log.d("debug", "Made connection!");
							List<LatLng> pathSoFar = tracePath(getBusSegs().get(i), endSeg, checkedSegs, 0);
							
							if (pathSoFar != null)
							{
								segPoints.addAll(pathSoFar);
								return segPoints;
							}
							
							break;
						}
				}
		}
		
		//No transfers found and no end reached. This is a dead end.
		return null;
	}
	
	/**
	 * Check if two points are equal within a given epsilon value
	 * @param one
	 * @param two
	 * @return
	 */
	private boolean samePoint(LatLng one, LatLng two)
	{
		double dis = Math.sqrt(Math.pow(one.latitude - two.latitude, 2) + Math.pow(one.longitude - two.longitude, 2));
		return dis <= EPSILON;
	}

	/**
	 * Get the segment closest to the given point
	 * @param point
	 * @param after
	 * @return
	 */
	private SegSection getClosestSegment(LatLng point)
	{
		int closeSeg = -1, closePoint = -1;
		double closeDistance = Double.MAX_VALUE;
		
		for (int seg = 0; seg < getBusSegs().size(); seg++)
		{
			for (int p = 0; p < getBusSegs().get(seg).size(); p++)
			{
				double dis = getDistance(point, getBusSegs().get(seg).get(p));
				
				if (dis < closeDistance)
				{
					closeSeg = seg;
					closePoint = p;
					closeDistance = dis;
				}
			}
		}
		
		return new SegSection(closeSeg, closePoint);
	}
	
	/**
	 * Returns segment at index bounded by start and end points
	 * @param segmentIndex
	 * @param start start, inclusive
	 * @param end end, exclusive
	 * @return
	 */
	private List<LatLng> buildSegment(int segmentIndex, int start, int end)
	{
		if (segmentIndex == -1 || start > end)
			return null;
		
		//Build the segment to return
		return getBusSegs().get(segmentIndex).subList(start, end);
	}
	
	/**
	 * Small inner class to ease segment calculations
	 * @author Connor Cormier
	 *
	 */
	public class SegSection
	{
		private int segPos, pointPos;
		
		public SegSection(int segPos, int pointPos)
		{
			this.segPos = segPos;
			this.pointPos = pointPos;
		}
		
		public int getSegPos()
		{
			return segPos;
		}
		
		public int getPointPos()
		{
			return pointPos;
		}
	}
	
	private double getDistance(LatLng one, LatLng two)
	{
		return Math.sqrt(Math.pow(one.latitude - two.latitude, 2) + Math.pow(one.longitude - two.longitude, 2));
	}
	
	public long getBusRouteId()
	{
		if (first == null)
			return -1;
		
		return first.getRouteId();
	}
}
