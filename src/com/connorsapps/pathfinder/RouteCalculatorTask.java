package com.connorsapps.pathfinder;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 * Perform the background calculations
 * @author Connor Cormier
 *
 */
public class RouteCalculatorTask extends AsyncTask<Object, Path, Boolean>
{
	private final MainActivity callback;
	private final LatLng loc;
	private final LatLng dest;
	
	//TODO remove debugging
	private long startTime;
	
	/**
	 * Create a route calculator async task to calculate the routes between two points
	 * @param loc
	 * @param dest
	 */
	public RouteCalculatorTask(MainActivity callback, LatLng loc, LatLng dest)
	{
		this.callback = callback;
		this.loc = loc;
		this.dest = dest;
	}
	
	@Override
	protected Boolean doInBackground(Object... params)
	{
		startTime = System.currentTimeMillis();
		
		Path bus = callback.getRoutey().getBusPath(loc, dest);
		Path walk = callback.getRoutey().getWalkingPath(loc, dest);
		
		Log.i("debug", "Time to find route: " + (System.currentTimeMillis() - startTime));
			
		this.publishProgress(bus, walk);

		return true;
	}
	
	@Override
	protected void onProgressUpdate(Path... paths)
	{
		Path bus = paths[0];
		Path walk = paths[1];
		
		callback.updateRoutes(bus, walk);
	}

}
