package com.connorsapps.pathfinder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/**
 * The class that will be utilized directly in our app
 * @author Connor Cormier
 *
 */
public class Router
{
	public static final String PLACES_API_KEY = "AIzaSyCwwmobpkrrnQEG76mqvb-XvVqR2T_1BFo";
	public static final long DEBUG_TIME = 1387837168000L;
	private MapsRouteFinder mapper;
	private BusPathfinder busser;
	private Map<String, LatLng> geocodeMemory;
	
	public Router()
	{
		//Walking by default. Can be reset later on.
		mapper = new MapsRouteFinder(0);
		//UVa's ID is hardcoded into the bus pathfinder for now.
		//Maybe allow this to be changed some day for wider distribution.
		busser = new BusPathfinder(347);
		
		//Remeber places that have previously been looked up to avoid excessive network usage and latency.
		geocodeMemory = new HashMap<String, LatLng>();
	}
	
	public Path getFastestPath(String start, String end, LatLng loc) throws InvalidLocationException
	{
		LatLng startLoc = this.getGeocode(start, loc);
		LatLng endLoc = this.getGeocode(end, loc);
		
		if (startLoc != null && endLoc != null)
			return getFastestPath(startLoc, endLoc);
		return null;
	}
	
	/**
	 * Make it easier for the front end to directly get the walking path
	 * @param loc
	 * @param destination
	 * @return
	 */
	public Path getWalkingPath(Location loc, String destination) throws InvalidLocationException
	{
		LatLng start = new LatLng(loc.getLatitude(), loc.getLongitude());
		LatLng dest = this.getGeocode(destination, start);
		
		return this.getWalkingPath(start, dest);
	}
	
	/**
	 * Make it easier for the front end to directly get the bus path
	 * @param loc
	 * @param destination
	 * @return
	 */
	public Path getBusPath(Location loc, String destination) throws InvalidLocationException
	{		
		LatLng start = new LatLng(loc.getLatitude(), loc.getLongitude());
		LatLng dest = this.getGeocode(destination, start);
		
		return this.getBusPath(start, dest);
	}
	
	/**
	 * Get the fastest path between two points, comparing walking and bus routes.
	 * @param start
	 * @param end
	 * @return
	 */
	public Path getFastestPath(LatLng start, LatLng end)
	{
		Path walking = getWalkingPath(start, end);
		Path bus = getBusPath(start, end);
		
		//A little testing
		System.out.println("Walking:\n" + walking + "\nBus:\n" + bus);
		
		//Return whichever's possible if one doesn't work
		if (bus == null)
			return walking;
		if (walking == null)
			return bus;
		
		//Walking's better if they're the same length :)
		return (walking.getEndTime() <= bus.getEndTime() ? walking : bus);
	}
	
	/**
	 * Get the walking path (technically could be biking, driving, or transit as well)
	 * @param start
	 * @param end
	 * @return
	 */
	public Path getWalkingPath(LatLng start, LatLng end)
	{
		long time = (MainActivity.DEBUG ? DEBUG_TIME : System.currentTimeMillis());
		return mapper.getPathBetween(start, end, time);
	}
	
	/**
	 * Get the bus path
	 * @param start
	 * @param end
	 * @return
	 */
	public Path getBusPath(LatLng start, LatLng end)
	{
		int modeOfTransportToCompare = this.getModeOfTransportInt();
		
		//Set to walking for bus route:
		this.setModeOfTransport(0);
		
		long time = (MainActivity.DEBUG ? DEBUG_TIME : System.currentTimeMillis());
		//Find the transit route
		Path p = busser.getBusPathBetween(start, end, time, mapper);
		//Path p = busser.getBusPathQuickly(start, end, System.currentTimeMillis(), mapper);
		
		//Set maps route finder back to original settings
		this.setModeOfTransport(modeOfTransportToCompare);
		
		return p;
	}
	
	/**
	 * Get the details of a place that was autocompleted
	 * @param reference
	 * @return
	 */
	public Place getPlaceDetails(String reference)
	{
		String request = "https://maps.googleapis.com/maps/api/place/details/json?key=" + PLACES_API_KEY + "&sensor=true&reference=" + reference;
		
		String response = NetworkUtils.getUrl(request);
		
		if (response.contains("\"status\" : \"OK\""))
		{
			//Create the location object off of the latitude and longitude of the first response returned by google
			LatLng location =  new LatLng(Double.parseDouble(response.split("\"lat\" : ")[1].split(",")[0]), 
					Double.parseDouble(response.split("\"lng\" : ")[1].split("(\n|\\})")[0].trim()));
			String name = response.split("\"name\" : \"")[1].split("\",")[0];
			
			return new Place(name, location);
		}
		
		return null;
	}
	
	/**
	 * Get a list of possible places associated with a String
	 * @param place
	 * @return
	 */
	public Place[] getGeocodePossibilities(String place, final LatLng about) throws InvalidLocationException
	{
		try
		{
			place = URLEncoder.encode(place, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		//TODO decide on type to use
		//String request = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?name=" + place + "&location=" + about.latitude + "," + about.longitude + "&rankby=distance&sensor=true&key=" + PLACES_API_KEY;
		String request = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + place + "&location=" + about.latitude + "," + about.longitude + "&radius=50000&sensor=true&key=" + PLACES_API_KEY;
		
		//Get the response from google
		String response = NetworkUtils.getUrl(request);
			
		if (response.contains("\"status\" : \"OK\""))
		{			
			String[] parts = response.split("\"geometry\" : \\{");
		
			try
			{
				if (parts.length <= 1)
					throw new Exception();
				
				//Array to hold all possible places
				Place[] places = new Place[parts.length - 1];
				
				for (int i = 1; i < parts.length; i++)
				{
					String part = parts[i];
					//Create the location object off of the latitude and longitude of the first response returned by google
					LatLng location =  new LatLng(Double.parseDouble(part.split("\"lat\" : ")[1].split(",")[0]), 
							Double.parseDouble(part.split("\"lng\" : ")[1].split("(\n|\\})")[0].trim()));
					String name = part.split("\"name\" : \"")[1].split("\",")[0];
					
					Place p = new Place(name, location);
					places[i - 1] = p;
				}
				
				//Sort places by distance to location
				Arrays.sort(places, new Comparator<Place>(){
					@Override
					public int compare(Place one, Place two)
					{
						Double disOne = Math.sqrt(Math.pow(one.getLocation().latitude - about.latitude, 2) + Math.pow(one.getLocation().longitude - about.longitude, 2));
						Double disTwo = Math.sqrt(Math.pow(two.getLocation().latitude - about.latitude, 2) + Math.pow(two.getLocation().longitude - about.longitude, 2));
						return disOne.compareTo(disTwo);
					}
					
				});
				
				//Return the possible places.
				return places;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		throw new InvalidLocationException("Unable to find " + place);
	}
	
	public PlaceReference[] getAutoCompleteSuggestions(String place, LatLng about)
	{
		try
		{
			place = URLEncoder.encode(place, "UTF-8");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		String request = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=" + place + "&location=" + about.latitude + "," + about.longitude + "&radius=500&sensor=true&key=" + PLACES_API_KEY;
		
		String response = NetworkUtils.getUrl(request);
		
		//Return an empty array if there are no suggestions
		if (!response.contains("\"description\" : \""))
			return new PlaceReference[]{};
		
		String[] parts = response.split("\"description\" : \"");
		PlaceReference[] suggestions = new PlaceReference[parts.length - 1];
		
		for (int i = 1; i < parts.length; i++)
		{
			String name = parts[i].split("\",")[0];
			String reference = parts[i].split("\"reference\" : \"")[1].split("\",")[0];
			suggestions[i - 1] = new PlaceReference(name, reference);
		}
		
		return suggestions;
	}
	
	/**
	 * Get the latitude and longitude of a place using the google maps places api.
	 * @param place
	 * @return
	 */
	public LatLng getGeocode(String place, LatLng about)
	{
		if (geocodeMemory.containsKey(place))
			return geocodeMemory.get(place);
		
		try
		{		
			//TODO update to use text search
			String request = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?keyword=" + URLEncoder.encode(place, "UTF-8") + "&location=" + about.latitude + "," + about.longitude + "&rankby=distance&sensor=true&key=" + PLACES_API_KEY;
			//Get the response from google
			String response = NetworkUtils.getUrl(request);
			
			if (response.contains("\"status\" : \"OK\""))
			{
				//Create the location object off of the latitude and longitude of the first response returned by google
				LatLng location =  new LatLng(Double.parseDouble(response.split("\"lat\" : ")[1].split(",")[0]), 
					Double.parseDouble(response.split("\"lng\" : ")[1].split("(\n|\\})")[0].trim()));
				
				//Add to mem map
				geocodeMemory.put(place, location);
				
				return location;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		//If things break or the location doesn't exist, just return null
		return null;
	}
	
	public void setModeOfTransport(int newMode)
	{
		mapper.setModeOfTransport(newMode);
	}
	
	public String getModeOfTransport()
	{
		return mapper.getModeOfTransport();
	}
	
	public int getModeOfTransportInt()
	{
		return mapper.getModeOfTransportInt();
	}
	
	public void setTransitAgency(int newAgency)
	{
		busser.setAgency(newAgency);
	}
	
	public int getTransitAgency()
	{
		return busser.getAgency();
	}
	
	public void setNumStops(int newNum)
	{
		busser.setNumStops(newNum);
	}
	
	public int getNumStops()
	{
		return busser.getNumStops();
	}
	
	public Route getBusRoute(long routeId)
	{
		return busser.getRouteNames().get(routeId);
	}
}
