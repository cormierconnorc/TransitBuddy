package com.connorsapps.pathfinder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Dialog that allows users to switch the number of stops the app checks
 * @author Connor Cormier
 *
 */
public class SelectionDialog extends DialogFragment
{	
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final MainActivity act = (MainActivity)this.getActivity();
		
		//Get current setting
		int numStops = act.getRoutey().getNumStops() - 1;
		
		String[] ops = {"1", "2", "3", "4", "5"};
		
		ops[numStops] += " (Current)";
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setTitle("Set number of stops to check")
				.setItems(ops, new OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						act.setNumStops(which + 1);
					}
					
				});
		
		return builder.create();
	}
}
