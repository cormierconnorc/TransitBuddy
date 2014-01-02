package com.connorsapps.pathfinder;

public class BrokenPathException extends RuntimeException
{
	public BrokenPathException()
	{
		
	}
	
	public BrokenPathException(String msg)
	{
		super(msg);
	}
}
