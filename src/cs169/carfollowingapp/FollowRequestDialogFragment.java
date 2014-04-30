package cs169.carfollowingapp;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/* Dialog boxes for follow requests. 
 * Code heavily based on code from 
 * http://developer.android.com/guide/topics/ui/dialogs.html
 */
public class FollowRequestDialogFragment extends DialogFragment {
	BroadcastActivity bActivity;
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
    	assert (bActivity != null) : "bActivity was null";
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] followerTuple = bActivity.getFollowerTuple();
        String followerName = followerTuple[0];
        String followerMessage = followerTuple[1];
        String dialogString;
        if (followerMessage == "") {
        	dialogString = followerName + " would like to follow you";
        } else {
        	dialogString = followerName + ": " + followerMessage;
        }
        builder.setMessage(dialogString)
               .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   bActivity.invitationAccepted = true;
                	   bActivity.invitationResponse();
                	   //bActivity.showFollowRequestDialog();
                   }
               })
               .setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   bActivity.invitationAccepted = false;
                	   bActivity.invitationResponse();
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    
    public void setBActivity(BroadcastActivity ba) {
    	bActivity = ba;
    }
    
}
