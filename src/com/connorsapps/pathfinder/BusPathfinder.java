package com.connorsapps.pathfinder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 * Class that provides a method of finding the shortest route between two stops on the bus loop.
 * @author Connor Cormier, 11/09/13
 */
public class BusPathfinder
{
	//Agency used in all API calls
	private int agency;
	//Caching to decrease number of API calls
	//Cache all possible stops
	private List<Stop> cachedStops;
	private Map<Long, Stop> cachedStopMap;
	//Cache all routes
	private Map<Long, Route> cachedRoutes;
	//Cache update String and time update String was received
	private String cachedUpdate, cachedArrivals;
	private long cachedUpdateTime, cachedArrivalsTime;
	//Time to wait before throwing cache away in ms
	public static final long CACHE_TRASH_TIME = 30000;
	private int numStops;	
	private Map<Long, List<List<LatLng>>> routeSegments;
	
	public BusPathfinder(int agency)
	{
		this.agency = agency;
		this.numStops = 2;	//2 seems to work in a reasonable amount of time.
	}
	
	/**
	 * Get the path to get from the starting location to the ending location assuming you start moving towards the nearest active bus stop with directions
	 * found by "mappy" at "startTime".
	 * @param startLoc
	 * @param endLoc
	 * @param startTime
	 * @param mappy
	 * @return A path if possible, null otherwise
	 */
	public Path getBusPathBetween(LatLng startLoc, LatLng endLoc, long startTime, MapsRouteFinder mappy)
	{
		//Check times between two active stops nearest to each point and return minimum of all four calculations
		List<Stop> stopsNearStart = getSortedActiveStops(startLoc);
		List<Stop> stopsNearEnd = getSortedActiveStops(endLoc);
		
		//System.out.println(stopsNearStart + "\n" + stopsNearEnd);
		
		List<Path> possiblePaths = new ArrayList<Path>();
		
		//Find NUM_STOPS_TO_CHECK ^ 2 possible paths
		for (int start = 0; start < numStops && start < stopsNearStart.size(); start++)
			for (int end = 0; end < numStops && end < stopsNearEnd.size(); end++)
			{				
				//Get walking paths to starting stop and from ending stop.
				List<Step> pathToStart = mappy.getDirectionsBetween(startLoc, stopsNearStart.get(start).getLocation());
				List<Step> pathToEnd = mappy.getDirectionsBetween(stopsNearEnd.get(end).getLocation(), endLoc);
				
				//Log.v("debug", "Found map directions");
				
				if (pathToStart != null && pathToEnd != null)
				{
					//TODO remove debugging
					long calculationsStartTime = System.currentTimeMillis();
					
					//When finding arrivals, take into account the time to get to the first stop, found using overview
					List<Arrival> arrivalList = getPathBetween(stopsNearStart.get(start), stopsNearEnd.get(end), startTime + pathToStart.get(0).getDuration());
					
					Log.d("debug", "Found bus route #" + (start * numStops + end) + " after calculating for " + (System.currentTimeMillis() - calculationsStartTime) + " ms");
					
					if (arrivalList != null)
					{
						//Note: no exception should be thrown here since the walking paths are calculated in advance
						Path p = buildPath(pathToStart, pathToEnd, arrivalList, mappy, stopsNearStart.get(start), stopsNearEnd.get(end), startTime);
						possiblePaths.add(p);
					}
				}
			}
		
		Path shortestPath = null;
		long shortestPathTime = Long.MAX_VALUE;
		
		
		for (Path path : possiblePaths)
			if (path.getDuration() < shortestPathTime)
			{
				shortestPath = path;
				shortestPathTime = path.getDuration();
			}
		
		//Return the time, otherwise
		return shortestPath;
	}
	
	public Path buildPath(List<Step> pathToStart, List<Step> pathToEnd, List<Arrival> arrivalList, MapsRouteFinder mappy, Stop start, Stop end, long startTime) throws BrokenPathException
	{
		List<Step> directions = new ArrayList<Step>();
		Step walkToOverview = pathToStart.get(0);
		Step walkFromOverview = pathToEnd.get(0);
		//Use the overview of the path to the first bus stop as a direction:
		directions.add(new Step(walkToOverview.getStart(), 
				walkToOverview.getEnd(), 
				walkToOverview.getMeters(), 
				walkToOverview.getDuration(), 
				mappy.getTransportVerb() + " from your location to the bus stop at " + start.getName(),
				walkToOverview.getPolyLine(),
				false));
		
		//Throw an exception if the bus arrives before the walker
		long waitTime = arrivalList.get(0).getTimestamp() - (startTime + walkToOverview.getDuration());
		
		if (waitTime < 0)
			//Throw an exception containing the arrival time found by walking. Needed to calculate a new bus path to re-add to the queue.
			throw new BrokenPathException(String.valueOf(startTime + walkToOverview.getDuration()));
		
		directions.add(new Step(start.getLocation(), start.getLocation(), 0, 
				waitTime, "Wait for your bus to arrive.", false));
		
		//Add in all the steps from the bus route:
		directions.addAll(toSteps(arrivalList));
		
		
		long endTime = arrivalList.get(arrivalList.size() - 1).getTimestamp() + walkFromOverview.getDuration();
		
		//Use the overview of the path to the end as a direction:
		directions.add(new Step(walkFromOverview.getStart(), 
				walkFromOverview.getEnd(), 
				walkFromOverview.getMeters(), 
				walkFromOverview.getDuration(), 
				mappy.getTransportVerb() + " from " + end.getName() + " to your destination at " + walkFromOverview.getEndAddress() + " and arrive around " + this.getClockTime(endTime),
				walkFromOverview.getPolyLine(),
				false));
		
		//Assemble the path:
		//Note: end time is based on final bus arrival time + time to walk to end
		return new Path(startTime, endTime, directions);
	}
	
	public List<Step> toSteps(List<Arrival> arrivals)
	{
		List<Step> busSteps = new ArrayList<Step>();
		
		for (int i = 0; i < arrivals.size(); i++)
		{
			Arrival a = arrivals.get(i);
			Stop stop = this.getStopWithId(a.getStopId());
			
			String polyLine = null;
			//Add distances are recorded as 0
			int meters = 0;
			
			//Board a bus at even indices:
			if (i % 2 == 0)
			{
				//Set up the necessary parts of the Step object
				String description = "Board the " + getRouteNames().get(a.getRouteId()) + " bus at " + stop.getName() + " at " + this.getClockTime(a.getTimestamp());

				long duration = arrivals.get(i + 1).getTimestamp() - a.getTimestamp();
				
				Stop nextStop = this.getStopWithId(arrivals.get(i + 1).getStopId());
				
				//Add the step. Includes the bus route for use in building the polyline later on
				busSteps.add(new BusStep(stop.getLocation(), nextStop.getLocation(), meters, duration, description, polyLine, false, getRouteSegments().get(a.getRouteId()), this.getArrivalsList(), this.getStopMap(), a, arrivals.get(i + 1)));
			}
			//Transfer busses at odd indices not at end
			else
			{
				//Get off the bus step info
				String description = "Get off the bus at " + stop.getName() + " at " + this.getClockTime(a.getTimestamp());
				busSteps.add(new Step(stop.getLocation(), stop.getLocation(), meters, 0, description, polyLine, false));
				
				if (i != arrivals.size() - 1)
				{
					//Wait for the next bus step info
					description = "Wait for your next bus.";
					busSteps.add(new Step(stop.getLocation(), stop.getLocation(), meters, arrivals.get(i + 1).getTimestamp() - a.getTimestamp(), description, polyLine, false));
				}
			}
		}
		
		return busSteps;
	}
	
	/**
	 * Utility to get a clock time associated with a Unix epoch time.
	 * @param timestamp
	 * @return
	 */
	public String getClockTime(long timestamp)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(new Date(timestamp));
		
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		
		return (hour == 12 || hour == 0 ? 12 : hour % 12) + ":" + (minute < 10 ? "0" + minute : minute) + " " + (hour >= 12 ? "PM" : "AM");
	}
	
	/**
	 * Get the time a path takes to complete. Just a basic utility for making comparisons.
	 * @param path
	 * @return
	 */
	public long getPathTime(List<Arrival> path)
	{
		//Not going anywhere? Takes no time, I guess
		if (path.size() == 0)
			return 0;
		
		//Return the time between the first and last stop
		return path.get(path.size() - 1).getTimestamp() - path.get(0).getTimestamp();
	}

	/**
	 * Get the path between two ACTIVE stops using arrivals
	 * @param start
	 * @param end
	 * @param startTime
	 * @return
	 */
	public List<Arrival> getPathBetween(Stop start, Stop end, long startTime)
	{
		List<Arrival> possible = getArrivalsAfter(startTime);
		List<Arrival> thisPath;
		
		for (int i = 0; i < possible.size(); i++)
		{		
			Arrival a = possible.get(i);
			
			//YES!! This is all coming together!
			if (a.getStopId() == end.getId())
			{
				//Set used to remember vehicles that have been visited in other branches. Know not to check them again
				Set<Long> memSet = new HashSet<Long>();
				
				//Trace backwards to see if any buses that crossed this one crossed the arrival
				thisPath = getShortestPathToArrival(start, a, possible.subList(0, i), memSet);
				
				//If the path works, return it.
				if (thisPath != null)
					return thisPath;
			}
		}
		
		//Impossible to connect two stops at this time
		return null;
	}
	
	/**
	 * Get the shortest path to some arrival point. ALWAYS chooses the one with the least amount of time spent on a bus.
	 * @param start
	 * @param thisArrival
	 * @param possible
	 * @return shortest path with least amount of time spent on bus, null if impossible.
	 */
	public List<Arrival> getShortestPathToArrival(Stop start, Arrival thisArrival, List<Arrival> possible, Set<Long> memSet)
	{
		//Set up a map containing all the stops this bus has been to:
		Map<Long, List<Arrival>> stops = new HashMap<Long, List<Arrival>>();
		
		//Assemble list of all stops this bus has been to. Used to check which buses have crossed its path
		for (int i = 0; i < possible.size(); i++)
		{
			Arrival a = possible.get(i);
			if (a.getVehicleId() == thisArrival.getVehicleId())
			{
				if (stops.get(a.getStopId()) == null)
					stops.put(a.getStopId(), new ArrayList<Arrival>());
				stops.get(a.getStopId()).add(a);
			}
		}
		
		if (stops.containsKey(start.getId()))
		{
			List<Arrival> thisPath = new ArrayList<Arrival>();
			thisPath.add(stops.get(start.getId()).get(0));	//Add the early visit of this vehicle to the stop to the arraylist
			thisPath.add(thisArrival);
			return thisPath;
		}
		
		//Trace back the arrivals and dive into the branches of any that share the same stops as thisArrival. Return the first one that isn't null (least time spent on bus)
		for (int i = possible.size() - 1; i >= 0; i--)
		{
			List<Arrival> thisPath;
			Arrival connection;
			//If the current vehicle has been to the other stop, try using it as an arrival
			//Only care if the bus arrived at the stop after this one
			if (stops.containsKey(possible.get(i).getStopId()) && (connection = isTransferPossible(stops.get(possible.get(i).getStopId()), possible.get(i))) != null)
			{				
				//If this bus hasn't been tested
				if (!memSet.contains(possible.get(i).getVehicleId()))
				{
					//Add this vehicle to the memSet so no more transfers are made to it.
					memSet.add(possible.get(i).getVehicleId());
					thisPath = getShortestPathToArrival(start, possible.get(i), possible.subList(0, i), memSet);
				
					//If the path made it to the origin, add in the arrival at this level and pass it on up!
					if (thisPath != null)
					{
						//Add the stop of the bus you're transferring from
						thisPath.add(connection);
						//Add in the bus you're transferring to.
						thisPath.add(thisArrival);
						return thisPath;
					}
				}
			}
			
		}
		
		//The path is impossible
		return null;
	}
	
	/**
	 * Quick utility method to get a certain identified stop.
	 * @param id
	 * @return
	 */
	public Stop getStopWithId(long id)
	{
		return getStopMap().get(id);
	}
	
	public Arrival isTransferPossible(List<Arrival> arrivals, Arrival a)
	{
		for (int i = 0; i < arrivals.size(); i++)
			if (arrivals.get(i).getTimestamp() > a.getTimestamp())
				return arrivals.get(i);
		return null;
	}
	
	/**
	 * Get a list of stops that are currently in service sorted by distance to a point
	 * @param nearest
	 * @return
	 */
	public List<Stop> getSortedActiveStops(LatLng nearest)
	{
		List<Stop> stops = getActiveStops();
		Collections.sort(stops, new StopDistanceComparator(nearest));
		return stops;
	}
	
	/**
	 * Get a list of active stops (stops with arrivals listed)
	 * @return
	 */
	public List<Stop> getActiveStops()
	{
		List<Stop> stops = getStops();
		Set<Long> activeStops = new HashSet<Long>();
		String[] arrivals = getArrivalsString().split("route_id");
		
		//Assemble a Set from the stops listed in the arrivals String
		for (int i = 1; i < arrivals.length; i++)
			activeStops.add(Long.parseLong(extractJsonField("stop_id", arrivals[i])));
		
		List<Stop> newStops = new ArrayList<Stop>();
		
		for (Stop s : stops)
			if (activeStops.contains(s.getId()))
				newStops.add(s);
		
		return newStops;
	}
	
	/**
	 * Get a map associating stop id's with stops.
	 * @return
	 */
	public Map<Long, Stop> getStopMap()
	{
		if (cachedStopMap != null)
			return cachedStopMap;
		
		Map<Long, Stop> stopMap = new HashMap<Long, Stop>();
		
		for (Stop s : getStops())
			stopMap.put(s.getId(), s);
		
		cachedStopMap = stopMap;
		
		return stopMap;
	}
	
	/**
	 * Get a list of all possible stops currently on this agency's routes
	 * @return
	 */
	public List<Stop> getStops()
	{
		if (cachedStops != null)
			return cachedStops;
		
		String stops = getStopString();
		String[] stopParts = stops.split("\"code\":");
		
		//Create list to hold each stop object
		cachedStops = new ArrayList<Stop>(stopParts.length - 1);
		
		//Assemble each stop
		for (int i = 1; i < stopParts.length; i++)
		{
			//Easy, direct extractions
			long id = Long.parseLong(extractJsonField("id", stopParts[i]));
			String name = extractJsonField("name", stopParts[i]);
			
			//This is a bit more complicated. Get the String first:
			String strPos = extractJsonField("position", stopParts[i]).replace("[", "").replace("]", "").trim();
			
			//Now convert the position String into a LatLng object
			LatLng position = new LatLng(Double.parseDouble(strPos.split(",")[0].trim()), Double.parseDouble(strPos.split(",")[1].trim()));
			
			cachedStops.add(new Stop(id, name, position));
		}
		
		return cachedStops;
	}
	
	/**
	 * @return a map where the route id corresponds with a polyline containing all segments in that route
	 */
	public Map<Long, List<List<LatLng>>> getRouteSegments()
	{
		if (routeSegments != null)
			return routeSegments;
		
		Map<Long, List<LatLng>> segments = new HashMap<Long, List<LatLng>>();
		Map<Long, List<List<LatLng>>> routeSegments = new HashMap<Long, List<List<LatLng>>>();
		
		String response = NetworkUtils.getUrl("http://feeds.transloc.com/2/segments?agencies=" + agency);
		
		String segStr = response.split("\"segments\": \\[\\{")[1];
		
		String[] segmentParts = segStr.split("\"id\": ");
		
		for (int i = 1; i < segmentParts.length; i++)
		{
			long id = Long.parseLong(segmentParts[i].split(",")[0]);
			String polyLine = segmentParts[i].split("\"points\": \"")[1].split("\"")[0];
			segments.put(id, Path.polyToList(polyLine));
		}
		
		//Now assemble the route segments from what's in the segments map
		String routeStr = response.split("\"segments\": \\[\\{")[0];
		String[] routeSegmentParts = routeStr.split("\"id\": ");
		
		for (int i = 1; i < routeSegmentParts.length; i++)
		{
			long routeId = Long.parseLong(routeSegmentParts[i].split(",")[0]);
			String[] segList = routeSegmentParts[i].split("\"segments\": \\[")[1].split("\\]")[0].split(",");
			
			List<List<LatLng>> segs = new ArrayList<List<LatLng>>();
			
			for (String s : segList)
			{
				if (s.trim().equals(""))
					break;
			
				long segId = Long.parseLong(s.trim());
				
				List<LatLng> seg = segments.get(Math.abs(segId));
				
				//Reverse if the id is negative, indicating that the bus is traveling backwards along this segment
				if (segId > 0)
					Collections.reverse(seg);
				
				if (seg.size() > 0)
					segs.add(seg);
			}
			
			routeSegments.put(routeId, segs);
		}
		
		this.routeSegments = routeSegments;
		
		return routeSegments;
	}
	
	/**
	 * Extract the first occurrence of a Json field from a String, throw an exception if it doesn't exist
	 * @param field
	 * @param source
	 * @return First occurrence of Json String (without quotes)
	 */
	private String extractJsonField(String field, String source)
	{
		return source.substring(source.indexOf("\"" + field + "\": ") + ("\"" + field + "\": ").length()).split("(, \"|\\})")[0].replace("\"", "").trim();
	}
	
	/**
	 * Return the stops String. Note that this should only have to be invoked once.
	 * @return
	 */
	public String getStopString()
	{
		return NetworkUtils.getUrl("http://feeds.transloc.com/2/stops?agencies=" + agency);
	}
	
	/**
	 * Get a list of arrivals after a certain time
	 * @param time
	 * @return
	 */
	public List<Arrival> getArrivalsAfter(long time)
	{
		List<Arrival> arrivals = getArrivalsList();
		Collections.sort(arrivals);
		
		int i = 0;
		for (; i < arrivals.size(); i++)
		{
			//End this loop when the timestamps are in the future of the start time we want
			if (arrivals.get(i).getTimestamp() > time)
				break;
		}
		
		//Get the list of relevant arrivals
		arrivals = arrivals.subList(i, arrivals.size());
		
		return arrivals;
	}
	
	/**
	 * Get a list of all arrivals at the current time
	 * @return
	 */
	public List<Arrival> getArrivalsList()
	{
		String[] arrivalParts = getArrivalsString().split("route_id\": ");
		List<Arrival> arrivalsList = new ArrayList<Arrival>();
		
		for (int i = 1; i < arrivalParts.length; i++)
		{
			long stopId = Long.parseLong(extractJsonField("stop_id", arrivalParts[i]));
			long timestamp = Long.parseLong(extractJsonField("timestamp", arrivalParts[i]));
			long vehicleId = Long.parseLong(extractJsonField("vehicle_id", arrivalParts[i]));
			long routeId = Long.parseLong(arrivalParts[i].substring(0, arrivalParts[i].indexOf(",")));
			
			arrivalsList.add(new Arrival(stopId, timestamp, vehicleId, routeId));
		}
		
		return arrivalsList;
	}
	
	/**
	 * Get the arrivals String. Returns cached copy if invoked within CACHE_TRASH_TIME window
	 * @return
	 */
	public String getArrivalsString()
	{
		//If data is current, return it
		if (System.currentTimeMillis() - cachedArrivalsTime < CACHE_TRASH_TIME)
			return cachedArrivals;
		//Update data and return new update if it isn't current
		cachedArrivals = NetworkUtils.getUrl("http://feeds.transloc.com/2/arrivals?agencies=" + agency);
		cachedArrivalsTime = System.currentTimeMillis();
		return cachedArrivals;
	}
	
	/**
	 * Get the update String. Returns cached copy if invoked within CACHE_TRASH_TIME window
	 * @return
	 */
	public String getUpdateString()
	{
		//If data is current, return it
		if (System.currentTimeMillis() - cachedUpdateTime < CACHE_TRASH_TIME)
			return cachedUpdate;
		//Update data and return new update if it isn't current
		cachedUpdate = NetworkUtils.getUrl("http://feeds.transloc.com/2/update?agencies=" + agency);
		cachedUpdateTime = System.currentTimeMillis();
		return cachedUpdate;
	}
	
	/**
	 * Get a map containing route names associated with thier id's.
	 * @return
	 */
	public Map<Long, Route> getRouteNames()
	{
		//Attempt to return cached values
		if (cachedRoutes != null)
			return cachedRoutes;
		
		//Access network otherwise
		String routeData = NetworkUtils.getUrl("http://feeds.transloc.com/2/routes?agencies=" + agency);
		
		String[] parts = routeData.split("agency_id");

		Map<Long, Route> routeNames = new HashMap<Long, Route>();

		for (int i = 1; i < parts.length; i++)
		{
			long id = Long.parseLong(this.extractJsonField("id", parts[i]));
			String name = this.extractJsonField("long_name", parts[i]);
			String color = this.extractJsonField("color", parts[i]);
			
			routeNames.put(id, new Route(name, color, id));
		}
		
		cachedRoutes = routeNames;
		
		return routeNames;
	}
	
	public int getAgency()
	{
		return agency;
	}


	public void setAgency(int agency)
	{
		//Reset all cached values
		this.cachedStops = null;
		this.cachedStopMap = null;
		this.cachedRoutes = null;
		this.cachedUpdate = null;
		this.cachedArrivals = null;
		this.cachedUpdateTime = 0;
		this.cachedArrivalsTime = 0;
		//Change the agency ID
		this.agency = agency;
	}
	
	public void setNumStops(int newNum)
	{
		this.numStops = newNum;
	}
	
	public int getNumStops()
	{
		return this.numStops;
	}
}
