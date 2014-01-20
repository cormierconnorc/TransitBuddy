package com.connorsapps.pathfinder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
/**
 * Primary activity
 * @author Connor Cormier
 *
 */
public class MainActivity extends FragmentActivity implements OnClickListener,
		OnEditorActionListener,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener
{
	//Enable or disable debugging mode
	public static final boolean DEBUG = false;
	public static final String SAVE_FILE = "savedState.txt";
	public static final int API_REQUEST_DELAY = 500, MIN_API_TEXT_LENGTH = 3;
	private Button go;
	private AutoCompleteTextView input;
	private ToggleButton walking, transit, steps, map;
	private LocationClient magicLocationTool;
	//Need this to reset edittext
	private String lastEnteredDestination;
	private Router routey;
	private Path curBusPath, curWalkPath;
	//Allow UI update to more easily determine the recommended route
	private boolean walkingRecommended;
	private Map<Integer, View> inflatedViews;
	private static final int AGENCY_SWITCH_CODE = 0;
	private long lastAutoCompleteRequestTime;
	private int showBusRouteSetting, mapTypeSetting;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		go = (Button)findViewById(R.id.goButton);
		input = (AutoCompleteTextView)findViewById(R.id.destination);
		walking = (ToggleButton)findViewById(R.id.walkingButton);
		transit = (ToggleButton)findViewById(R.id.transitButton);
		steps = (ToggleButton)findViewById(R.id.stepsListButton);
		map = (ToggleButton)findViewById(R.id.mapViewButton);
		
		//Set listeners
		go.setOnClickListener(this);
		walking.setOnClickListener(this);
		transit.setOnClickListener(this);
		steps.setOnClickListener(this);
		map.setOnClickListener(this);
		
		//Set the listener to undo input color change on click
		input.setOnClickListener(new UndoChangesListener());
		
		//Set input enter text:
		input.setOnEditorActionListener(this);
		
		//Set input text change listener
		input.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable arg0)
			{
				//Do nothing
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
				//Do nothing
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count)
			{
				//Ensure that the location client is already connected
				if (!MainActivity.this.magicLocationTool.isConnected())
					MainActivity.this.magicLocationTool.connect();
				
				Location loc = MainActivity.this.magicLocationTool.getLastLocation();
				long timeSinceLastRequest = System.currentTimeMillis() - lastAutoCompleteRequestTime;
				if (loc != null && timeSinceLastRequest >= API_REQUEST_DELAY && s.toString().length() >= MIN_API_TEXT_LENGTH)
				{
					lastAutoCompleteRequestTime = System.currentTimeMillis();
					LatLng about = new LatLng(loc.getLatitude(), loc.getLongitude());
					AutoCompleteTask task = new AutoCompleteTask(MainActivity.this, about, input);
					task.execute(s.toString());
				}
			}
			
		});
		
		//Set the listener for autocomplete items
		input.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View childView, int position, long id)
			{
				MainActivity.this.analyzeRoute((PlaceReference)input.getAdapter().getItem(position));
			}
			
		});
		
		//Disable input on all buttons until a valid destination is entered
		lockButtons();
		
		//Create the location client
		if (!DEBUG)
			magicLocationTool = new LocationClient(this, this, this);
		else
		{
			magicLocationTool = new LocationClient(this, this, this){
				@Override
				public Location getLastLocation()
				{
					Location loc = super.getLastLocation();
					if (loc != null)
					{
						//Mechanical engineering building at UVa
						//loc.setLatitude(38.032569);
						//loc.setLongitude(-78.511231);
						//Woody house
						loc.setLatitude(38.0326);
						loc.setLongitude(-78.5155167);
					}
					return loc;
				}
			};
		}
		
		//Create the router
		routey = new Router();
		
		//Create the inflated views map
		inflatedViews = new HashMap<Integer, View>();
		
		//Load the saved agency
		loadSavedState();
		
		//By default, show entire bus routes but do not enable pathfinding
		//Set to two to enable pathfinding, anything else to not draw bus routes
		this.showBusRouteSetting = 1;
		
		//Show hybrid map by default
		this.mapTypeSetting = 1;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem touchMe)
	{
		switch (touchMe.getItemId())
		{
		case R.id.selectModeOfTransport:
			createModeDialog();
			break;
		case R.id.selectNumStops:
			createSelectionDialog();
			break;
		case R.id.changeAgency:
			changeAgency();
			break;
		case R.id.selectBusRouteSetting:
			createBusSelectionDialog();
			break;
		case R.id.changeMapType:
			createMapChangeDialog();
			break;
		}
		
		return true;
	}
	
	public void loadSavedState()
	{
		LoadInfoTask task = new LoadInfoTask(this);
		task.execute(SAVE_FILE);
	}
	
	public void changeAgency()
	{
		Intent intent = new Intent(this, AgencySelectionActivity.class);
		//Put in the current agency
		intent.putExtra("curAgency", getRoutey().getTransitAgency());
		this.startActivityForResult(intent, AGENCY_SWITCH_CODE);
	}
	
	/**
	 * Write all relevant save info to the save file
	 */
	public void saveState()
	{
		this.writeSaveFile(getRoutey().getTransitAgency(), this.showBusRouteSetting, this.mapTypeSetting, this.getRoutey().getNumStops(), this.getRoutey().getModeOfTransportInt());
	}
	
	/**
	 * Save the settings to the save file
	 * @param newAgency
	 */
	public void writeSaveFile(Integer... parts)
	{
		SaveInfoTask task = new SaveInfoTask(this);
		task.execute(parts);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		//Log.d("debug", "Result = " + resultCode);
		if (requestCode == AGENCY_SWITCH_CODE && resultCode == RESULT_OK)
		{
			int newAgency = data.getIntExtra("newAgency", -1);
			this.getRoutey().setTransitAgency(newAgency);
			//Write the new agency to the save file
			this.saveState();
		}
	}
	
	/**
	 * Create a dialog allowing the user to select the number of stops to check
	 */
	public void createSelectionDialog()
	{
		SelectionDialog dial = new SelectionDialog();
		dial.show(getSupportFragmentManager(), "selecty");
	}
	
	/**
	 * Create a dialog that allows the user to change whether bus routes are shown
	 */
	public void createBusSelectionDialog()
	{
		BusSelectionDialog dial = new BusSelectionDialog();
		dial.show(getSupportFragmentManager(), "busSelecty");
	}
	
	/**
	 * Create a dialog that allows the user to change the type of map being used
	 */
	public void createMapChangeDialog()
	{
		MapChangeDialog dial = new MapChangeDialog();
		dial.show(getSupportFragmentManager(), "mapSelecty");
	}
	
	/**
	 * Create mode of transport selection dialog
	 */
	public void createModeDialog()
	{
		TransportDialog port = new TransportDialog();
		port.show(getSupportFragmentManager(), "tranny");
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		//Go button case
		case R.id.goButton:
			analyzeRoute();
			return;
		case R.id.walkingButton:
			setWalking();
			break;
		case R.id.transitButton:
			setTransit();
			break;
		case R.id.stepsListButton:
			setStepsView();
			break;
		case R.id.mapViewButton:
			setMapView();
			break;
		}
		
		updateUI();
	}
	
	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{
			analyzeRoute();
			return true;
		}
		return false;
	}
	
	/**
	 * Analyze a route based on autocomplete place
	 * @param ref
	 */
	public void analyzeRoute(PlaceReference ref)
	{
		LatLng loc = prepForAnalysis();
		
		if (loc == null)
			return;
		
		PlaceReferenceAnalysisTask prat = new PlaceReferenceAnalysisTask(this, loc);
		prat.execute(ref);
	}
	
	/**
	 * Interact with back end to get route information. Start by finding place location.
	 * If multiple possible places, prompt user to select. Otherwise, move on to finding route.
	 */
	public void analyzeRoute()
	{
		LatLng loc = prepForAnalysis();
		
		if (loc == null)
			return;
		
		GeocodeFinderTask gTask = new GeocodeFinderTask(this, loc);
		gTask.execute(lastEnteredDestination);
	}
	
	/**
	 * Prepare to analyze route
	 * @return User's current location
	 */
	public LatLng prepForAnalysis()
	{
		//Lock input
		lockButtons();
		
		hideKeyboard();
		
		//Interact with back end based on user input.
		lastEnteredDestination = this.input.getText().toString();
		
		Location myLoc = magicLocationTool.getLastLocation();
		
		//Show user error if location could not be found. Then return.
		if (myLoc == null)
		{
			showAlertDialog("Failed to get your location.", "This is your fault somehow.", "I accept responsibility");
			return null;
		}
		
		return new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
	}
	
	/**
	 * Hide the soft keyboard
	 */
	public void hideKeyboard()
	{
		InputMethodManager imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
	}
	
	/**
	 * Find the routes to a given destination
	 * @param dest
	 */
	public void findRoutes(LatLng loc, LatLng dest)
	{	
		//Tell the user that the application is working:
		this.showNotificationText("Finding route. Please wait.");
		
		//Start the async task that will handle the calculations
		RouteCalculatorTask rTask = new RouteCalculatorTask(this, loc, dest);
		rTask.execute();
	}
	
	/**
	 * Invoked by RouteCalculatorTask to signify that the entered destination is not valid
	 */
	public void reportInvalidDestination()
	{
		input.setTextColor(Color.RED);
		input.setText("Entered destination is invalid");
	}
	
	public void updateRoutes(Path bus, Path walk)
	{
		this.curBusPath = bus;
		this.curWalkPath = walk;
		
		//Now that the routes have been updated, update the text reflecting the user's transport preference
		this.walking.setTextOn(routey.getModeOfTransport());
		this.walking.setTextOff(routey.getModeOfTransport());
		
		
		
		//Log.v("debug", "Bus path:\n" + bus + "\nWalking path:\n" + walk);
		
		provideRecommendation();
	}
	
	public void provideRecommendation()
	{
		//Recommend the other path is one is null
		if (curBusPath == null && curWalkPath == null)
		{
			showAlertDialog("No route found.", "This is probably Google's fault. Their API provides the directions.", "Damn it, Google");
			//Clear the view window so it's clear that things aren't working
			clearViews();
			return;
		}
		else if (curBusPath == null)
		{
			setWalking();
			setStepsView();
			//Alert the user
			showAlertDialog("No bus route found", "You can still walk!", "Fine, I'll take the other route");
			//Unlock the buttons
			freeButtons();
			//Disable the appropriate button
			transit.setClickable(false);
			this.walkingRecommended = true;
		}
		else if (curWalkPath == null)
		{
			setTransit();
			setStepsView();
			//Alert the user
			showAlertDialog("No walking route found", "You can still take the bus!", "Fine, I'll take the other route");
			//Unlock the buttons
			freeButtons();
			//Disable the appropriate button
			walking.setClickable(false);
			this.walkingRecommended = false;
		}
		else
		{
			//Both routes work! Hooray! Recommend whichever one's fastest
			if (curBusPath.getDuration() < curWalkPath.getDuration())
			{
				setTransit();
				setStepsView();
				this.walkingRecommended = false;
			}
			else
			{
				setWalking();
				setStepsView();
				this.walkingRecommended = true;
			}
			
			freeButtons();
		}
		
		updateUI();
	}
	
	/**
	 * Display a dialog with set title and message
	 * @param title
	 * @param message
	 * @param button
	 */
	public void showAlertDialog(String title, String message, String button)
	{
		CustomAlertDialog d = new CustomAlertDialog();
		Bundle b = new Bundle();
		b.putString("title", title);
		b.putString("message", message);
		b.putString("button", button);
		d.setArguments(b);
		d.show(getSupportFragmentManager(), "currentAlertDialog");
	}
	
	public void freeButtons()
	{
		changeButtonState(true);
	}
	
	public void lockButtons()
	{
		walking.setChecked(false);
		transit.setChecked(false);
		steps.setChecked(false);
		map.setChecked(false);
		changeButtonState(false);
	}
	
	public void changeButtonState(boolean newState)
	{
		walking.setClickable(newState);
		transit.setClickable(newState);
		steps.setClickable(newState);
		map.setClickable(newState);
	}
	
	public void setWalking()
	{
		walking.setChecked(true);
		transit.setChecked(false);
	}
	
	public void setTransit()
	{
		transit.setChecked(true);
		walking.setChecked(false);
	}
	
	public void setStepsView()
	{
		steps.setChecked(true);
		map.setChecked(false);
	}
	
	public void setMapView()
	{
		map.setChecked(true);
		steps.setChecked(false);
	}
	
	/**
	 * Update the UI based on the current state of the toggle buttons
	 */
	public void updateUI()
	{
		boolean isRecommended;
		Path curPath;
		
		if (this.walking.isChecked())
		{
			curPath = this.curWalkPath;
			isRecommended = this.walkingRecommended;
		}
		else
		{
			curPath = this.curBusPath;
			isRecommended = !this.walkingRecommended;
		}
		
		//Do not continue if the current path is null
		if (curPath == null)
			return;
		
		//Display steps list
		if (this.steps.isChecked())
		{
			ListView list = inflateListView();
			
			List<String> steps = new ArrayList<String>();
			
			steps.add((isRecommended ? "Recommended " : "Not recommended ") + "route. Estimated arrival at " + getClockTime(curPath.getEndTime()));
			
			for (Step s : curPath.getDirections())
			{
				String stepString = s.getDescription() + "\n\t" + formatTime(s.getDuration()) + "\n\t" + formatDistance(s.getMeters());
				steps.add(stepString);
			}
			
			ArrayAdapter<String> stepList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, steps);
			
			list.setAdapter(stepList);
		}
		//Display map
		else
		{
			GoogleMap map = inflateMapView();
			
			//Add all polylines to this map
			for (PolylineOptions p : curPath.getWalkPolylines())
				map.addPolyline(p);
			
			List<PolylineOptions> busPolies;
			
			if (this.showBusRouteSetting == 1)
				busPolies = curPath.getSegmentPolylines(routey);
			else if (this.showBusRouteSetting == 2)
				busPolies = curPath.getRoutePolylines(routey);
			else
				busPolies = null;
			
			if (busPolies != null)
				for (PolylineOptions ops : busPolies)
					map.addPolyline(ops);
			
			//Add all markers to this map
			for (MarkerOptions m : curPath.getMarkers())
				map.addMarker(m);
		}
		
	}
	
	/**
	 * Format duration into hours, minutes, and seconds
	 * @param durationMs duration in milliseconds
	 * @return
	 */
	public String formatTime(long durationMs)
	{
		int seconds = (int)(durationMs / 1000);
		int minutes = seconds / 60;
		seconds %= 60;
		int hours = minutes / 60;
		minutes %= 60;
		
		return (hours == 0 ? "" : hours + " hours, ") + (hours == 0 && minutes == 0 ? "" : minutes + " minutes and ") + seconds + " seconds"; 
	}
	
	/**
	 * Format distance into miles
	 * @param meters
	 * @return
	 */
	public String formatDistance(int meters)
	{
		//Booo, stupid customary system.
		int feet = (int)(meters * 3.28084);
		//Yeah, that's a sensible conversion factor
		double miles = (double)feet / 5280;
		
		return String.format("%.2f miles", miles);
	}
	
	public void clearViews()
	{
		ViewGroup g = (ViewGroup)findViewById(R.id.selectedContentView);
		g.removeAllViews();
	}
	
	public GoogleMap inflateMapView()
	{
		showLayout(R.layout.map_layout);
		GoogleMap map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();

		map.setMyLocationEnabled(true);
		
		switch (this.mapTypeSetting)
		{
		case 0:
			map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			break;
		case 1:
			map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
			break;
		case 2:
			map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		}
		//Clear out everything that was already on the map
		map.clear();

		//Set to use proper info window
		map.setInfoWindowAdapter(new InfoWindowAdapter(){

			@Override
			public View getInfoContents(Marker arg0)
			{
				//ListView to hold steps
				ListView v = new ListView(MainActivity.this);
				ArrayAdapter<String> adapt = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, arg0.getSnippet().split("\n"));
				v.setAdapter(adapt);
				return v;
			}

			@Override
			public View getInfoWindow(Marker arg0)
			{
				//Do nothing
				return null;
			}
			
		});
		
		Location loc;
		if ((loc = magicLocationTool.getLastLocation()) != null)
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 15));
		
		return map;
	}
	
	public void inflatePlaces(final LatLng about, final Place[] places)
	{
		ListView v = inflateListView();
		
		final List<String> placesList = new ArrayList<String>(places.length + 1);
		
		placesList.add("Select your desired destination:");
		
		for (Place p : places)
			placesList.add(p.toString());
		
		ArrayAdapter<String> placesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, placesList);
		
		//Set a listener to notify the proper method when a choice is selected
		v.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View childView, int position, long id)
			{
				//Don't allow user to click prompt
				if (position == 0)
					return;
				//When the item is selected, start finding routes.
				MainActivity.this.findRoutes(about, places[position - 1].getLocation());
			}
			
		});
		
		v.setAdapter(placesAdapter);
	}
	
	/**
	 * Inflate the list view
	 * @return Visible ListView object
	 */
	public ListView inflateListView()
	{
		ListView v = (ListView)showLayout(R.layout.list_layout).findViewById(R.id.stepsList);
		//Remove item click listener
		v.setOnItemClickListener(null);
		return v;
	}
	
	/**
	 * Inflate a layout and show it in the selected content window
	 * @param layout
	 * @return
	 */
	public View showLayout(int layout)
	{
		View v;
		ViewGroup g = (ViewGroup)findViewById(R.id.selectedContentView);
		
		if (!this.inflatedViews.containsKey(layout))
		{
			LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(layout, null);
			inflatedViews.put(layout, v);
		}
		else
			v = this.inflatedViews.get(layout);
		g.removeAllViews();
		g.addView(v, 0);
		return v;
	}
	
	public void showNotificationText(String text)
	{
		ViewGroup g = (ViewGroup)this.findViewById(R.id.selectedContentView);
		g.removeAllViews();
		TextView view = new TextView(this);
		view.setText(text);
		view.setTextColor(Color.RED);
		g.addView(view, 0);
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
	 * Start location updates when this activity starts
	 */
	@Override
	public void onStart()
	{
		super.onStart();
		
		//Connect
		if (!magicLocationTool.isConnected())
			magicLocationTool.connect();
	}
	
	/**
	 * Stop location updates when this activity stops
	 */
	@Override
	public void onStop()
	{
		//Disconnect from play services
		if (magicLocationTool.isConnected())
			magicLocationTool.disconnect();
		
		super.onStop();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result)
	{
		//Attempt to have Google Play Services try again
		if (result.hasResolution())
		{
			try
			{
				//Tell it to resolve a problem with this activity
				//Request code is 9000 for this service.
				result.startResolutionForResult(this, 9000);
			}
			catch (IntentSender.SendIntentException e)
			{
				e.printStackTrace();
			}
		}
		
		//If there is no resolution, break down in tears.
		//this.cryHeavily()
	}

	@Override
	public void onConnected(Bundle arg0) {/*No need to do anything here*/}

	@Override
	public void onDisconnected() {/*User still doesn't need to know*/}
	
	public class UndoChangesListener implements OnClickListener
	{

		@Override
		public void onClick(View v)
		{
			//If it needs to be reset, do so.
			if (v.getId() == R.id.destination && input.getCurrentTextColor() == Color.RED)
			{
				input.setTextColor(Color.BLACK);
				input.setText(lastEnteredDestination);
			}
		}
		
	}

	public Router getRoutey()
	{
		return routey;
	}

	public void setRoutey(Router routey)
	{
		this.routey = routey;
	}
	
	public void setNumStops(int newNum)
	{
		if (getRoutey().getNumStops() != newNum)
		{
			getRoutey().setNumStops(newNum);
			this.saveState();
		}
	}
	
	/**
	 * Set the router's mode of transportation for comparison.
	 * @param newMode
	 */
	public void setModeOfTranportation(int newMode)
	{
		if (routey.getModeOfTransportInt() != newMode)
		{
			routey.setModeOfTransport(newMode);
			this.saveState();
		}
	}
	
	public int getShowBusRouteSetting()
	{
		return this.showBusRouteSetting;
	}
	
	public void setShowBusRouteSetting(int newSetting)
	{
		if (this.showBusRouteSetting != newSetting)
		{
			this.showBusRouteSetting = newSetting;
			this.saveState();
			this.updateUI();
		}
	}
	
	public int getMapTypeSetting()
	{
		return this.mapTypeSetting;
	}
	
	public void setMapTypeSetting(int newSetting)
	{
		if (this.mapTypeSetting != newSetting)
		{
			this.mapTypeSetting = newSetting;
			this.saveState();
			this.updateUI();
		}
	}
}
