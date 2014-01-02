package com.connorsapps.pathfinder;
/**
 * Representation of an Arrival event
 * @author Connor Cormier
 *
 */
public class Arrival implements Comparable<Arrival>
{
	private final long stopId, timestamp, vehicleId, routeId;
	
	public Arrival(long stopId, long timestamp, long vehicleId, long routeId)
	{
		this.stopId = stopId;
		this.timestamp = timestamp;
		this.vehicleId = vehicleId;
		this.routeId = routeId;
	}

	public long getStopId()
	{
		return stopId;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public long getVehicleId()
	{
		return vehicleId;
	}
	
	public boolean equals(Object o)
	{
		return (o instanceof Arrival) && ((Arrival)o).stopId == stopId && ((Arrival)o).timestamp == timestamp && ((Arrival)o).vehicleId == vehicleId;
	}
	
	public String toString()
	{
		return "Arrival[stopId = " + stopId + ", timestamp = " + timestamp + ", vehicleId = " + vehicleId + "]";
	}
	
	public int compareTo(Arrival other)
	{
		//Natural order is based on arrival time:
		return (new Long(this.timestamp)).compareTo(other.timestamp);
	}

	public long getRouteId()
	{
		return routeId;
	}
}
