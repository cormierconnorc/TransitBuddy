package com.connorsapps.pathfinder;

import com.google.android.gms.maps.model.LatLng;

/**
 * Immutable Stop object used to hold stop information
 * @author Connor Cormier
 *
 */
public class Stop
{
	private final long id;
	private final String name;
	private final LatLng loc;
	
	public Stop(long id, String name, LatLng location)
	{
		this.id = id;
		this.name = name;
		this.loc = location;
	}
	
	public long getId()
	{
		return id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public LatLng getLocation()
	{
		return loc;
	}
	
	public String toString()
	{
		return "Stop[id = " + id + ", name = " + name + ", location = " + loc + "]";
	}
	
	public boolean equals(Object o)
	{
		return (o instanceof Stop) && o.hashCode() == this.hashCode();
	}
	
	public int hashCode()
	{
		//Simple hashing implementation that adds together the hash codes of the fields. Note: doesn't rely on LatLng hashcode
		return (new Long(id)).hashCode() + name.hashCode() + (new Double(loc.latitude)).hashCode() + (new Double(loc.longitude)).hashCode();
	}
}
