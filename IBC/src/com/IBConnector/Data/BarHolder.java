package com.IBConnector.Data;


import java.util.TreeMap;


/**
 * barHolder is a data structure used to hold bar updates from Interactive brokers
 * @author dylancooke34
 *
 */
public class BarHolder {
	
	public final String symbol;
	TreeMap<Long, Bar> barData = new TreeMap<Long, Bar>();
	
	public BarHolder(String symbol) {	this.symbol = symbol;	}
	
	/**
	 * updates the barHolder with the most recent bar
	 * @param bar
	 */
	public void addBar(Bar bar)	{	barData.put(bar.time, bar);						}
	
	/**
	 * prints all recorded bars stored in the barHolder
	 */
	public void printAllBars()	{	for(Bar bar: barData.values())	bar.print();	}

	/**
	 * retrieves a sorted array of all the bars which have been received from IB
	 * @return sorted array of Bars
	 */
	public Bar[] getSortedBars() {
		return barData.values().toArray(new Bar[barData.size()]);
	}
	
	/**
	 * retrieves the most recently updated bar
	 * @return most recent bar
	 */
	public Bar getMostRecentBar() 	{	return barData.get(barData.lastKey());	}
	
	/**
	 * retrieves bar for a given time in seconds
	 * @param barTimeSec
	 * @return requested bar
	 */
	public Bar getBar(long barTimeSec) {	return barData.get(barTimeSec - barTimeSec % 5);	}
	

}
