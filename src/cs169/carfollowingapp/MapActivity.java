package cs169.carfollowingapp;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;



public class MapActivity extends FragmentActivity {

    protected GoogleMap map;
    /* Chris: I moved this from BradcastActivity to here since handleError
       needs these variables */
    protected String myUsername;
    protected String myPassword;
    private Marker myMarker;
    
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

    public Marker plot(LatLng coord) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 13));
        return map.addMarker(new MarkerOptions().position(coord));
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
