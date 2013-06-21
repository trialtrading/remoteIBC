package com.IBConnector.Data;
// reza //
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TimeZone;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.UnderComp;


public class IBDataConnector implements EWrapper{
	public static final int DEFAULT_CLIENT_ID = 0;
	public static final int DEFAULT_PORT = 7496;
	public static final String DEFAULT_HOST = "localHost";
	EClientSocket client = new EClientSocket(this);
	int uniqueId = 1;
	HashMap<Integer, BarHolder> data = new HashMap<Integer, BarHolder>();
	HashMap<Integer, String> symbols = new HashMap<Integer, String>();
	HashMap<String, Integer> tickerIds = new HashMap<String, Integer>();
	private static int msgDisplayLevel = 0;
	private final long dayStartTimeSec = getDayStartTimeSec();

	
	/*
	 * -----------------------------------------------------
	 *     CONNECT, DISCONNECT and isConnected Methods
	 * -----------------------------------------------------
	 */
	public void connect()									{	connect(DEFAULT_CLIENT_ID, DEFAULT_PORT, DEFAULT_HOST);	}
	public void connect(int clientId) 						{	connect(clientId, DEFAULT_PORT, DEFAULT_HOST);			}
	public void connect(int clientId, int port)				{	connect(clientId, port, DEFAULT_HOST);					}
	public void connect(int clientId, int port, String host){	client.eConnect(host, port, clientId);					}
	public void disconnect()								{ 	client.eDisconnect();			}
	public boolean isConnected() 							{ 	return client.isConnected();	}
	
	
	/*
	 * -----------------------------------------------------
	 *                Subscription Methods
	 * -----------------------------------------------------
	 */
	
	/**
	 * called by user to subscribe to realTimeBars for a symbol
	 * @param symbol
	 */
	public void subscribeToSymbol(String symbol)
	{
		if(tickerIds.get(symbol) == null)
		{
			int tickerId = uniqueId++;
			Contract contract = getStockContract(symbol);
			client.reqRealTimeBars(tickerId, contract, 5, "TRADES", true);
			tickerIds.put(symbol, tickerId);
			symbols.put(tickerId, symbol);
		}
	}
	
	/**
	 * called by user to unsubscribe from realTimeBars for a symbol
	 * @param symbol
	 */
	public void unsubscribeFromSymbol(String symbol)
	{
		Integer tickerId = tickerIds.get(symbol);
		if(tickerId != null)
		{
			client.cancelRealTimeBars(tickerIds.get(symbol));
			data.remove(tickerIds.get(symbol));
			symbols.remove(tickerIds.get(symbol));
			tickerIds.remove(symbol);
		}
	}
	
	/**
	 * called by Interactive brokers to notify connector when a new Bar is received
	 */
	@Override
	public void realtimeBar(int reqId, long time, double open, double high, double low,
			double close, long volume, double wap, int count) {
		BarHolder holder = data.get(reqId);
		if(holder == null) holder = new BarHolder(symbols.get(reqId));
		Bar bar = new Bar(holder.symbol, reqId, time, open, high, low, close, volume, wap, count);
		holder.addBar(bar);
		data.put(reqId, holder);
	}
	
	/**
	 * creates a new IB contract that gets executed on the IB SMART exchange, in USD, and is a Stock
	 * @param equity symbol
	 * @return interactive brokers Contract
	 */
	private Contract getStockContract(String symbol)
	{
		Contract contract = new Contract();
		contract.m_symbol = symbol;
		contract.m_exchange = "SMART";
		contract.m_currency = "USD";
		contract.m_secType = "STK";
		return contract;
	}
	
	/*
	 * -----------------------------------------------------
	 *            Methods to Retrieve Price Data
	 * -----------------------------------------------------
	 */
	public double getOpenPrice(String symbol, int hr, int min, int sec)
	{
		Bar bar = getBar(symbol, hr, min, sec);
		return (bar != null) ? bar.open : -1;
	}
	
	public double getHighPrice(String symbol, int hr, int min, int sec)
	{
		Bar bar = getBar(symbol, hr, min, sec);
		return (bar != null) ? bar.high : -1;
	}
	public double getLowPrice(String symbol, int hr, int min, int sec)
	{
		Bar bar = getBar(symbol, hr, min, sec);
		return (bar != null) ? bar.low : -1;
	}
	public double getClosePrice(String symbol, int hr, int min, int sec)
	{
		Bar bar = getBar(symbol, hr, min, sec);
		return (bar != null) ? bar.close : -1;
	}
	public long getVolume(String symbol, int hr, int min, int sec)
	{
		Bar bar = getBar(symbol, hr, min, sec);
		return (bar != null) ? bar.volume : -1;
	}
	
	private Bar getBar(String symbol, int hr, int min, int sec)
	{
		Integer tickerId = tickerIds.get(symbol);
		if(tickerId != null && data.get(tickerId) != null)
		{
			Bar bar = data.get(tickerId).getBar(dayStartTimeSec + hr * 3600 + min * 60 + sec);
			if(bar != null) return bar;
			else
			{
				System.out.printf("Gap In RealTimeBars - Missing bar for %d:%d:%d %n", hr, min, sec);
				return null;
			}
		}
		else
		{
			System.out.println("Requested data for unsubscribed Ticker - Subscribe first, then request data");
			return null;
		}
	}
	
	
	/*
	 * -----------------------------------------------------
	 *            Message Display preferences
	 * -----------------------------------------------------
	 */
	public int getMsgDisplayLevel() 					{	return IBDataConnector.msgDisplayLevel;				}
	public void setMsgDisplayLevel(int msgDisplayLevel) { 	IBDataConnector.msgDisplayLevel = msgDisplayLevel;	}
	
	@Override
	public void connectionClosed() {
		if(msgDisplayLevel < 1)	System.out.println(" [API.connectionClosed] Closed connection with TWS");
	}

	@Override
	public void error(Exception e) {
		if(msgDisplayLevel < 2)
			System.out.println((new StringBuilder(" [API.msg3] ")).append("Exception").append(" {").append(e.getMessage()).append(", ").append(e.getLocalizedMessage()).append("}").toString());
		
	}

	@Override
	public void error(String str) {
		if(msgDisplayLevel < 2)
			System.out.println((new StringBuilder(" [API.msg1] ")).append(str).toString());
	}

	@Override
	public void error(int data1, int data2, String str) {
		if(msgDisplayLevel < 1 || msgDisplayLevel == 1 && !str.contains("Market data farm connection is OK:"))
			System.out.println((new StringBuilder(" [API.msg2] ")).append(str).append(" {").append(data1).append(", ").append(data2).append("}").toString());
	}

	
	/*
	 * -----------------------------------------------------
	 *            Helper and Debug methods
	 * -----------------------------------------------------
	 */
	/**
	 * retrieves the number of seconds since Jan 1st, 1970 until 12:00AM today in the New York time zone
	 * @return numberOfSeconds
	 */
	public long getDayStartTimeSec() {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
	    int year = cal.get(Calendar.YEAR);
	    int month = cal.get(Calendar.MONTH);
	    int day = cal.get(Calendar.DATE);
	    cal.set(year, month, day, 0, 0, 0);
	    return cal.getTimeInMillis() / 1000;
	}
	
	
	/**
	 * example debug code to connect, subscribe to realtime bars for GOOG, and print data, then disconnect;
	 * @param does not take input
	 */
	public static void main(String[] args)
	{
		IBDataConnector test = new IBDataConnector();
		test.connect();
		test.waitForUserInput();
		test.subscribeToSymbol("GOOG");
		test.waitForUserInput();
		test.printData();
		test.waitForUserInput();
		test.disconnect();
		test.waitForUserInput();
	}
	/**
	 * prints all recorded bars for all data values that were subscribed
	 */
	public void printData()	{	for(BarHolder barHolder: data.values()) barHolder.printAllBars();	}
	
	/**
	 * simple scanner that allows the user to control the pace of the program
	 */
	public void waitForUserInput()	{	if((new Scanner(System.in)).nextLine() != null) System.out.print("");	}
	
	
	
	/*
	 *  ---------------------------------------------------------
	 *     ALL OTHER METHOD SIGNATURES BELOW ARE PLACEHOLDERS
	 *  ---------------------------------------------------------
	 */
	
	@Override
	public void accountDownloadEnd(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bondContractDetails(int arg0, ContractDetails arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commissionReport(CommissionReport arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void contractDetails(int arg0, ContractDetails arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void contractDetailsEnd(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void currentTime(long arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deltaNeutralValidation(int arg0, UnderComp arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execDetails(int arg0, Contract arg1, Execution arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execDetailsEnd(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fundamentalData(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void managedAccounts(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void marketDataType(int arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void nextValidId(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void openOrder(int arg0, Contract arg1, Order arg2, OrderState arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void openOrderEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void orderStatus(int arg0, String arg1, int arg2, int arg3,
			double arg4, int arg5, int arg6, double arg7, int arg8, String arg9) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void receiveFA(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scannerData(int arg0, int arg1, ContractDetails arg2,
			String arg3, String arg4, String arg5, String arg6) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scannerDataEnd(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scannerParameters(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickEFP(int arg0, int arg1, double arg2, String arg3,
			double arg4, int arg5, String arg6, double arg7, double arg8) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickGeneric(int arg0, int arg1, double arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickOptionComputation(int arg0, int arg1, double arg2,
			double arg3, double arg4, double arg5, double arg6, double arg7,
			double arg8, double arg9) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickPrice(int arg0, int arg1, double arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickSize(int arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickSnapshotEnd(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickString(int arg0, int arg1, String arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAccountTime(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAccountValue(String arg0, String arg1, String arg2,
			String arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateMktDepth(int arg0, int arg1, int arg2, int arg3,
			double arg4, int arg5) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateMktDepthL2(int arg0, int arg1, String arg2, int arg3,
			int arg4, double arg5, int arg6) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNewsBulletin(int arg0, int arg1, String arg2, String arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updatePortfolio(Contract arg0, int arg1, double arg2,
			double arg3, double arg4, double arg5, double arg6, String arg7) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void historicalData(int arg0, String arg1, double arg2, double arg3,
			double arg4, double arg5, int arg6, int arg7, double arg8,
			boolean arg9) {
		// TODO Auto-generated method stub
		
	}


}
