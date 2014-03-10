import java.util.ArrayList;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class BroadcastActivity extends MapActivity {

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
	    postData.put("username", username);
	    postData.put("password", password);
	    JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest("base_url/api/stop_broadcast", postData);
	    int statusCode = obj.getInt("status code");
	    if (statusCode != SUCCESS) {
		CharSequence text = "Incorrect error code was returned";
		showToast(text);
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
    
	// Displays toast showing the text argument.
	private void showToast(CharSequence text) {
	    Context context = getApplicationContext();
	    int duration = Toast.LENGTH_SHORT;
	    
	    Toast toast = Toast.makeText(context, text, duration);
	    toast.show();
	}

    }