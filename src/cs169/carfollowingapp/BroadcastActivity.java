package cs169.carfollowingapp;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class BroadcastActivity extends MapActivity {
    
	protected static final int SUCCESS = 1;
    protected static final int NO_SUCH_USER = -1;
    protected static final int INCORRECT_PASSWORD = -2;
    protected static final int MALFORMED_LOCATION = -3;
    
	// if it doesn't go through the try statement will not having a default be a problem?
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        //this.map.setMyLocationEnabled(true);

        if (FrontPageActivity.DEBUG) {
            LatLng location = new LatLng(
                    Double.valueOf(90), 
                    Double.valueOf(90)
            );
            
            ArrayList<LatLng> coords = new ArrayList<LatLng>();
            coords.add(location);
            this.plot(coords);
            return;
        }
        
        final Location currentLocation = this.map.getMyLocation();
        
		try {
		    JSONObject postData = new JSONObject();
		    Intent intent = getIntent();
		    String myUsername = intent.getStringExtra(FrontPageActivity.MY_U_KEY);
		    String myPassword = intent.getStringExtra(FrontPageActivity.MY_P_KEY);
		    String latitude = Double.toString(currentLocation.getLatitude());
		    String longitude = Double.toString(currentLocation.getLongitude());
		    postData.put("username", myUsername);
		    postData.put("password", myPassword);
		    postData.put("latitude", latitude);
		    postData.put("longitude", longitude);
		    
		    /*TODO: Real url*/
		    JSONObject obj = SimpleHTTPPOSTRequester
		    		.makeHTTPPOSTRequest("base_url/api/broadcast", postData); 
		    
		    int statusCode = obj.getInt("status code");
		    if (statusCode == SUCCESS) {
		        LatLng location = new LatLng(
                        Double.valueOf(latitude), 
                        Double.valueOf(longitude)
                );
                
                ArrayList<LatLng> coords = new ArrayList<LatLng>();
                coords.add(location);
                this.plot(coords);
		    } else if (statusCode == NO_SUCH_USER) {
	    		CharSequence text = "No such user!";
	    		handleError(text);
		    } else if (statusCode == INCORRECT_PASSWORD) {
	    		CharSequence text = "Incorrect password!";
	    		handleError(text);
		    } else if (statusCode == MALFORMED_LOCATION) {
	    		CharSequence text = "malformed location!";
	    		handleError(text);
		    }
	    } catch (RuntimeException e) {
		    CharSequence text = "Connection Error";
		    handleError(text);
		} catch (JSONException e) {
		    CharSequence text = "JSON Error";
		    handleError(text);
		} catch (Exception e) {
		    CharSequence text = "Error";
		    handleError(text);
		}
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.follow, menu);
        return true;
    }
    
    public void stopBroadcasting(View view) {
        
        if (FrontPageActivity.DEBUG) {
            Intent intent = new Intent(this, FrontPageActivity.class);
            startActivity(intent);
            finish();
        }
        
    	try {
    	    JSONObject postData = new JSONObject();
    	    postData.put("username", myUsername);
    	    postData.put("password", myPassword);
    	    JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest("base_url/api/stop_broadcast", postData);
    	    int statusCode = obj.getInt("status code");
    	    if (statusCode != SUCCESS) {
        		CharSequence text = "Incorrect error code was returned";
        		handleError(text);
    	    }
	    } catch (RuntimeException e) {
	    	CharSequence text = "Connection Error";
	    	handleError(text);
	    } catch (JSONException e) {
	    	CharSequence text = "JSON Error";
	    	handleError(text);
	    } catch (Exception e) {
	    	CharSequence text = "Error";
	    	handleError(text);
	    }
	    Intent intent = new Intent(this, FrontPageActivity.class);
	    intent.putExtra(FrontPageActivity.MY_U_KEY, myUsername);
	    intent.putExtra(FrontPageActivity.MY_P_KEY, myPassword);
	    startActivity(intent);
	    finish();
	}
    
}