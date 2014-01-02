package com.connorsapps.pathfinder;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

/**
 * Task to handle associating an entered location with a latlng
 * @author Connor Cormier
 *
 */
public class GeocodeFinderTask extends AsyncTask<String, Place, Boolean>
{
	private boolean isInvalidLocation;
	private final LatLng about;
	private final MainActivity callback;

	public GeocodeFinderTask(MainActivity callback, LatLng about)
	{
		this.callback = callback;
		this.about = about;
	}
	
	@Override
	protected Boolean doInBackground(String... locations)
	{
		try
		{
			this.publishProgress(callback.getRoutey().getGeocodePossibilities(locations[0], about));
		}
		catch (InvalidLocationException e)
		{
			e.printStackTrace();
			this.isInvalidLocation = true;
			this.publishProgress();
		}

		return true;
	}
	
	protected void onProgressUpdate(Place... places)
	{
		if (this.isInvalidLocation)
			callback.reportInvalidDestination();
		//If there is only one possibility, find the route
		else if (places.length == 1)
			callback.findRoutes(about, places[0].getLocation());
		//If there is ambiguity, prompt the user
		else
			callback.inflatePlaces(about, places);
	}
}
