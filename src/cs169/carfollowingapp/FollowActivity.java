package cs169.carfollowingapp;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;



public class FollowActivity extends MapActivity {
	private String loginUrl = Constants.BASE_SERVER_URL + "api/login";
	private String username;
	private int errCode;
	private ArrayList<LatLng> coords = new ArrayList<LatLng>();
	protected static final int SUCCESS = 1;
    protected static final int NO_SUCH_USER = -1;
    protected static final int USER_NOT_BROADCASTING = -2;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        this.map.setMyLocationEnabled(true);

        if (Constants.DEBUG) {
            LatLng location = new LatLng(Double.valueOf(90), Double.valueOf(90));
            
            coords.add(location);
            this.plot(coords);
            return;
        }
        
        Intent intent = getIntent();
        username = intent.getStringExtra(FrontPageActivity.U_KEY);

        new HttpAsyncTask().execute(loginUrl);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.follow, menu);
        return true;
    }
    
    private class HttpAsyncTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... urls) {

        		JSONObject postData = new JSONObject();
        		try {
					postData.put("username", username);
					JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/follow", postData);
					return obj;
        		} catch (JSONException e) {
					handleError(e.getMessage());
				} catch (RuntimeException e) {
					handleError(e.getMessage());
				}
        		return null;
        	
        }
        @Override
        protected void onPostExecute(JSONObject result) {
            //Toast.makeText(getBaseContext(),result, Toast.LENGTH_LONG).show();
            JSONObject fin;
            try {
                fin = result;
                errCode = fin.getInt("status code");
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case SUCCESS:
                    	coords.add(new LatLng(fin.getDouble("latitude"), fin.getDouble("longitude")));
                        plot(coords);
                        break;
                    case NO_SUCH_USER:
                        handleError("User does not exist.");
                        break;
                    case USER_NOT_BROADCASTING:
                        handleError("User is not broadcasting.");
                        break;
                    default:
                        handleError("Unknown errCode.");
                        break;
                }
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
                handleError(e.getMessage());
            }
        }
    }
    
    
    
    
    
    // TODO: make sure non-authenticated users can't get anyone's location
    /* 
     * username: who to obtain the location point from
     * returns the obtained latitude/longitude in the form of LatLng, or null otherwise
     * Code snipet from: http://stackoverflow.com/questions/11213594/how-to-make-http-get-request-in-android
     */
    /*
    public LatLng getLocation(String username) throws Exception {
    	try { 
    		JSONObject postData = new JSONObject();
    		postData.put("username", username);
    		JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/follow", postData);
    		if(obj.getInt("status code") == SUCCESS){
    			return new LatLng(obj.getDouble("latitude"), obj.getDouble("longitude")); 
    		} else if(obj.getInt("status code") == NO_SUCH_USER) {
    			
    			throw new RuntimeException("No such user!");
    		} else if(obj.getInt("status code") == USER_NOT_BROADCASTING) {
    			
    			throw new RuntimeException("User not broadcasting!");
    		} 
    	} catch (RuntimeException e) {
			
			
			throw e;
		} catch (JSONException e) {
			
			throw new JSONException("JSON Error");
			
		} catch (Exception e) {
			
			throw new Exception("Error");
		}
		return null; 
    }
    */
    public void stopFollowing(View view) {
	    Intent intent = new Intent(this, FrontPageActivity.class);
	    startActivity(intent);
	    finish();
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
