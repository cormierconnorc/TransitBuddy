package com.connorsapps.pathfinder;

/**
 * Exception thrown when google maps can't find a geocode associated with a String
 * @author Connor Cormier
 *
 */
public class InvalidLocationException extends Exception
{
	private static final long serialVersionUID = -2901648459946740275L;

	public InvalidLocationException()
	{
		super();
	}
	
	public InvalidLocationException(String whatBroke)
	{
		super(whatBroke);
	}
}
