package com.connorsapps.pathfinder;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Dialog allowing user to select an option
 * @author Connor Cormier
 *
 */
public class TransportDialog extends DialogFragment
{
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final MainActivity getYoActTogether = (MainActivity)this.getActivity();
		int modeOfTranspo = getYoActTogether.getRoutey().getModeOfTransportInt();
		
		String[] ops = {"Walking",  "Biking", "Driving"};
		
		ops[modeOfTranspo] += " (Current)";
		
		builder.setTitle("Choose your mode of transport")
				.setItems(ops, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						getYoActTogether.setModeOfTranportation(which);
					}
				});
	
		return builder.create();
	}
}