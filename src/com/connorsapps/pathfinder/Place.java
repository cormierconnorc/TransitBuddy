package com.connorsapps.pathfinder;

import com.google.android.gms.maps.model.LatLng;

public class Place
{
	private String name;
	private LatLng location;
	
	public Place(String name, LatLng location)
	{
		setName(name);
		setLocation(location);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public LatLng getLocation()
	{
		return location;
	}

	public void setLocation(LatLng location)
	{
		this.location = location;
	}
	
	public String toString()
	{
		return name;
	}
}
