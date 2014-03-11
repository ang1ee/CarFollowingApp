package cs169.carfollowingapp;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;



public class MapActivity extends FragmentActivity {

    protected GoogleMap map;
    protected static final int SUCCESS = 1;
    protected static final int NO_SUCH_USER = -1;
    protected static final int USER_NOT_BROADCASTING = -2;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        this.map.setMyLocationEnabled(true);
        
        LatLng location = this.getLocation();
        ArrayList<LatLng> coords = new ArrayList<LatLng>();
        coords.add(location);
        this.plot(coords);
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
    
    public LatLng getLocation() {
        String defaultUser = "";
        return getLocation(defaultUser);
    }
    
    // TODO: make sure non-authenticated users can't get anyone's location
    /* 
     * username: who to obtain the location point from
     * returns the obtained latitude/longitude in the form of LatLng, or null otherwise
     * Code snipet from: http://stackoverflow.com/questions/11213594/how-to-make-http-get-request-in-android
     */
    public LatLng getLocation(String username) {
    	try { 
    		JSONObject obj = SimpleHTTPGETRequester.makeHTTPPOSTRequest("http://our-server.com?username="+username);//TODO:use the real url
    		if(obj.get("errCode").toString().equals("1")){
    			return new LatLng(obj.getDouble("latitude"), obj.getDouble("longitude")); 
    		} else if(obj.getInt("errCode") == NO_SUCH_USER) {
    			Context context = getApplicationContext();
    			CharSequence text = "No such user!";
    			int duration = Toast.LENGTH_SHORT;

    			Toast toast = Toast.makeText(context, text, duration);
    			toast.show();
    			return null;
    		} else if(obj.getInt("errCode") == USER_NOT_BROADCASTING) {
    			Context context = getApplicationContext();
    			CharSequence text = "User not broadcasting!";
    			int duration = Toast.LENGTH_SHORT;
    			
    			Toast toast = Toast.makeText(context, text, duration);
    			toast.show();
    			return null;
    		} 
    	} catch (RuntimeException e) {
			
			Context context = getApplicationContext();
			CharSequence text = "Connection Error";
			int duration = Toast.LENGTH_SHORT;
			
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			throw e;
		} catch (JSONException e) {
			Context context = getApplicationContext();
			CharSequence text = "JSON Error";
			int duration = Toast.LENGTH_SHORT;
			
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		} catch (Exception e) {
			Context context = getApplicationContext();
			CharSequence text = "Error";
			int duration = Toast.LENGTH_SHORT;
			
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		}
		return null; 
    }
 
	// Displays toast showing the text argument.
	protected void showToast(CharSequence text) {
	    Context context = getApplicationContext();
	    int duration = Toast.LENGTH_SHORT;
	    
	    Toast toast = Toast.makeText(context, text, duration);
	    toast.show();
	}
    
}
