package com.connorsapps.pathfinder;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

public class PlaceReferenceAnalysisTask extends AsyncTask<PlaceReference, Place, Boolean>
{
	private final MainActivity callback;
	private final LatLng about;
	private boolean locationInvalid;
	
	public PlaceReferenceAnalysisTask(MainActivity callback, LatLng loc)
	{
		this.callback = callback;
		this.about = loc;
	}

	@Override
	protected Boolean doInBackground(PlaceReference... params)
	{
		//Get the location associated with a place reference
		Place place = callback.getRoutey().getPlaceDetails(params[0].getReference());
		
		if (place == null)
			locationInvalid = true;
		
		this.publishProgress(place);
		
		return true;
	}
	
	@Override
	protected void onProgressUpdate(Place...places)
	{
		if (locationInvalid)
			callback.reportInvalidDestination();
		else
			callback.findRoutes(about, places[0].getLocation());
	}
}
