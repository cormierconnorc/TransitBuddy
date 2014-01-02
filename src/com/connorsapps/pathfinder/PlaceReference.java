package com.connorsapps.pathfinder;

public class PlaceReference
{
	private final String name;
	private final String reference;
	
	public PlaceReference(String name, String reference)
	{
		this.name = name;
		this.reference = reference;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getReference()
	{
		return reference;
	}
	
	public String toString()
	{
		return name;
	}
}
