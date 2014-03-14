package cs169.carfollowingapp;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
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
    
	// if it doesn't go through the try statement will not having a default be a problem?
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        this.map.setMyLocationEnabled(true);
        
        Intent intent = getIntent();
        myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        myPassword = intent.getStringExtra(Constants.MY_P_KEY);

        if (Constants.DEBUG) {
            LatLng location = new LatLng(
                    Double.valueOf(90), 
                    Double.valueOf(90)
            );
            
            ArrayList<LatLng> coords = new ArrayList<LatLng>();
            coords.add(location);
            this.plot(coords);
            return;
        }
/*        
        Location currentLocation = null;

        try {
        	currentLocation = this.map.getMyLocation();
        } catch (IllegalStateException e) {
        	CharSequence text = "my-location layer not enabled";
		    handleError(text);
        }
 */       

        
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = service.getBestProvider(criteria, false);
        Location currentLocation = service.getLastKnownLocation(provider);
        
        if (currentLocation == null) {
        	CharSequence text = "Cannot get current location";
		    handleError(text);
		    return;
        }
        
        LatLng userLocation = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
/*        
        LatLng location = new LatLng(
                currentLocation.getLatitude(), 
                currentLocation.getLongitude()
        );
 */       
        ArrayList<LatLng> coords = new ArrayList<LatLng>();
        coords.add(userLocation);
        plot(coords);
        
        JSONObject postData = new JSONObject();
    	
        
        String latitude = Double.toString(currentLocation.getLatitude());
		String longitude = Double.toString(currentLocation.getLongitude());
        
    	try {
    		String myUsername = intent.getStringExtra(Constants.MY_U_KEY);
    		String myPassword = intent.getStringExtra(Constants.MY_P_KEY);
    		latitude = Double.toString(currentLocation.getLatitude());
    		longitude = Double.toString(currentLocation.getLongitude());
    		postData.put("username", myUsername);
    		postData.put("password", myPassword);
    		postData.put("latitude", latitude);
    		postData.put("longitude", longitude);
    		Log.e("=========username==========", "" + myUsername);
    		Log.e("=========password==========", "" + myPassword);
    		Log.e("=========latitude==========", "" + latitude);
    		Log.e("=========longitude=========", "" + longitude);
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
    
    public void stopBroadcasting(View view) {
        
        if (Constants.DEBUG) {
            Intent intent = new Intent(this, FrontPageActivity.class);
            startActivity(intent);
            finish();
        }
        
        JSONObject postData = new JSONObject();
    	try {
    	    postData.put("username", myUsername);
    	    postData.put("password", myPassword);
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
    	static final String CONNECTION_ERROR = "Connection Error";
    	
    	@Override
        protected String doInBackground(JSONObject... jsonObjects) {
    		JSONObject postData = jsonObjects[0];
    		JSONObject obj = null;
    		try {
    			obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/stop_broadcast", postData);
    		} catch (RuntimeException e) {
    	    	return CONNECTION_ERROR;
    	    } 
    		return obj.toString();
    		/*
        	Location currentLocation = locations[0];
        	
        	JSONObject postData = new JSONObject();
        	
     
            String latitude = Double.toString(currentLocation.getLatitude());
    		String longitude = Double.toString(currentLocation.getLongitude());
            
        	try {
        		Intent intent = getIntent();
        		String myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        		String myPassword = intent.getStringExtra(Constants.MY_P_KEY);
        		latitude = Double.toString(currentLocation.getLatitude());
        		longitude = Double.toString(currentLocation.getLongitude());
        		postData.put("username", myUsername);
        		postData.put("password", myPassword);
        		postData.put("latitude", latitude);
        		postData.put("longitude", longitude);
        		Log.e("=========username==========", "" + myUsername);
        		Log.e("=========password==========", "" + myPassword);
        		Log.e("=========latitude==========", "" + latitude);
        		Log.e("=========longitude=========", "" + longitude);
        		JSONObject obj = SimpleHTTPPOSTRequester
        				.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/broadcast", postData);
        		return obj.toString();
        	} catch (RuntimeException e) {
        	    Log.e("BroadcastActivity", e.getMessage());
    		    return CONNECTION_ERROR;
    		} catch (JSONException e) {
    		    Log.e("BroadcastActivity", e.getMessage());
    		    return JSON_ERROR;
    		} catch (Exception e) {
    		    Log.e("BroadcastActivity", e.getMessage());
    		    return ERROR;
    		}
    		*/
        }

        @Override
        protected void onPostExecute(String result) {
        	if (result == null) {
        		CharSequence text = "Unable to update database with current location";
    		    handleError(text);
        	} else if (result == CONNECTION_ERROR) {
        		CharSequence text = "Connection Error";
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
        	} catch (JSONException e) {
    		    CharSequence text = "JSON Error";
    		    handleError(text);
    		}	
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
    	static final String CONNECTION_ERROR = "Connection Error";
    	
    	@Override
        protected String doInBackground(JSONObject... jsonObjects) {
    		JSONObject postData = jsonObjects[0];
    		JSONObject obj = null;
    		try {
    			obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/stop_broadcast", postData);
    		} catch (RuntimeException e) {
    	    	return CONNECTION_ERROR;
    	    } 
    		return obj.toString();
        }

        @Override
        protected void onPostExecute(String result) {
        	if (result == null) {
        		CharSequence text = "Unable to update database with current location";
    		    handleError(text);
        	} else if (result == CONNECTION_ERROR) {
        		CharSequence text = "Connection Error";
    			handleError(text);
    			return;
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
    
}