package cs169.carfollowingapp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
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

import cs169.carfollowingapp.BroadcastActivity.HTTPPOSTBroadcastAsyncTask;



public class FollowActivity extends MapActivity {

    private String followUrl = Constants.BASE_SERVER_URL + "api/follow";
    private String cancelUrl = Constants.BASE_SERVER_URL + "api/follow_cancellation";
    private int errCode;
    private String username;
    private Handler handler = new Handler();
    private int frequency = 5000;
    protected static final int SUCCESS = 1;
    protected static final int NO_SUCH_USER = -1;
    protected static final int INCORRECT_PASSWORD = -2;
    protected static final int NO_SUCH_BROADCASTER = -3;
    protected static final int USER_NOT_BROADCASTING = -4;
    protected static final int ACCESS_NOT_PERMITTED = -5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!GeneralMethods.cookieCheck(getApplicationContext())) {
            GeneralMethods.clearCookies(getApplicationContext());
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_follow);
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.follow_map)).getMap();
        this.map.setMyLocationEnabled(true);

        Intent intent = getIntent();
        myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        myPassword = intent.getStringExtra(Constants.MY_P_KEY);
        username = intent.getStringExtra(Constants.U_KEY);
        new FollowTask().execute(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.follow, menu);
        return true;
    }

    protected void quit(Intent intent) {
        startActivity(intent);
        handler.removeCallbacksAndMessages(null);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), FrontPageActivity.class);
        quit(intent);
    }

    private class FollowTask extends AsyncTask<FollowActivity, Void, String> {
        private FollowActivity fActivity;
        @Override
        protected String doInBackground(FollowActivity... followActivities) {
            fActivity = followActivities[0];
            JSONObject postData = new JSONObject();
            try {
                postData.put(Constants.MY_U_KEY, myUsername);
                postData.put(Constants.MY_P_KEY, myPassword);
                postData.put("username", username);
                JSONObject obj = Singleton.getInstance().makeHTTPPOSTRequest(followUrl, postData);
                return obj.toString();
            } catch (JSONException e) {
                return "JSON_EXCEPTION";
            } catch (RuntimeException e) {
                return "RUNTIME_EXCEPTION";
            } catch (Exception e) {
                return "ERROR";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(),result, Toast.LENGTH_LONG).show();
            JSONObject fin;
            try {
                if (result == "JSON_EXCEPTION") {
                    handleError("JSON Error");
                } else if (result == "RUNTIME_EXCEPTION") {
                    handleError("Connection Error");
                } else if (result == "ERROR") {
                    handleError("Error");
                }
                fin = new JSONObject(result);
                errCode = fin.getInt("status code");
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case SUCCESS:
                        ArrayList<LatLng> coords = new ArrayList<LatLng>();
                        coords.add(new LatLng(fin.getDouble("latitude"), fin.getDouble("longitude")));
                        fActivity.plot(coords);
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                new FollowTask().execute(fActivity);
                            }
                        }, frequency);
                        break;
                    case NO_SUCH_USER:
                        showToast("User does not exist.");
                        break;
                    case INCORRECT_PASSWORD:
                        showToast("Username and password do not match.");
                        break;
                    case NO_SUCH_BROADCASTER:
                        showToast("Broadcaster does not exist.");
                        break;
                    case USER_NOT_BROADCASTING:
                        showToast("User is not broadcasting.");
                        break;
                    case ACCESS_NOT_PERMITTED:
                        showToast("No permission to follow user.");
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


    private class CancelTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            JSONObject postData = new JSONObject();
            try {
                //TODO: Adjust JSON parameters to match spec
                postData.put(Constants.MY_U_KEY, myUsername);
                postData.put(Constants.MY_P_KEY, myPassword);
                postData.put("username", username);
                JSONObject obj = Singleton.getInstance().makeHTTPPOSTRequest(urls[0], postData);
                return obj.toString();
            } catch (JSONException e) {
                return "JSON_EXCEPTION";
            } catch (RuntimeException e) {
                return "RUNTIME_EXCEPTION";
            } catch (Exception e) {
                return "ERROR";
            }


        }
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(),result, Toast.LENGTH_LONG).show();
            JSONObject fin;
            try {
                if (result == "JSON_EXCEPTION") {
                    showToast("JSON Error");
                } else if (result == "RUNTIME_EXCEPTION") {
                    showToast("Connection Error");
                } else if (result == "ERROR") {
                    showToast("Error");
                }
                fin = new JSONObject(result);
                errCode = fin.getInt("status code");
                Intent intent = new Intent(getApplicationContext(), FrontPageActivity.class);
                intent.putExtra(Constants.MY_U_KEY, myUsername);
                intent.putExtra(Constants.MY_P_KEY, myPassword);
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case SUCCESS:
                        break;
                    case NO_SUCH_USER:
                        showToast("User does not exist.");
                        break;
                    case INCORRECT_PASSWORD:
                        showToast("Username and password do not match.");
                        break;
                    case NO_SUCH_BROADCASTER:
                        showToast("Broadcaster does not exist.");
                        break;
                    case USER_NOT_BROADCASTING:
                        break;
                    default:
                        showToast("Unknown errCode.");
                        break;
                }
                quit(intent);
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
                handleError(e.getMessage());
            }
        }
    }

    public void stopFollowing(View view) {
        new CancelTask().execute(cancelUrl);
    }

    /* Finds the current location of the application user, who is currently broadcasting,
     * and updates the database.
     */
    private class SetFollowerPositionTask extends AsyncTask<FollowActivity, FollowActivity, Integer> {
    	static final int GET_LOC_SUCCESS = 1;
    	static final int GET_LOC_FAIL = -1;
    	static final int BROADCAST_FREQUENCY = 10 * 1000;
    	private FollowActivity fActivity;
    	//private String myUsername;
    	//private String myPassword;
    		
    	
    	@Override
        protected Integer doInBackground(FollowActivity... followActivities) {
    		fActivity = followActivities[0];
    		//myUsername = bActivity.myUsername;
    		//myPassword = bActivity.myPassword;
    		
    		// Getting the location of user
            LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = service.getBestProvider(criteria, false);
            
            int result = getLocationAndUpdateDB(bActivity, service, provider);
            
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
    			obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + broadcastActionURL, postData, getApplicationContext());
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
        	if (result == GET_LOC_SUCCESS) {
        		broadcastHandler.postDelayed(new Runnable() {
                    public void run() {
                    	new HTTPPOSTBroadcastAsyncTask().execute(bActivity);
                    }
                }, BROADCAST_FREQUENCY);
        	}
        }
        
    }


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
