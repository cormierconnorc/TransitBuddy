package com.connorsapps.pathfinder;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

/**
 * Activity to allow agency selection
 * @author Connor Cormier
 *
 */
public class AgencySelectionActivity extends Activity
{
	private ListView view;
	

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_agency_selection);
		// Show the Up button in the action bar.
		setupActionBar();
		
		view = (ListView)findViewById(R.id.agencyList);
		//Get the current agency setting
		int curAgency = this.getIntent().getIntExtra("curAgency", -1);
		
		populateList(curAgency);
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar()
	{
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.agency_selection, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void populateList(int curAgency)
	{
		ListPopulatorTask task = new ListPopulatorTask(this, curAgency);
		task.execute(view);
	}

}
