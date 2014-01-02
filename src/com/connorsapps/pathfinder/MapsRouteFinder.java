package com.connorsapps.pathfinder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.gms.maps.model.LatLng;

/**
 * Find route between points using Google Maps
 * @author Connor Cormier
 *
 */
public class MapsRouteFinder
{
	private String modeOfTransport;
	private int modeInt;
	private Map<String, List<Step>> memMap;
	
	
	public MapsRouteFinder(int modeOfTransport)
	{
		setModeOfTransport(modeOfTransport);
		memMap = new HashMap<String, List<Step>>();
	}
	
	/**
	 * Get the path between two points at a certain time. Includes detailed directions of each step.
	 * @param start
	 * @param end
	 * @param startTime
	 * @return Path if possible, null otherwise
	 */
	public Path getPathBetween(LatLng start, LatLng end, long startTime)
	{
		List<Step> steps = getDirectionsBetween(start, end);
		
		if (steps == null)
			return null;
		
		//Path including overview polyline
		Path path = new Path(startTime, startTime + steps.get(0).getDuration(), steps.subList(1, steps.size()), steps.get(0).getPolyLine());
		
		//Add end step for a nice, conclusive feeling
		path.addDirection(new Step(steps.get(0).getEnd(), steps.get(0).getEnd(), 0, 0, "Arrive at your destination, " + steps.get(0).getEndAddress() + " around " + this.getClockTime(path.getEndTime()), "", false));
		
		return path;
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
	 * Returns a list containing all of the steps to get from one point to another, null if something goes wrong.
	 * @param start
	 * @param end
	 * @return List containing steps. The first step IS AN OVERVIEW OF THE ENTIRE ROUTE. Subsequent steps are single directions.
	 */
	public List<Step> getDirectionsBetween(LatLng start, LatLng end)
	{
		//Key to memoization
		String key = "(" + getModeOfTransport() + ")" + start + end;
		
		if (memMap.containsKey(key))
			return memMap.get(key);
		
		try
		{
			//Log.v("debug", "Making request");
			
			String request = "http://maps.googleapis.com/maps/api/directions/json?origin=" + start.latitude + "," + start.longitude + 
					"&destination=" + end.latitude + "," + end.longitude + "&mode=" + getModeOfTransport().toLowerCase() + "&sensor=false";
			String response = NetworkUtils.getUrl(request);
			
			//Log.v("debug", "Got return value");
			
			if (response.contains("\"status\" : \"OK\""))
			{
				List<Step> steps = new ArrayList<Step>();
				
				response = response.substring(response.indexOf("\"legs\" : ["));
				
				String[] parts = response.split("distance");
				
				//Get the overview piece first
				if (response.contains("steps"))
					steps.add(getStepPiece(response, true));
				
				for (int i = 2; i < parts.length; i++)
				{
					response = parts[i];
					
					steps.add(getStepPiece(response, false));
				}
				
				//Memoize this so the same call isn't made to Google Maps again. Nothing should change each time.
				memMap.put(key, steps);
				
				//Log.v("debug", "Built steps list");
				
				return steps;	
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Get the first step that appears in this JSON map direction String
	 * @param step
	 * @param isOverview If overview, handle things a bit differently
	 */
	private Step getStepPiece(String step, boolean isOverview)
	{
		int meters = Integer.parseInt(extractGoogleJSONPart("value", step));
		
		step = step.substring(step.indexOf("duration"));
		
		long time = Integer.parseInt(extractGoogleJSONPart("value", step)) * 1000;
		
		LatLng endLoc = new LatLng(Double.parseDouble(this.extractGoogleJSONPart("lat", step)), Double.parseDouble(this.extractGoogleJSONPart("lng", step)));
		
		
		String instructions; 
		if (!isOverview)
			instructions = this.extractGoogleJSONPart("html_instructions", step);
		else
			instructions = "Route from " + this.extractGoogleJSONPart("start_address", step) + Step.ADDRESS_DELIMITER + this.extractGoogleJSONPart("end_address", step);
		
		//Remove excess html bullshit
		instructions = cleanHtmlBullshit(instructions);
		
		String polyLine;
		//Get the polyline slightly differently for the overview
		if (!isOverview)
		{
			polyLine = step.split("\"points\" :")[1].split("\"")[1].trim();
		}
		else
		{
			String section = step.substring(step.indexOf("overview_polyline"));
			section = section.split("\"points\" :")[1].split("\"")[1];
			polyLine = section.trim();
		}
		
		step = step.split("start_location")[1];
		
		LatLng startLoc = new LatLng(Double.parseDouble(this.extractGoogleJSONPart("lat", step)), Double.parseDouble(this.extractGoogleJSONPart("lng", step)));
	
		
		return new Step(startLoc, endLoc, meters, time, instructions, polyLine, isOverview);
	}
	
	private String cleanHtmlBullshit(String bullshit)
	{
		//This specific tag should be a period and space.
		bullshit = bullshit.replace("\\u003cdiv style=\\font-size:0.9em\\\\u003e", ". ");
		//Stand back, guys. I know regex (well, a bit). Also, XKCD is great.
		return bullshit.replaceAll("\\\\u003c.*?\\\\u003e", "");
	}
	
	private String extractGoogleJSONPart(String field, String source)
	{
		//TODO move away from substring here?
		String part = source.substring(source.indexOf("\"" + field + "\" : ") + ("\"" + field + "\" : ").length()).split("(\",|\\}|,\n)")[0].replace("\"", "").trim();
		return part;
	}
	
	
	public void setModeOfTransport(int mode)
	{
		this.modeInt = mode;
		
		switch (mode)
		{
		case 0:
			modeOfTransport = "Walking";
			break;
		case 1:
			modeOfTransport = "Bicycling";
			break;
		case 2:
			modeOfTransport = "Driving";
			break;
		case 3:
			modeOfTransport = "Transit";
			break;
		default:
			throw new RuntimeException("Invalid mode of transport");
		}
	}
	
	public String getTransportVerb()
	{
		if (this.modeOfTransport.equals("Driving"))
			return "Drive";
		if (this.modeOfTransport.equals("Walking"))
			return "Walk";
		if (this.modeOfTransport.equals("Bicycling"))
			return "Ride";
			
		return "Take transit";
	}
	
	public int getModeOfTransportInt()
	{
		return modeInt;
	}
	
	public String getModeOfTransport()
	{
		return this.modeOfTransport;
	}
	
	/**
	 * Bit o' debugging
	 * @param args
	 */
	public static void main(String[] args)
	{
//		MapsRouteFinder finder = new MapsRouteFinder(1);
//		Router router = new Router();
//		//System.out.println(NetworkUtils.getUrl("http://maps.googleapis.com/maps/api/directions/json?origin=Charlottesville&destination=Albermarle&mode=walking&sensor=false"));
//		List<Step> steps = finder.getDirectionsBetween(router.getGeocode("Woody House, Charlottesville"), router.getGeocode("Clark Hall, Virginia"));
//		
//		for (Step step : steps)
//			System.out.println(step);
	}
}
