package com.connorsapps.pathfinder;

import android.util.Log;

/**
 * Representation of a bus route
 * @author Connor Cormier
 *
 */
public class Route
{
	public static final String TRANSPARENCY = "80";
	private final String name;
	private final int color;
	private final long id;
	
	/**
	 * @param name
	 * @param hexColor Hex representation of the color of this route. DOES NOT INCLUDE ALPHA CHANNEL.
	 * @param id
	 */
	public Route(String name, String hexColor, long id)
	{
		this(name, (int)Long.parseLong(TRANSPARENCY + hexColor, 16), id);
	}
	
	/**
	 * @param name
	 * @param color long representation of color
	 * @param id
	 */
	public Route(String name, int color, long id)
	{
		this.name = name;
		this.color = color;
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public int getColor()
	{
		return color;
	}
	
	public long getId()
	{
		return id;
	}
	
	public String toString()
	{
		return name;
	}
}
