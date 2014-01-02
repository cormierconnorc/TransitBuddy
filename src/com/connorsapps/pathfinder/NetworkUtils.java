package com.connorsapps.pathfinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Just offloading a common utility method to a different class so I don't have to repeat it constantly.
 * @author Connor Cormier
 *
 */
public final class NetworkUtils
{
	private NetworkUtils() {}
	
	/**
	 * Get the text from a URL, used in this context for API calls
	 * Made thread-safe just to be sure
	 * @param url
	 * @return
	 */
	public synchronized static String getUrl(String url)
	{
		//Log.v("debug", "Getting URL: " + url);
		
		//If debugging, redirect queries to debugging server
		if (MainActivity.DEBUG && url.contains("http://feeds.transloc.com/2/"))
			url = "http://raspbi.mooo.com/Debugger/TransitDebugger?type=" + url.split("http://feeds.transloc.com/2/")[1].replace("?", "&");
		
		BufferedReader read = null;
		
		try
		{			
			//Open a connection with the request URL
			HttpURLConnection connection = (HttpURLConnection)(new URL(url).openConnection());
			
			//Hey guys! I'm Google Chrome. No, seriously! Nothing out of the ordinary here.
			//connection.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36");
		
			connection.setReadTimeout(10000);
			connection.setConnectTimeout(15000);
			connection.setRequestMethod("GET");
			connection.setDoInput(true);
			connection.connect();

			//Log.v("debug", "Got input");
			
			read = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			StringBuilder s = new StringBuilder();
			//Buffer to read into
			char[] buf = new char[8 * 1024];
			
			while (true)
			{
				int n = read.read(buf);
				if (n < 0)
					break;
				s.append(buf, 0, n);
			}
	
			return s.toString();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			//Return nothing so things don't break
			return "";
		}
		//Executed even after return to free up those precious system resources
		finally
		{
			if (read != null)
			{
				try
				{
					read.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
