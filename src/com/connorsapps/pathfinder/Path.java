package com.connorsapps.pathfinder;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Representation of a path made up of a list of steps.
 * @author Connor Cormier
 *
 */
public class Path
{
	private long startTime, endTime;
	private List<Step> directions;
	//String representation of the overview of this polyline
	private String overviewPoly;
	
	/**
	 * Create a path with all fields set
	 * @param startTime
	 * @param endTime
	 * @param directions
	 */
	public Path(long startTime, long endTime, List<Step> directions)
	{
		setStartTime(startTime);
		setEndTime(endTime);
		setDirections(directions);
	}
	
	/**
	 * Set all fields and include an overview polyline
	 * @param startTime
	 * @param endTime
	 * @param directions
	 * @param overviewPoly
	 */
	public Path(long startTime, long endTime, List<Step> directions, String overviewPoly)
	{
		this(startTime, endTime, directions);
		setOverviewPoly(overviewPoly);
	}
	
	/**
	 * Create a path with a set start time but no directions, can be added later on.
	 * @param startTime
	 */
	public Path(long startTime)
	{
		setStartTime(startTime);
		setEndTime(startTime);
		setDirections(new ArrayList<Step>());
	}
	
	/**
	 * Create a path with just a start time and an overview polyline
	 * @param startTime
	 * @param overviewPoly
	 */
	public Path(long startTime, String overviewPoly)
	{
		this(startTime);
		setOverviewPoly(overviewPoly);
	}
	
	/**
	 * Set the start time and provide a list of directions, but generate the end time dynmically
	 * @param startTime
	 * @param directions
	 */
	public Path(long startTime, List<Step> directions)
	{
		setStartTime(startTime);
		setDirections(directions);
		setEndTimeDynamically();
	}
	
	/**
	 * Set the start time and provide a list of directions. Also give an overview.
	 * @param startTime
	 * @param directions
	 * @param overviewPoly
	 */
	public Path(long startTime, List<Step> directions, String overviewPoly)
	{
		this(startTime, directions);
		setOverviewPoly(overviewPoly);
	}
	
	/**
	 * Add a step and adjust the end time to accommodate it.
	 * @param s
	 */
	public void addDirection(Step s)
	{
		getDirections().add(s);
		setEndTime(getEndTime() + s.getDuration());
	}

	public long getStartTime()
	{
		return startTime;
	}

	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}

	public long getEndTime()
	{
		return endTime;
	}

	public void setEndTime(long endTime)
	{
		this.endTime = endTime;
	}
	
	/**
	 * Set the end time based on the time of each directions step.
	 */
	public void setEndTimeDynamically()
	{
		setEndTime(startTime);
		
		for (Step s : getDirections())
			setEndTime(getEndTime() + s.getDuration());
	}
	
	public List<Step> getDirections()
	{
		return directions;
	}

	public void setDirections(List<Step> directions)
	{
		this.directions = directions;
	}
	
	public long getDuration()
	{
		return this.getEndTime() - this.getStartTime();
	}
	
	public String getOverviewPoly()
	{
		return overviewPoly;
	}

	public void setOverviewPoly(String overviewPoly)
	{
		this.overviewPoly = overviewPoly;
	}
	
	/**
	 * Get a list of all markers associated with this path
	 * @return
	 */
	public List<MarkerOptions> getMarkers()
	{
		List<MarkerOptions> ops = new ArrayList<MarkerOptions>();

		for (Step s : directions)
		{
			if (s.getPolyLine() == null)
			{
				addMarker(s, ops);
			}
		}
		
		return ops;
	}
	
	/**
	 * Add a new marker if one doesn't exist at the same location, tack the description on to the old one if it does
	 * @param s
	 * @param ops
	 */
	private void addMarker(Step s, List<MarkerOptions> ops)
	{
		int markerIndex;
		if ((markerIndex = indexOf(s, ops)) != -1)
		{
			ops.get(markerIndex).snippet(ops.get(markerIndex).getSnippet() + "\n" + s.getDescription());
		}
		else
		{
			ops.add(new MarkerOptions().position(s.getStart()).title("Directions:").snippet(s.getDescription()));
		}
	}
	
	private int indexOf(Step s, List<MarkerOptions> ops)
	{
		for (int i = ops.size() - 1; i >= 0; i--)
			if (ops.get(i).getPosition().equals(s.getStart()))
				return i;
		
		return -1;
	}

	/**
	 * Get a list of all polylines associated with this path
	 * @return
	 */
	public List<PolylineOptions> getWalkPolylines()
	{
		List<PolylineOptions> ops = new ArrayList<PolylineOptions>();
		
		//Just look at the overview if one exists
		if (this.overviewPoly != null)
		{
			List<LatLng> parts = polyToList(this.getOverviewPoly());
			PolylineOptions p = new PolylineOptions();
			for (LatLng part : parts)
				p.add(part);
			p.color(Color.BLUE);
			ops.add(p);
			return ops;
		}
		
		
		for (Step s : directions)
		{
			if (s.getPolyLine() != null)
			{
				List<LatLng> polyParts = polyToList(s.getPolyLine());
				PolylineOptions thisOps = new PolylineOptions();
				
				for (LatLng point : polyParts)
					thisOps.add(point);
				
				thisOps.color(Color.BLUE);
				
				ops.add(thisOps);
			}
		}
		
		return ops;
	}
	
	/**
	 * Get the polylines associated with bus routes
	 * @param routey
	 * @return
	 */
	public List<PolylineOptions> getRoutePolylines(Router routey)
	{
		//Code did not work, not using.
		//return this.getSegmentPolylines(routey, true);
		
		List<PolylineOptions> polies = new ArrayList<PolylineOptions>();
		
		for (Step step : directions)
		{
			if (step instanceof BusStep)
			{
				BusStep bs = (BusStep)step;
				
				PolylineOptions op = new PolylineOptions();
				
				//Set color of route
				op.color(routey.getBusRoute(bs.getBusRouteId()).getColor());
				
				List<LatLng> route = bs.getBusRoute();
				
				if (route != null)
					for (LatLng point : route)
						op.add(point);

				polies.add(op);
			}
		}
		return polies;
	}
	
	public List<PolylineOptions> getSegmentPolylines(Router routey)
	{
		return this.getSegmentPolylines(routey, false);
	}
	
	public List<PolylineOptions> getSegmentPolylines(Router routey, boolean traceRoute)
	{
		List<PolylineOptions> polylines = new ArrayList<PolylineOptions>();
		
		for (Step s : directions)
		{
			if (s instanceof BusStep)
			{
				BusStep bs = (BusStep)s;
				
				List<List<LatLng>> polies = (traceRoute ? bs.getBusRouteSegs() : bs.getBusSegs());
				
				for (List<LatLng> poly : polies)
				{
					PolylineOptions ops = new PolylineOptions();
					
					ops.color(routey.getBusRoute(bs.getBusRouteId()).getColor());
					
					for (LatLng point : poly)
						ops.add(point);
					
					polylines.add(ops);
				}
			}
		}
		
		return polylines;
	}

	/**
	 * Convert a polyline String into a list of LatLng objects.
	 * Code from: http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java 
	 * @param encoded
	 * @return
	 */
	public static List<LatLng> polyToList(String encoded)
	{
		//Log.i("debug", "Polyline received: " + encoded);
		
		//IMPORTANT: This replacement was what was missing the whole time.
		encoded = encoded.replace("\\\\", "\\");
		//Don't ask me what's going on in this method.
		List<LatLng> poly = new ArrayList<LatLng>();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;
		
		while (index < len)
		{
			int b, shift = 0, result = 0;
			
			do
			{
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			}
			while (b >= 0x20);
			
			int dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dLat;
			
			//Reset for lng calculations
			result = 0;
			shift = 0;
			
			do
			{
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			}
			while (b >= 0x20);
			
			int dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dLng;
			
			LatLng p = new LatLng((((double)lat / 1E5)), (((double)lng / 1E5)));
			poly.add(p);
		}
		
		return poly;
	}
	
	public String toString()
	{
		String ret = "";
		
		for (Step s : directions)
			ret += s.getDescription() + "\n";
		
		return ret;
	}
}
