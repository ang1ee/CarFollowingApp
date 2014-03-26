package cs169.carfollowingapp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class BroadcastActivity extends MapActivity {
    
	protected static final int SUCCESS = 1;
    protected static final int NO_SUCH_USER = -1;
    protected static final int INCORRECT_PASSWORD = -2;
    protected static final int MALFORMED_LOCATION = -3;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);
        this.map = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.broadcast_map)).getMap();
        this.map.setMyLocationEnabled(true);
        
        Intent intent = getIntent();
        myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        myPassword = intent.getStringExtra(Constants.MY_P_KEY);

        // Getting the location of user
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = service.getBestProvider(criteria, false);
        Location currentLocation = service.getLastKnownLocation(provider);
        
        // If running on emulator, hardcode the location to test UI
        if (Constants.DEBUG) {
            String mocProvider = "testLocationProvider";
            // add the fake service if it hasn't been added
            if (!service.isProviderEnabled(mocProvider)) {
                service.addTestProvider(mocProvider, false, false, false, false,
                        true, true, true, 0, 5);
                service.setTestProviderEnabled(mocProvider, true);
            }
            // create a new location (hardcoded coordinates)
            currentLocation = new Location(provider);
            currentLocation.setLatitude(37.0);
            currentLocation.setLongitude(-122.0);
            currentLocation.setTime(System.currentTimeMillis());
            currentLocation.setAccuracy(3.0f);
            
            // Below is necessary fix for sdk's smaller than 17
            Method locationJellyBeanFixMethod = null;
            try {
                locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
                if (locationJellyBeanFixMethod != null) {
                    locationJellyBeanFixMethod.invoke(currentLocation);
                 }
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            service.setTestProviderLocation(provider, currentLocation);
        }
        
        if (currentLocation == null) {
        	CharSequence text = "Cannot get current location";
		    handleError(text);
		    return;
        }
        
        // Plot user location
        LatLng userLocation = new LatLng(
                currentLocation.getLatitude(),
                currentLocation.getLongitude()
        );
        ArrayList<LatLng> coords = new ArrayList<LatLng>();
        coords.add(userLocation);
        plot(coords);
        
        // Preparing information to store in database
        JSONObject postData = new JSONObject();
        
    	try {
    		String latitude = Double.toString(currentLocation.getLatitude());
    		String longitude = Double.toString(currentLocation.getLongitude());
    		postData.put("username", myUsername);
    		postData.put("password", myPassword);
    		postData.put("latitude", latitude);
    		postData.put("longitude", longitude);
    		postData.put(Constants.ACTION_URL, "api/broadcast");
    		
    		/*
    		JSONObject obj = SimpleHTTPPOSTRequester
    				.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/broadcast", postData);
    		return obj.toString();
    		*/
    	} catch (RuntimeException e) {
    	    Log.e("BroadcastActivity", e.getMessage());
    	    CharSequence text = "A RuntimeException has occurred";
    		handleError(text);
    		return;
		} catch (JSONException e) {
		    Log.e("BroadcastActivity", e.getMessage());
		    CharSequence text = "JSON Error";
    		handleError(text);
    		return;
		} catch (Exception e) {
		    Log.e("BroadcastActivity", e.getMessage());
		    CharSequence text = "Error";
	    	handleError(text);
		}
        
        new HTTPPOSTBroadcastAsyncTask().execute(postData);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.follow, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onPause() {
        // TODO: in future iterations, pause any ongoing broadcast updates
        super.onPause();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Before this activity dies, tell the server 
        // to stop broadcasting will that take too much time?
        stopBroadcasting();
    }
    
    public void stopBroadcasting(View view) {
        stopBroadcasting();
    }
    
    public void stopBroadcasting() {
        JSONObject postData = new JSONObject();
    	try {
    	    postData.put("username", myUsername);
    	    postData.put("password", myPassword);
    	    postData.put(Constants.ACTION_URL, "api/stop_broadcast");
	    } catch (JSONException e) {
	    	CharSequence text = "JSON Error";
	    	handleError(text);
	    }
    	
	    Intent intent = new Intent(this, FrontPageActivity.class);
	    intent.putExtra(Constants.MY_U_KEY, myUsername);
	    intent.putExtra(Constants.MY_P_KEY, myPassword);
	    
	    new HTTPPOSTStopBroadcastingAsyncTask().execute(postData);
	    
	    startActivity(intent);
	    finish();
	}
        
    /* Finds the current location of the application user and updates
     * the database.
     */
    private class HTTPPOSTBroadcastAsyncTask extends HTTPPOSTAsyncTask {
    	
        @Override
        protected void onPostExecute(String result) {
        	if (result == null) {
        		CharSequence text = "Unable to update database with current location";
    		    handleError(text);
        	} else if (result == CONNECTION_ERROR) {
        		CharSequence text = "Connection Error";
    			handleError(text);
    			return;
        	} else if (result == "JSON_EXCEPTION") {
        		handleError("JSON Error");
        	}
        	
        	try {
        		JSONObject jsonResult = new JSONObject(result);
        		int statusCode = jsonResult.getInt("status code");
        		if (statusCode == SUCCESS) {
    		        return;
    		    } else if (statusCode == NO_SUCH_USER) {
    	    		CharSequence text = "No such user!";
    	    		handleError(text);
    	    		return;
    		    } else if (statusCode == INCORRECT_PASSWORD) {
    	    		CharSequence text = "Incorrect password!";
    	    		handleError(text);
    	    		return;
    		    } else if (statusCode == MALFORMED_LOCATION) {
    	    		CharSequence text = "malformed location!";
    	    		handleError(text);
    	    		return;
    		    }
        	} catch (JSONException e) {
    		    CharSequence text = "JSON Error";
    		    handleError(text);
        	}
        	
        	/*
        	JSONObject newPostData = new JSONObject();
            try {
            	newPostData.put("username", myUsername);
            } catch (JSONException e) {
            	Log.e("BroadcastActivity", e.getMessage());
    		    CharSequence text = "JSON Error for follow request";
        		handleError(text);
        		return;
            }
            new HTTPPOSTFollowAsyncTask().execute(newPostData);
            */
        }
        
        /*
        @Override
        protected void onPostExecute(String result) {
        	if (result == null) {
        		CharSequence text = "Unable to update database with current location";
    		    handleError(text);
        	} else if (result == CONNECTION_ERROR) {
        		CharSequence text = "Connection Error";
    			handleError(text);
    			return;
        	} else if (result == JSON_ERROR) {
        		CharSequence text = "JSON Error";
    			handleError(text);
    			return;
        	} else if (result == ERROR) {
        		CharSequence text = "Error";
    			handleError(text);
    			return;
        	}
        	
        	try {
        		JSONObject jsonResult = new JSONObject(result);
        		int statusCode = jsonResult.getInt("status code");
        		if (statusCode == SUCCESS) {
    		        return;
    		    } else if (statusCode == NO_SUCH_USER) {
    	    		CharSequence text = "No such user!";
    	    		handleError(text);
    	    		return;
    		    } else if (statusCode == INCORRECT_PASSWORD) {
    	    		CharSequence text = "Incorrect password!";
    	    		handleError(text);
    	    		return;
    		    } else if (statusCode == MALFORMED_LOCATION) {
    	    		CharSequence text = "malformed location!";
    	    		handleError(text);
    	    		return;
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
        */
        
    }
    
    /* Finds the current location of the application user and updates
     * the database.
     */
    private class HTTPPOSTStopBroadcastingAsyncTask extends HTTPPOSTAsyncTask {

        @Override
        protected void onPostExecute(String result) {
        	if (result == null) {
        		CharSequence text = "Unable to update database with current location";
    		    handleError(text);
        	} else if (result == CONNECTION_ERROR) {
        		CharSequence text = "Connection Error";
    			handleError(text);
    			return;
        	} else if (result == "JSON_EXCEPTION") {
        		handleError("JSON Error");
        	}
        	
        	try {
        		JSONObject jsonResult = new JSONObject(result);
        		int statusCode = jsonResult.getInt("status code");
        		if (statusCode != SUCCESS) {
        			CharSequence text = "Incorrect error code was returned";
        			handleError(text);
        			return;
        		}
        	} catch (JSONException e) {
    		    CharSequence text = "JSON Error";
    		    handleError(text);
        	}
        }
        
    }
 
    /*
    private class HTTPPOSTFollowAsyncTask extends HTTPPOSTAsyncTask {

        @Override
        protected void onPostExecute(String result) {
        	if (result == null) {
        		CharSequence text = "Unable to update database with current location";
    		    handleError(text);
        	} else if (result == CONNECTION_ERROR) {
        		CharSequence text = "Connection Error";
    			handleError(text);
    			return;
        	} else if (result == "JSON_EXCEPTION") {
        		handleError("JSON Error");
        	}
        	
        	try {
        		JSONObject jsonResult = new JSONObject(result);
        		int statusCode = jsonResult.getInt("status code");
        		if (statusCode == SUCCESS) {
        			System.out.println(jsonResult.getDouble("latitude"));
        			System.out.println(jsonResult.getDouble("longitude"));
        			return;
        		} else if (statusCode != -1) {
        			CharSequence text = "No such user for follow";
        			handleError(text);
        			return;
        		} else if (statusCode != -2) {
        			CharSequence text = "user not broadcasting for follow";
        			handleError(text);
        			return;
        		}
        	} catch (JSONException e) {
    		    CharSequence text = "JSON Error";
    		    handleError(text);
        	}
        }
    }
    */
    
}