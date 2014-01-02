package com.connorsapps.pathfinder;

import java.io.PrintWriter;

import android.os.AsyncTask;

/**
 * Task to handle saving info to a file
 * @author Connor Cormier
 *
 */
public class SaveInfoTask extends AsyncTask<Integer, Object, Boolean>
{
	private MainActivity callback;
	
	public SaveInfoTask(MainActivity callback)
	{
		this.callback = callback;
	}

	@Override
	protected Boolean doInBackground(Integer... params)
	{
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(callback.openFileOutput(MainActivity.SAVE_FILE, 0));
			
			for (int i : params)
				out.print(i + " ");
			
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			if (out != null)
				out.close();
		}

	}

}
