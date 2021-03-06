package com.connorsapps.pathfinder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Dialog that allows users to change if bus routes are shown on the map or not
 * @author Connor Cormier
 *
 */
public class MapChangeDialog extends DialogFragment
{	
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final MainActivity act = (MainActivity)this.getActivity();
		
		//Get current setting
		int setting = act.getMapTypeSetting();
		
		String[] ops = {"Normal", "Hybrid", "Satellite"};
		
		ops[setting] += " (Current)";
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setTitle("Select Map Type")
				.setItems(ops, new OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						act.setMapTypeSetting(which);
					}
					
				});
		
		return builder.create();
	}
}