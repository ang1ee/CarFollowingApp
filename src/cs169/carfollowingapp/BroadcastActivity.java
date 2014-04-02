package cs169.carfollowingapp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
    
    private static int currentID = 0;
    private int instanceID;
    private Handler broadcastHandler = new Handler();
    //private Runnable broadcastRunnable;
    //private Runnable followRequestRunnable;
    private Handler followRequestHandler = new Handler();
    
    /* Used to save currentLocation for AsyncTask. 
     * Maybe should change at some point since seems
     * somewhat hackish...
     */
    protected Location currentLocation;
    /* Same with the four below... */
    protected boolean progressSuccessful = true;
    protected CharSequence errorText;
    protected LinkedList<String> followRequestUsernames = new LinkedList<String>();
    protected String followName;
    protected double debugLatitude = 37.0;
    protected double debugLongitude = -122.0;
    
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
            currentLocation = new Location(mocProvider);
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

            service.setTestProviderLocation(mocProvider, currentLocation);
        }
        
        if (currentLocation == null) {
        	CharSequence text = "Cannot get current location";
		    handleError(text);
		    return;
        }
        
        /* Set ID so that AsyncTasks of old instances of BroadcastActivity
         * terminate when they see that their ID is not the current ID.
         */
        instanceID = BroadcastActivity.currentID;
        
        /*
        broadcastRunnable = new Runnable() {
            public void run() {
            	new HTTPPOSTBroadcastAsyncTask().execute(this);
            }
        };
        
        followRequestRunnable = new Runnable() {
            public void run() {
            	new HTTPPOSTGetFollowRequestsAsyncTask().execute(this);
            }
        };
        */
                
        new HTTPPOSTBroadcastAsyncTask().execute(this);
        new HTTPPOSTGetFollowRequestsAsyncTask().execute(this);
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
    	handleCleanup();
    	
	    Intent intent = new Intent(this, FrontPageActivity.class);
	    intent.putExtra(Constants.MY_U_KEY, myUsername);
	    intent.putExtra(Constants.MY_P_KEY, myPassword);
	    
	    new HTTPPOSTStopBroadcastingAsyncTask().execute(this);
	    
	    startActivity(intent);
	    finish();
	}
    
    protected String getFollowName() {
    	return followName;
    }
    
    protected void setCurrentLocation(Location currLoc) {
    	progressSuccessful = true;
    	currentLocation = currLoc;
    }
    
    protected void setErrorText(CharSequence errTxt) {
    	progressSuccessful = false;
    	errorText = errTxt;
    }
    
    protected void setFollowRequestUsernames(LinkedList<String> followers) {
    	progressSuccessful = true;
    	followRequestUsernames = followers;
    }
    
    public void invitationResponse() {
    	new HTTPPOSTInvitationResponseAsyncTask().execute(this);
    }
    
    public void showFollowRequestDialog() {
    	if (followRequestUsernames.size() <= 0) {
    		return;
    	}
    	followName = followRequestUsernames.remove();
    	FollowRequestDialogFragment newFragment = new FollowRequestDialogFragment();
    	newFragment.setBActivity(this);
    	newFragment.show(getFragmentManager(), "follow request");
    }
    
    @Override
    protected void handleCleanup() {
        //BroadcastActivity.currentID++;
    	broadcastHandler.removeCallbacksAndMessages(null);
    	followRequestHandler.removeCallbacksAndMessages(null);
    }
        
    /* Finds the current location of the application user and updates
     * the database.
     */
    private class HTTPPOSTBroadcastAsyncTask extends AsyncTask<BroadcastActivity, BroadcastActivity, Integer> {
    	static final int GET_LOC_SUCCESS = 1;
    	static final int GET_LOC_FAIL = -1;
    	static final int BROADCAST_FREQUENCY = 10 * 1000;
    	private int instanceID;
    	private BroadcastActivity bActivity;
    	private String myUsername;
    	private String myPassword;
    		
    	
    	@Override
        protected Integer doInBackground(BroadcastActivity... broadcastActivties) {
    		bActivity = broadcastActivties[0];
    		instanceID = bActivity.instanceID;    		
    		myUsername = bActivity.myUsername;
    		myPassword = bActivity.myPassword;
    		
    		// Getting the location of user
            LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = service.getBestProvider(criteria, false);
            
            //while (instanceID == BroadcastActivity.currentID) {
            //bActivity.map.clear();
            int result = getLocationAndUpdateDB(bActivity, service, provider);
            /*
            if (result == GET_LOC_FAIL) {
            	break;
            }
            */
            //}
            
            return result;
    	}
    	
    	protected int getLocationAndUpdateDB(BroadcastActivity bActivity, LocationManager service, String provider) {
    		String broadcastActionURL = "api/broadcast";
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
                currentLocation = new Location(mocProvider);
                currentLocation.setLatitude(debugLatitude);
                currentLocation.setLongitude(debugLongitude);
                debugLatitude++;
                debugLongitude++;
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
				
                service.setTestProviderLocation(mocProvider, currentLocation);
            }
            
            if (currentLocation == null) {
            	bActivity.setErrorText("Cannot get current location");
            	publishProgress(bActivity);
    		    return GET_LOC_FAIL;
            }
            
            bActivity.setCurrentLocation(currentLocation);
            // Plot user location
            publishProgress(bActivity);
            
            // Preparing information to store in database
            JSONObject postData = new JSONObject();
            
        	try {
        		String latitude = Double.toString(currentLocation.getLatitude());
        		String longitude = Double.toString(currentLocation.getLongitude());
        		postData.put("username", myUsername);
        		postData.put("password", myPassword);
        		postData.put("latitude", latitude);
        		postData.put("longitude", longitude);
        		
        		/*
        		JSONObject obj = SimpleHTTPPOSTRequester
        				.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/broadcast", postData);
        		return obj.toString();
        		*/
        	} catch (RuntimeException e) {
        	    Log.e("HTTPPOSTBroadcastAsyncTask", e.getMessage());
        	    bActivity.setErrorText("A RuntimeException has occurred");
        		publishProgress(bActivity);
        		return GET_LOC_FAIL;
    		} catch (JSONException e) {
    		    Log.e("HTTPPOSTBroadcastAsyncTask", e.getMessage());
    		    bActivity.setErrorText("JSON Error");
    		    publishProgress(bActivity);
        		return GET_LOC_FAIL;
    		} catch (Exception e) {
    		    Log.e("HTTPPOSTBroadcastAsyncTask", e.getMessage());
    	    	bActivity.setErrorText("A RuntimeException has occurred");
        		publishProgress(bActivity);
    		}
    		
    		JSONObject obj = null;
    		try {
    			obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + broadcastActionURL, postData);
    		} catch (RuntimeException e) {
    			bActivity.setErrorText("Connection Error");
        		publishProgress(bActivity);
    			return GET_LOC_FAIL;
    		}
    		int checkJSONResult = checkJSONResponse(bActivity, obj.toString());
    		if (checkJSONResult == GET_LOC_FAIL) {
    			return GET_LOC_FAIL;
    		} else {
    			return GET_LOC_SUCCESS;
    		}
    	}
    	
    	/* Checks if there were any errors indicated by the JSON response.
    	 * Returns 1 if there were no errors and returns -1 if there was an issue.
    	 */
        protected int checkJSONResponse(BroadcastActivity bActivity, String response) {
        	if (response == null) {
    		    bActivity.setErrorText("Unable to update database with current location");
        		publishProgress(bActivity);
        		return GET_LOC_FAIL;
        	}
        	
        	int statusCode = 0;
        	try {
        		JSONObject jsonResponse = new JSONObject(response);
        		statusCode = jsonResponse.getInt("status code");
        		if (statusCode == SUCCESS) {
    		        return GET_LOC_SUCCESS;
    		    } else if (statusCode == NO_SUCH_USER) {
    	    		bActivity.setErrorText("No such user!");
            		publishProgress(bActivity);
    	    		return GET_LOC_FAIL;
    		    } else if (statusCode == INCORRECT_PASSWORD) {
    	    		bActivity.setErrorText("Incorrect password!");
            		publishProgress(bActivity);
    	    		return GET_LOC_FAIL;
    		    } else if (statusCode == MALFORMED_LOCATION) {
    	    		bActivity.setErrorText("malformed location!");
            		publishProgress(bActivity);
    	    		return GET_LOC_FAIL;
    		    }
        	} catch (JSONException e) {
    		    bActivity.setErrorText("JSON Error");
        		publishProgress(bActivity);
        		return GET_LOC_FAIL;
        	}
        	if (statusCode == SUCCESS) {
		        return GET_LOC_SUCCESS;
		    } else {
		    	bActivity.setErrorText("Unrecognized status code!");
		    	publishProgress(bActivity);
		    	return GET_LOC_FAIL;
		    }
	    }
        
        protected void onProgressUpdate(BroadcastActivity... broadcastActivties) {
        	BroadcastActivity bActivity = broadcastActivties[0];
        	
        	if (bActivity.progressSuccessful == false) {
        		bActivity.handleError(bActivity.errorText);
        		return;
        	}
        	Location currentLocation = bActivity.currentLocation;
        	// Plot user location
            LatLng userLocation = new LatLng(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude()
            );
            ArrayList<LatLng> coords = new ArrayList<LatLng>();
            coords.add(userLocation);
            bActivity.plot(coords);
        }
        
        @Override
        protected void onPostExecute(Integer result) {
        	if (result == GET_LOC_SUCCESS) { //&& (instanceID == BroadcastActivity.currentID)) {
        		broadcastHandler.postDelayed(new Runnable() {
                    public void run() {
                    	new HTTPPOSTBroadcastAsyncTask().execute(bActivity);
                    }
                }, BROADCAST_FREQUENCY);
        	}
        }
        
    }
    
    /* Receives follow requests for the broadcaster. */
    private class HTTPPOSTGetFollowRequestsAsyncTask extends AsyncTask<BroadcastActivity, BroadcastActivity, Integer> {
    	static final int SUCCESS = 1;
    	static final int NO_SUCH_USER_FOR_MY_USERNAME = -1;
    	static final int INCORRECT_PASSWORD = -2;
    	static final int USER_NOT_BROADCASTING = -3;
    	
    	static final int REQUEST_CHECK_FREQUENCY = 10 * 1000;
    	static final int CHECK_FOLLOW_REQS_SUCCESS = 1;
    	static final int CHECK_FOLLOW_REQS_FAIL = -1;
    	private int instanceID;
    	private String myUsername;
    	private String myPassword;
    	private BroadcastActivity bActivity;
    	    	
    	@Override
        protected Integer doInBackground(BroadcastActivity... broadcastActivties) {
    		bActivity = broadcastActivties[0];
    		instanceID = bActivity.instanceID;    		
    		myUsername = bActivity.myUsername;
    		myPassword = bActivity.myPassword;
    		
            //while (instanceID == BroadcastActivity.currentID) {
            int result = checkFollowRequests(bActivity);
            /*
            	if (result == CHECK_FOLLOW_REQS_FAIL) {
            		break;
            	}
            	try {
            		Thread.sleep(REQUEST_CHECK_FREQUENCY);
            	} catch(InterruptedException e) {
            		e.printStackTrace();
            		bActivity.setErrorText("Thread has interrupted current thread");
        		    publishProgress(bActivity);
            	}	
            }
            */
            return result;
    	}
    	
    	protected int checkFollowRequests(BroadcastActivity bActivity) {
    		String actionURL = "api/get_follow_requests";

            // Preparing information to store in database
            JSONObject postData = new JSONObject();
            
        	try {
        		postData.put("myUsername", myUsername);
        		postData.put("myPassword", myPassword);        		
        		/*
        		JSONObject obj = SimpleHTTPPOSTRequester
        				.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/broadcast", postData);
        		return obj.toString();
        		*/
        	} catch (RuntimeException e) {
        	    Log.e("HTTPPOSTBroadcastAsyncTask", e.getMessage());
        	    bActivity.setErrorText("A RuntimeException has occurred");
        		publishProgress(bActivity);
        		return CHECK_FOLLOW_REQS_FAIL;
    		} catch (JSONException e) {
    		    Log.e("HTTPPOSTBroadcastAsyncTask", e.getMessage());
    		    bActivity.setErrorText("JSON Error");
    		    publishProgress(bActivity);
        		return CHECK_FOLLOW_REQS_FAIL;
    		} catch (Exception e) {
    		    Log.e("HTTPPOSTBroadcastAsyncTask", e.getMessage());
    	    	bActivity.setErrorText("A RuntimeException has occurred");
        		publishProgress(bActivity);
    		}
    		
    		JSONObject obj = null;
    		JSONArray followers = null;
    		try {
    			obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + actionURL, postData);
    		} catch (RuntimeException e) {
    			bActivity.setErrorText("Connection Error");
        		publishProgress(bActivity);
    			return CHECK_FOLLOW_REQS_FAIL;
    		}
    		String JSONString = obj.toString();
    		int checkJSONResult = checkJSONResponse(bActivity, JSONString);
    		if (checkJSONResult == CHECK_FOLLOW_REQS_FAIL) {
    			return CHECK_FOLLOW_REQS_FAIL;
    		} else {
    			try {
    				followers = obj.getJSONArray("follow requests");
    				putFollowReqsInList(bActivity, followers);
    				publishProgress(bActivity);
    				return CHECK_FOLLOW_REQS_SUCCESS;
    			} catch (JSONException e) {
        			e.printStackTrace();
        			bActivity.setErrorText("JSON error");
            		publishProgress(bActivity);
        			return CHECK_FOLLOW_REQS_FAIL;
        		}
    		}
    	}
    	
    	/* Puts follow requests in an array list instance variable in BroadcastActivity
    	 * instance.
    	 */
    	private void putFollowReqsInList(BroadcastActivity bActivity, JSONArray followers) throws JSONException {
    		bActivity.progressSuccessful = true;
    		for (int i = 0; i < followers.length(); i++) {
    			try {
    				String follower = followers.getString(i);
    				bActivity.followRequestUsernames.add(follower);
    			} catch (JSONException e) {
    				throw e;
    			}
    		}
    	}
    	
    	/* Checks if there were any errors indicated by the JSON response.
    	 * Returns 1 if there were no errors and returns -1 if there was an issue.
    	 */
        protected int checkJSONResponse(BroadcastActivity bActivity, String response) {
        	if (response == null) {
    		    bActivity.setErrorText("Unable to update database with current location");
        		publishProgress(bActivity);
        		return CHECK_FOLLOW_REQS_FAIL;
        	}
        	
        	int statusCode = 0;
        	try {
        		JSONObject jsonResponse = new JSONObject(response);
        		statusCode = jsonResponse.getInt("status code");
        		if (statusCode == NO_SUCH_USER_FOR_MY_USERNAME) {
    	    		bActivity.setErrorText("No such user!");
            		publishProgress(bActivity);
    	    		return CHECK_FOLLOW_REQS_FAIL;
    		    } else if (statusCode == INCORRECT_PASSWORD) {
    	    		bActivity.setErrorText("Incorrect password!");
            		publishProgress(bActivity);
    	    		return CHECK_FOLLOW_REQS_FAIL;
    		    } else if (statusCode == USER_NOT_BROADCASTING) {
    	    		bActivity.setErrorText("User not broadcasting!");
            		publishProgress(bActivity);
    	    		return CHECK_FOLLOW_REQS_FAIL;
    		    }
        	} catch (JSONException e) {
    		    bActivity.setErrorText("JSON Error");
        		publishProgress(bActivity);
        		return CHECK_FOLLOW_REQS_FAIL;
        	}
        	if (statusCode == SUCCESS) {
		        return CHECK_FOLLOW_REQS_SUCCESS;
		    } else {
		    	bActivity.setErrorText("Unrecognized status code!");
        		publishProgress(bActivity);
	    		return CHECK_FOLLOW_REQS_FAIL;
		    }
        }
        
        protected void onProgressUpdate(BroadcastActivity... broadcastActivties) {
        	BroadcastActivity bActivity = broadcastActivties[0];
        	
        	if (bActivity.progressSuccessful == false) {
        		handleError(bActivity.errorText);
        		return;
        	}
        	
        	if (bActivity.followRequestUsernames.size() > 0) {
        		bActivity.showFollowRequestDialog();
        	}
        	/*
        	for (int i = 0; i < bActivity.followRequestUsernames.size(); i++) {
        		DialogFragment newFragment = new FollowRequestDialogFragment();
                newFragment.show(getFragmentManager(), "follow request");
        	}
        	*/
        }
        
        @Override
        protected void onPostExecute(Integer result) {
        	if (result == CHECK_FOLLOW_REQS_SUCCESS) {// && (instanceID == BroadcastActivity.currentID)) {
        		followRequestHandler.postDelayed(new Runnable() {
                    public void run() {
                    	new HTTPPOSTGetFollowRequestsAsyncTask().execute(bActivity);
                    }
                }, REQUEST_CHECK_FREQUENCY);
        	}
        }
        
    }
    
    /* Finds the current location of the application user and updates
     * the database.
     */
    private class HTTPPOSTStopBroadcastingAsyncTask extends AsyncTask<BroadcastActivity, BroadcastActivity, String> {
    	static final String CONNECTION_ERROR = "Connection Error";
    	protected static final String JSON_ERROR = "json error";
    	BroadcastActivity bActivity;
    	
    	@Override
    	protected String doInBackground(BroadcastActivity... broadcastActivities) {
    		bActivity = broadcastActivities[0];
    		
    		JSONObject postData = new JSONObject();
        	try {
        	    postData.put(Constants.U_KEY, myUsername);
        	    postData.put(Constants.P_KEY, myPassword);
    	    } catch (JSONException e) {
    	    	return JSON_ERROR;
    	    }
        	    		
    		JSONObject obj = new JSONObject();
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
    		    bActivity.handleError(text);
    		    return;
        	} else if (result == CONNECTION_ERROR) {
        		CharSequence text = "Connection Error";
    			bActivity.handleError(text);
    			return;
        	} else if (result == "JSON_EXCEPTION") {
        		CharSequence text = "JSON Error";
        		bActivity.handleError(text);
        		return;
        	}
        	
        	try {
        		JSONObject jsonResult = new JSONObject(result);
        		int statusCode = jsonResult.getInt("status code");
        		if (statusCode != SUCCESS) {
        			CharSequence text = "Incorrect error code was returned";
        			bActivity.handleError(text);
        			return;
        		}
        	} catch (JSONException e) {
        		e.printStackTrace();
    		    CharSequence text = "JSON Error";
    		    bActivity.handleError(text);
        	}
        }
        
    }
    
    private class HTTPPOSTInvitationResponseAsyncTask extends AsyncTask<BroadcastActivity, BroadcastActivity, String> {
    	static final int SUCCESS = 1;
    	static final int NO_SUCH_USERNAME_FOR_MY_USERNAME = -1;
    	static final int INCORRECT_PASSWORD = -2;
    	static final int USER_NOT_BROADCASTING = -3;
    	static final int NO_SUCH_FOLLOWER_USERNAME = -4;

    	
    	static final int JSON_SUCCESS = 1;
    	static final int JSON_FAIL = -1;
    	    	
    	@Override
        protected String doInBackground(BroadcastActivity... broadcastActivities) {
    		BroadcastActivity bActivity = broadcastActivities[0];
    		String actionURL = "/api/invitation_response";
    		
    		JSONObject postData = new JSONObject();
    		try {
        		postData.put("myUsername", bActivity.myUsername);
        		postData.put("myPassword", bActivity.myPassword);
        		postData.put("username", bActivity.followName);
        		String myUsername = postData.getString("myUsername");
        		System.out.println("hi");
        		//XXX ADD BOOLEAN
        		
        		/*
        		JSONObject obj = SimpleHTTPPOSTRequester
        				.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/broadcast", postData);
        		return obj.toString();
        		*/
        	} catch (RuntimeException e) {
        	    Log.e("HTTPPOSTInvitationResponseAsyncTask", e.getMessage());
        	    bActivity.setErrorText("Connection error");
        		publishProgress(bActivity);
        		return null;
    		} catch (JSONException e) {
    		    Log.e("HTTPPOSTInvitationResponseAsyncTask", e.getMessage());
    		    bActivity.setErrorText("JSON Error");
    		    publishProgress(bActivity);
        		return null;
    		} catch (Exception e) {
    		    Log.e("HTTPPOSTInvitationResponseAsyncTask", e.getMessage());
    	    	bActivity.setErrorText("A RuntimeException has occurred");
    	    	publishProgress(bActivity);
    	    	return null;
    		}    		
    		
    		JSONObject obj = null;
    		try {
    			obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + actionURL, postData);
    		} catch (RuntimeException e) {
    			Log.e("HTTPPOSTInvitationResponseAsyncTask", e.getMessage());
        	    bActivity.setErrorText("Connection error");
        		publishProgress(bActivity);
        		return null;
    		}
    		checkJSONResponse(bActivity, obj.toString());
    		return null;
    	}
    	
    	protected void onProgressUpdate(BroadcastActivity... broadcastActivties) {
        	BroadcastActivity bActivity = broadcastActivties[0];
        	
        	if (bActivity.progressSuccessful == false) {
        		bActivity.handleError(bActivity.errorText);
        		return;
        	}
        }
    	
    	/* Checks if there were any errors indicated by the JSON response.
    	 * Returns 1 if there were no errors and returns -1 if there was an issue.
    	 */
        protected int checkJSONResponse(BroadcastActivity bActivity, String response) {
        	if (response == null) {
    		    bActivity.setErrorText("Unable to update database with current location");
        		publishProgress(bActivity);
        		return JSON_FAIL;
        	}
        	
        	try {
        		JSONObject jsonResponse = new JSONObject(response);
        		int statusCode = jsonResponse.getInt("status code");
        		if (statusCode == SUCCESS) {
    		        return JSON_SUCCESS;
    		    } else if (statusCode == NO_SUCH_USERNAME_FOR_MY_USERNAME) {
    	    		bActivity.setErrorText("No such user!");
            		publishProgress(bActivity);
    	    		return JSON_FAIL;
    		    } else if (statusCode == NO_SUCH_FOLLOWER_USERNAME) {
    	    		bActivity.setErrorText("Incorrect password!");
            		publishProgress(bActivity);
    	    		return JSON_FAIL;
    		    } else if (statusCode == USER_NOT_BROADCASTING) {
    		    	
    		    } else if (statusCode == INCORRECT_PASSWORD) {
    		    	
    		    }
        	} catch (JSONException e) {
    		    bActivity.setErrorText("JSON Error");
        		publishProgress(bActivity);
        		return JSON_FAIL;
        	}
        	return JSON_SUCCESS;
        }
        
        @Override
        protected void onPostExecute(String result) {
        	// Do nothing
        }
    	
    } 
     
}