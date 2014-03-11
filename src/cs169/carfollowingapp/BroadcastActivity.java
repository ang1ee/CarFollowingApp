package cs169.carfollowingapp;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class BroadcastActivity extends MapActivity {
	
	protected static final int SUCCESS = 1;
    protected static final int NO_SUCH_USER = -1;
    protected static final int INCORRECT_PASSWORD = -2;
    protected static final int MALFORMED_LOCATION = -3;
    
	// if it doesn't go through the try statement will not having a default be a problem?
    private String username;
    private String password;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        //this.map.setMyLocationEnabled(true);

	try {
	    JSONObject postData = new JSONObject();
	    Intent intent = getIntent();
	    String username = intent.getStringExtra("username");
	    String password = intent.getStringExtra("password");
	    LatLng location = this.getLocation();
	    String latitude = Double.toString(location.latitude);
	    String longitude = Double.toString(location.longitude);
	    postData.put("username", username);
	    postData.put("password", password);
	    
	    // ? did we intend to have the key be password for both of these?
	    postData.put("password", latitude);
	    postData.put("password", longitude);
	    JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest("base_url/api/broadcast", postData);
	    
	    int statusCode = obj.getInt("status code");
	    if (statusCode == SUCCESS) {
		return;
	    } else if (statusCode == NO_SUCH_USER) {
		CharSequence text = "No such user!";
		showToast(text);
		return;
	    } else if (statusCode == INCORRECT_PASSWORD) {
		CharSequence text = "Incorrect password!";
		showToast(text);
		return;
	    } else if (statusCode == MALFORMED_LOCATION) {
		CharSequence text = "malformed location!";
		showToast(text);
		return;
	    }
	    //LatLng location = this.getLocation();
	    ArrayList<LatLng> coords = new ArrayList<LatLng>();
	    coords.add(location);
	    this.plot(coords);
        } catch (RuntimeException e) {
	    CharSequence text = "Connection Error";
	    showToast(text);
	} catch (JSONException e) {
	    CharSequence text = "JSON Error";
	    showToast(text);
	} catch (Exception e) {
	    CharSequence text = "Error";
	    showToast(text);
	}
	
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.follow, menu);
        return true;
    }
    
    public void stopBroadcasting() {
    	try {
    	    JSONObject postData = new JSONObject();
    	    postData.put("username", username);
    	    postData.put("password", password);
    	    JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest("base_url/api/stop_broadcast", postData);
    	    int statusCode = obj.getInt("status code");
    	    if (statusCode != SUCCESS) {
        		CharSequence text = "Incorrect error code was returned";
        		showToast(text);
    	    }
	    } catch (RuntimeException e) {
	    	CharSequence text = "Connection Error";
	    	showToast(text);
	    } catch (JSONException e) {
	    	CharSequence text = "JSON Error";
	    	showToast(text);
	    } catch (Exception e) {
	    	CharSequence text = "Error";
	    	showToast(text);
	    }
	    Intent intent = new Intent(this, FrontPageActivity.class);
	    startActivity(intent);
	}
    
}