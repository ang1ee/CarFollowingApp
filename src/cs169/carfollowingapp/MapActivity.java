package cs169.carfollowingapp;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;



public class MapActivity extends FragmentActivity {

    protected GoogleMap map;
    /* Chris: I moved this from BradcastActivity to here since handleError
       needs these variables */
    protected String myUsername;
    protected String myPassword;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    public void plot(ArrayList<LatLng> coords) {
        if (coords.size() < 1) {
            return;
        }
        
        // I don't know what 13 encodes yet... i'll make a class variable eventually
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(coords.get(0), 13));
        
        for (int i = 0; i < coords.size(); i++) {
            map.addMarker(new MarkerOptions().position(coords.get(i)));
        }
    }
    
	// Displays toast showing the text argument.
	protected void showToast(CharSequence text) {
	    Context context = getApplicationContext();
	    int duration = Toast.LENGTH_SHORT;
	    
	    Toast toast = Toast.makeText(context, text, duration);
	    toast.show();
	}
	
	/* -displays error using showToast
	 * -uses intent to navigate back to FrontPageActivity
	 * -finishes Broadcast/Follow activity
	 */
	protected void handleError(CharSequence text) {
		handleCleanup();
	    showToast(text);
	    Intent intent = new Intent(this, FrontPageActivity.class);
	    intent.putExtra(Constants.MY_U_KEY, myUsername);
	    intent.putExtra(Constants.MY_P_KEY, myPassword);
        startActivity(intent);
        finish();
	}
	
	/* Used to handle any additional clean up necessary
	 * when an error occurs.  Override in subclass to have
	 * special cleanup for the subclass.
	 */
	protected void handleCleanup() {
		// Implement in subclass
	}
    
}
