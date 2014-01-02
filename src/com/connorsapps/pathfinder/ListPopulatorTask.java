package com.connorsapps.pathfinder;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListPopulatorTask extends AsyncTask<ListView, ListPopulatorTask.Agency, Boolean>
{
	private AgencySelectionActivity callback;
	private ListView view;
	private int curAgency;
	
	public ListPopulatorTask(AgencySelectionActivity callback, int curAgency)
	{
		this.callback = callback;
		this.curAgency = curAgency;
	}
	
	@Override
	protected Boolean doInBackground(ListView... params)
	{
		view = params[0];
		
		//Access and parse the open api's list of agencies, but add UVa first!
		Agency[] agencies = getAgencies();
		
		this.publishProgress(agencies);
		
		return true;
	}
	
	public Agency[] getAgencies()
	{
		String list = NetworkUtils.getUrl("http://feeds.transloc.com/2/agencies");
		
		String[] pieces = list.split("\"affiliated_agencies\":");
		Agency[] agencies = new Agency[pieces.length - 1];

		boolean found = false;
		
		for (int i = 1; i < pieces.length; i++)
		{			
			list = pieces[i];
			
			String name = list.split("\"long_name\": \"")[1].split("\",")[0].trim();
			int id = Integer.parseInt(list.split("\"id\": ")[1].split(",")[0].trim());
	
			if (curAgency == id)
			{
				//Put the current agency at the start of the list
				agencies[0] = new Agency(name + " (Current)", id);
				//Put the next value at the current index
				found = true;
			}
			else
				agencies[i + (found ? -1 : 0)] = new Agency(name, id);
			
			//list = list.substring(Math.max(list.indexOf("\"long_name\": \"") + "\"long_name\": \"".length(), list.indexOf("\"id\": ") + "\"id\": ".length()));
		}
		
		return agencies;
	}
	
	@Override
	protected void onProgressUpdate(final Agency... agencies)
	{
		view.setAdapter(new ArrayAdapter<Agency>(callback, android.R.layout.simple_list_item_1, agencies));
		
		view.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View childView, int position, long id)
			{
				//Set the agency and finish this activity. MainActivity will need to take result
				Intent data = new Intent();
				data.putExtra("newAgency", agencies[position].getAgencyId());
				callback.setResult(MainActivity.RESULT_OK, data);
				callback.finish();
			}
			
		});
	}
	
	/**
	 * Class to hold agency information
	 * @author Connor Cormier
	 *
	 */
	public class Agency
	{
		private String name;
		private int agencyId;
		
		public Agency(String name, int id)
		{
			setName(name);
			setAgencyId(id);
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public int getAgencyId()
		{
			return agencyId;
		}

		public void setAgencyId(int agencyId)
		{
			this.agencyId = agencyId;
		}
		
		public String toString()
		{
			return this.name;// + " (" + agencyId + ")";
		}
	}
}
