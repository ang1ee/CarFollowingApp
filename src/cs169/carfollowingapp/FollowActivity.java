package cs169.carfollowingapp;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class FollowActivity extends MapActivity {
	protected static final int SUCCESS = 1;
    protected static final int NO_SUCH_USER = -1;
    protected static final int USER_NOT_BROADCASTING = -2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        this.map.setMyLocationEnabled(true);
        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        LatLng location = this.getLocation(username);
        ArrayList<LatLng> coords = new ArrayList<LatLng>();
        coords.add(location);
        this.plot(coords);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.follow, menu);
        return true;
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
    /*
    private class DisplayCurrentLocation extends AsyncTask<GoogleMap, Integer, Long> {
        protected Long doInBackground(GoogleMap... maps) {
        	GoogleMap map = maps[0];
        	while (true) {
        		Location currLocation = map.getMyLocation();
        		ArrayList<LatLng> coords = new ArrayList<LatLng>();
        		coords.add();
        	}
        	// old code
            int count = urls.length;
            long totalSize = 0;
            for (int i = 0; i < count; i++) {
                totalSize += Downloader.downloadFile(urls[i]);
                publishProgress((int) ((i / (float) count) * 100));
                // Escape early if cancel() is called
                if (isCancelled()) break;
            }
            return totalSize;
            
        	
        }

        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Long result) {
            //showDialog("Downloaded " + result + " bytes");
        }
    }
    */
}
