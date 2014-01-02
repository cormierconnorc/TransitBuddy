package com.connorsapps.pathfinder;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A flexible alert dialog for reporting error messages
 * @author Connor Cormier
 *
 */
public class CustomAlertDialog extends DialogFragment
{
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		String title = this.getArguments().getString("title");
		String message = this.getArguments().getString("message");
		String button = this.getArguments().getString("button");
		
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		
		builder.setPositiveButton(button, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				//Do nothing, it's of no consequence.
			}
		});
		
		
		
		return builder.create();
	}
}
