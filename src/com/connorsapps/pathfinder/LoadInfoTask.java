package com.connorsapps.pathfinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Task to handle loading saved state of app and prompting user to enter a transit agency if one is
 * not already present
 * @author Connor Cormier
 *
 */
public class LoadInfoTask extends AsyncTask<String, Integer, Boolean>
{
	private boolean promptAgency;
	private MainActivity callback;
	
	public LoadInfoTask(MainActivity callback)
	{
		this.callback = callback;
	}

	@Override
	protected Boolean doInBackground(String... fileName)
	{
		Scanner input = null;
		try
		{
			input = new Scanner(callback.openFileInput(fileName[0]));
			
			List<Integer> params = new ArrayList<Integer>();
			
			while (input.hasNextInt())
				params.add(input.nextInt());		
			
			Integer[] parts = new Integer[params.size()];
			params.toArray(parts);
			
			this.publishProgress(parts);
			
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			promptAgency = true;
			this.publishProgress();
			
			return false;
		}
		finally
		{
			if (input != null)
				input.close();
		}
	}
	
	@Override
	public void onProgressUpdate(Integer... loadedValues)
	{
		//If file didn't exist or was broken
		if (promptAgency)
		{
			//Prompt the user to change the agency
			callback.changeAgency();
			//Write the default agency to the file in the background
			//so they won't be prompted again, even if they don't change anything
			callback.saveState();
		}
		else
		{
			try
			{
				//Set the transit agency to the loaded value
				callback.getRoutey().setTransitAgency(loadedValues[0]);
				callback.setShowBusRouteSetting(loadedValues[1]);
				callback.setMapTypeSetting(loadedValues[2]);
				callback.setNumStops(loadedValues[3]);
				callback.setModeOfTranportation(loadedValues[4]);
			}
			catch (Exception e)
			{
				Log.d("debug", "Invalid save file detected. Being changed now.");
			}
		}
	}

}
