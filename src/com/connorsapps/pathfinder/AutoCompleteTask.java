package com.connorsapps.pathfinder;

import java.util.List;

import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.android.gms.maps.model.LatLng;

/**
 * Update the autocomplete suggestions in the destination input
 * @author Connor Cormier
 *
 */
public class AutoCompleteTask extends AsyncTask<String, ArrayAdapter<PlaceReference>, Boolean>
{
	private MainActivity callback;
	private LatLng about;
	private AutoCompleteTextView input;
	
	public AutoCompleteTask(MainActivity callback, LatLng about, AutoCompleteTextView input)
	{
		this.callback = callback;
		this.about = about;
		this.input = input;
	}
	
	@Override
	protected Boolean doInBackground(String... arg0)
	{
		String str = arg0[0];
		PlaceReference[] autoComplete = callback.getRoutey().getAutoCompleteSuggestions(str, about);
		ArrayAdapter<PlaceReference> adapter = new ArrayAdapter<PlaceReference>(callback, android.R.layout.simple_list_item_1, autoComplete);
		this.publishProgress(adapter);
		return true;
	}

	@Override
	protected void onProgressUpdate(ArrayAdapter<PlaceReference>... adapters)
	{
		input.setAdapter(adapters[0]);
	}
}
