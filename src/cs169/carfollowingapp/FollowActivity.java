package cs169.carfollowingapp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class FollowActivity extends MapActivity {

    private String followUrl = Constants.BASE_SERVER_URL + "api/follow";
    private String cancelUrl = Constants.BASE_SERVER_URL + "api/follow_cancellation";
    private int errCode;
    private String username;
    private Handler followHandler = new Handler();
    private Handler setFollowerPositionHandler = new Handler();
    private int frequency = 5000;
    private boolean update = false;
    protected static final int SUCCESS = 1;
    protected static final int SUCCESS_AND_CONTAINS_DIRECTIONS = 2;
    protected static final int AUTHENTICATION_FAILED = -1;
    protected static final int NO_SUCH_BROADCASTER = -3;
    protected static final int USER_NOT_BROADCASTING = -4;
    protected static final int ACCESS_NOT_PERMITTED = -5;
    
    /* Used to save currentLocation for AsyncTask. 
     * Maybe should change at some point since seems
     * somewhat hackish...
     */
    protected Location currentLocation;
    /* Same with the two below... */
    protected boolean progressSuccessful = true;
    protected CharSequence errorText;

    private Marker broadcaster;
    private Marker follower;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Singleton.getInstance().cookieCheck()) {
            Singleton.getInstance().clearCookies();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_follow);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.follow_map)).getMap();
        this.map.setMyLocationEnabled(true);

        Intent intent = getIntent();
        myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        myPassword = intent.getStringExtra(Constants.MY_P_KEY);
        username = intent.getStringExtra(Constants.U_KEY);
        new FollowTask().execute(this);
        new SetFollowerPositionTask().execute(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.follow, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        String msg;
        AlertDialog.Builder builder;
        switch (item.getItemId()) {
            case R.id.action_update:
                this.update = true;
                showToast("Request Processed");
                return true;
            case R.id.action_help:
                builder = new AlertDialog.Builder(this);
                msg = "You are now following a broadcaster!\n\nA corresponding marker will indicate your position and the broadcaster's position, and a path to follow to get to the broadcaster.\n\nWhenever you want to see the rest of the path to the broadcaster, hit Update to update the line!";
                builder.setMessage(msg)
                        .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void handleCleanup() {
    	followHandler.removeCallbacksAndMessages(null);
        setFollowerPositionHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
    	handleCleanup();
    	new CancelTask().execute(cancelUrl);
    }
    
    protected void setCurrentLocation(Location currLoc) {
    	progressSuccessful = true;
    	currentLocation = currLoc;
    }
    
    protected void setErrorText(CharSequence errTxt) {
    	progressSuccessful = false;
    	errorText = errTxt;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        handleCleanup();
	    new CancelTask().execute(cancelUrl);	    
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
                postData.put("update", update);
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
        
        /* code taken from http://stackoverflow.com/questions/14702621/answer-draw-path-between-two-points-using-google-maps-android-api-v2
         */
        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<LatLng>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng( (((double) lat / 1E5)),
                         (((double) lng / 1E5) ));
                poly.add(p);
            }

            return poly;
        }
        
        public void drawPath(String result) {
            try {
                    //Tranform the string into a json object
                   JSONObject jObj = new JSONObject(result);
                   JSONArray routeArray = jObj.getJSONArray("routes");
                   JSONObject routes = routeArray.getJSONObject(0);
                   JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
                   String encodedString = overviewPolylines.getString("points");
                   List<LatLng> list = decodePoly(encodedString);

                   for(int z = 0; z<list.size()-1;z++){
                        LatLng src= list.get(z);
                        LatLng dest= list.get(z+1);
                        Polyline line = map.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude, dest.longitude))
                        .width(3)
                        .color(Color.BLUE).geodesic(true));
                    }

            } 
            catch (JSONException e) {
                Log.d("DRAWPATH", e.getLocalizedMessage());
            }
        } 
        
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(),result, Toast.LENGTH_LONG).show();
            JSONObject fin;
            try {
                if (result == "JSON_EXCEPTION") {
                    handleError("JSON Error");
                    return;
                } else if (result == "RUNTIME_EXCEPTION") {
                    showToast("Connection Error");
                    followHandler.postDelayed(new Runnable() {
                        public void run() {
                            new FollowTask().execute(fActivity);
                        }
                    }, frequency);
                    return;
                } else if (result == "ERROR") {
                    handleError("Error");
                    return;
                }
                fin = new JSONObject(result);
                LatLng coord;
                errCode = fin.getInt("status code");
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case SUCCESS_AND_CONTAINS_DIRECTIONS:
                    case SUCCESS:
                        coord = new LatLng(fin.getDouble("latitude"), fin.getDouble("longitude"));
                        if (broadcaster == null) {
                            MarkerOptions marcOpt = new MarkerOptions().position(coord);
                            marcOpt.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                            broadcaster = map.addMarker(marcOpt);
                        } else {
                            broadcaster.setPosition(coord);
                        }
                        followHandler.postDelayed(new Runnable() {
                            public void run() {
                                new FollowTask().execute(fActivity);
                            }
                        }, frequency);
                        break;
                    
                       /* map.clear();
                        coord = new LatLng(fin.getDouble("latitude"), fin.getDouble("longitude"));
                        if (follower != null) {
                            follower = fActivity.plot(follower.getPosition(), false);
                        }
                        broadcaster = fActivity.plot(coord, false);
                        followHandler.postDelayed(new Runnable() {
                            public void run() {
                                new FollowTask().execute(fActivity);
                            }
                        }, frequency);
                        String jo = (String) fin.get("directions");
                        drawPath(jo);
                        break;
                        */
                    case AUTHENTICATION_FAILED:
                        handleError("Authentication failed.");
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

            } catch (JSONException je) {
                Log.d("JSONEXCEPTION", je.getMessage());
            } 
            /*
            catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
                handleError(e.getMessage());
            }
            */
        }
    }


    private class CancelTask extends AsyncTask<String, Void, String> {
    	private static final int SUCCESS = 1;
    	private static final int AUTHENTICATION_FAILED = -1;
    	private static final int NO_SUCH_BROADCASTER_USERNAME = -3;
    	private static final int USER_NOT_BROADCASTING = -4;
    	
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
                    handleError("JSON Error");
                    return;
                } else if (result == "RUNTIME_EXCEPTION") {
                    showToast("Connection Error");
                    return;
                } else if (result == "ERROR") {
                    handleError("Error");
                    return;
                }
                fin = new JSONObject(result);
                errCode = fin.getInt("status code");
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case SUCCESS:
                        break;
                    case AUTHENTICATION_FAILED:
                        handleError("Authentication failed.");
                        break;
                    case NO_SUCH_BROADCASTER_USERNAME:
                        showToast("No such broadcaster username.");
                        break;
                    case USER_NOT_BROADCASTING:
                        showToast("User not braodcasting.");
                        break;
                    default:
                        showToast("Unknown errCode.");
                        break;
                }
                finish();
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
                handleError(e.getMessage());
            }
        }
    }

    public void stopFollowing(View view) {
    	handleCleanup();
    	new CancelTask().execute(cancelUrl);
    }
    
    public void update(View view) {
        this.update = true;
        showToast("Request Processed");
    }

    /* Finds the current location of the application user, who is currently broadcasting,
     * and updates the database.
     */
    private class SetFollowerPositionTask extends AsyncTask<FollowActivity, FollowActivity, Integer> {
    	static final int GET_LOC_SUCCESS = 1;
    	static final int GET_LOC_FAIL = -1;
    	static final int BROADCAST_FREQUENCY = 10 * 1000;
    	private FollowActivity fActivity;

    	protected double debugLatitude = 37.0;
        protected double debugLongitude = -122.0;
        private boolean finishActivity = true;
        
        protected static final int SUCCESS = 1;
        protected static final int AUTHENTICATION_FAILED = -1;
        protected static final int USER_NOT_FOLLOWING = -2;
        protected static final int MALFORMED_LOCATION = -3;
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
            
            int result = getLocationAndUpdateDB(fActivity, service, provider);
            
            return result;
    	}
    	
    	protected int getLocationAndUpdateDB(FollowActivity fActivity, LocationManager service, String provider) {
    		String broadcastActionURL = "api/set_follower_position";
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
                debugLatitude = debugLatitude + 0.001;
                debugLongitude = debugLongitude + 0.001;
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
            	fActivity.setErrorText("Cannot get current location");
            	finishActivity = false;
            	publishProgress(fActivity);
    		    return GET_LOC_FAIL;
            }
            
            fActivity.setCurrentLocation(currentLocation);
            // Plot user location
            publishProgress(fActivity);
            
            // Preparing information to store in database
            JSONObject postData = new JSONObject();
            
        	try {
        		String latitude = Double.toString(currentLocation.getLatitude());
        		String longitude = Double.toString(currentLocation.getLongitude());
        		//postData.put("username", myUsername);
        		//postData.put("password", myPassword);
        		postData.put("latitude", latitude);
        		postData.put("longitude", longitude);
        	} catch (RuntimeException e) {
        	    Log.e("HTTPPOSTBroadcastAsyncTask", e.getMessage());
        	    fActivity.setErrorText("A RuntimeException has occurred");
        		publishProgress(fActivity);
        		return GET_LOC_FAIL;
    		} catch (JSONException e) {
    		    Log.e("HTTPPOSTBroadcastAsyncTask", e.getMessage());
    		    fActivity.setErrorText("JSON Error");
    		    publishProgress(fActivity);
        		return GET_LOC_FAIL;
    		} catch (Exception e) {
    		    Log.e("HTTPPOSTBroadcastAsyncTask", e.getMessage());
    	    	fActivity.setErrorText("A RuntimeException has occurred");
        		publishProgress(fActivity);
    		}
    		
    		JSONObject obj = null;
    		try {
    			obj = Singleton.getInstance().makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + broadcastActionURL, postData);
    		} catch (RuntimeException e) {
    			fActivity.setErrorText("Connection Error");
    			finishActivity = false;
        		publishProgress(fActivity);
    			return GET_LOC_FAIL;
    		}
    		int checkJSONResult = checkJSONResponse(fActivity, obj.toString());
    		if (checkJSONResult == GET_LOC_FAIL) {
    			return GET_LOC_FAIL;
    		} else {
    			return GET_LOC_SUCCESS;
    		}
    	}
    	
    	/* Checks if there were any errors indicated by the JSON response.
    	 * Returns 1 if there were no errors and returns -1 if there was an issue.
    	 */
        protected int checkJSONResponse(FollowActivity fActivity, String response) {
        	if (response == null) {
    		    fActivity.setErrorText("Unable to update database with current location");
        		publishProgress(fActivity);
        		return GET_LOC_FAIL;
        	}
        	
        	int statusCode = 0;
        	try {
        		JSONObject jsonResponse = new JSONObject(response);
        		statusCode = jsonResponse.getInt("status code");
        		if (statusCode == SUCCESS) {
    		        return GET_LOC_SUCCESS;
    		    } else if (statusCode == AUTHENTICATION_FAILED) {
    	    		fActivity.setErrorText("Authentication failed!");
            		publishProgress(fActivity);
    	    		return GET_LOC_FAIL;
    		    } else if (statusCode == USER_NOT_FOLLOWING) {
    	    		fActivity.setErrorText("User not following!");
            		publishProgress(fActivity);
    	    		return GET_LOC_FAIL;
    		    } else if (statusCode == MALFORMED_LOCATION) {
    	    		fActivity.setErrorText("malformed location!");
            		publishProgress(fActivity);
    	    		return GET_LOC_FAIL;
    		    }
        	} catch (JSONException e) {
    		    fActivity.setErrorText("JSON Error");
        		publishProgress(fActivity);
        		return GET_LOC_FAIL;
        	}
        	if (statusCode == SUCCESS) {
		        return GET_LOC_SUCCESS;
		    } else {
		    	fActivity.setErrorText("Unrecognized status code!");
		    	publishProgress(fActivity);
		    	return GET_LOC_FAIL;
		    }
	    }
        
        protected void onProgressUpdate(FollowActivity... followActivties) {
        	FollowActivity fActivity = followActivties[0];
        	
        	if (fActivity.progressSuccessful == false) {
        		if (finishActivity) {
        			fActivity.handleError(fActivity.errorText);
        		} else {
        			fActivity.handleError(fActivity.errorText);
        		}
        		finishActivity = true;
        		return;
        	}
        	Location currentLocation = fActivity.currentLocation;
        	// Plot user location
            LatLng userLocation = new LatLng(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude()
            );
            if (follower == null) {
                follower = fActivity.plot(userLocation, true);
            } else {
                follower.setPosition(userLocation);
            }
        }
        
        @Override
        protected void onPostExecute(Integer result) {
        	if (result == GET_LOC_SUCCESS) {
        		setFollowerPositionHandler.postDelayed(new Runnable() {
                    public void run() {
                    	new SetFollowerPositionTask().execute(fActivity);
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
