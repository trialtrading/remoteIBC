package com.IBConnector.Data;
// testing git commit and push functionality
public class Bar {
	
	public final String symbol;
	public final int reqId;
	public final long time;
	public final double open;
	public final double high;
	public final double low;
	public final double close;
	public final long volume;
	public final double wap;
	public final int count;
	
	public Bar(String symbol, int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count)
	{
		this.symbol = symbol;
		this.reqId = reqId;
		this.time = time;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
		this.wap = wap;
		this.count = count;
	}
	
	public void print()
	{
		System.out.printf("ReqId: %d, Time: %d, Open: %.2f, High: %.2f, Low: %.2f, Close: %.2f, Vol: %d , WAP: %.2f, Count: %d %n",
				reqId, time, open, high, low, close, volume, wap, count);
	}
	
	@Override
	public String toString()
	{
		return "ReqId: " + reqId + ", Time: " + time + ", Open: " + open + ", High: " + high + ", Low: " + low +
				", Close: " + close + ", Volume: " + volume + ", WAP: " + wap + ", Count: " + count;
	}

}
