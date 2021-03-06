package cs169.carfollowingapp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class BroadcastActivity extends MapActivity {

	protected HashMap<String, Marker> followerCircleDict = new HashMap<String, Marker>();
	protected HashMap<String, Float> followerColorMap = new HashMap<String, Float>();
	
	protected static final int SUCCESS = 1;
    protected static final int NO_SUCH_USER = -1;
    protected static final int INCORRECT_PASSWORD = -2;
    protected static final int MALFORMED_LOCATION = -3;
    
    private Handler broadcastHandler = new Handler();
    private Handler followRequestHandler = new Handler();
    private Handler getFollowPositionsHandler = new Handler();
    private int nextColorNum = 0;
    private float[] colors = {
            BitmapDescriptorFactory.HUE_AZURE,
            BitmapDescriptorFactory.HUE_BLUE,
            BitmapDescriptorFactory.HUE_CYAN,
            BitmapDescriptorFactory.HUE_GREEN,
            BitmapDescriptorFactory.HUE_MAGENTA,
            BitmapDescriptorFactory.HUE_ORANGE,
            BitmapDescriptorFactory.HUE_RED,
            BitmapDescriptorFactory.HUE_ROSE,
            BitmapDescriptorFactory.HUE_VIOLET,
            BitmapDescriptorFactory.HUE_YELLOW
    }; 
    
    /* Used to save currentLocation for AsyncTask. 
     * Maybe should change at some point since seems
     * somewhat hackish...
     */
    protected Location currentLocation;
    /* Same with the four below... */
    protected boolean progressSuccessful = true;
    protected CharSequence errorText;
    protected LinkedList<String[]> followRequestUsers = new LinkedList<String[]>();
    protected String[] followerTuple = new String[2];
    protected double debugLatitude = 37.866750;
    protected double debugLongitude = -122.262074;
    protected boolean invitationAccepted = false;
    
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    
    private Marker broadcaster;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);

        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 
                R.drawable.ic_drawer, R.string.open_drawer, R.string.close_drawer) {
            
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
                
                // creates call to onPrepareOptionsMenu
                invalidateOptionsMenu();
            }
            
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(mDrawerTitle);
                
                mDrawerList.setAdapter(new FollowerListAdapter(
                        getApplicationContext(),
                        R.layout.item_follower, 
                        followerColorMap.entrySet().toArray((Map.Entry<String, Float>[]) new Map.Entry[]{}))
                );
                
                // creates call to onPrepareOptionsMenu
                invalidateOptionsMenu();
            }
        };
        
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        
        mDrawerList.setAdapter(new FollowerListAdapter(
                this,
                R.layout.item_follower, 
                followerColorMap.entrySet().toArray((Map.Entry<String, Float>[]) new Map.Entry[]{}))
        );
        
        
        // mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        this.map = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.broadcast_map)).getMap();
        this.map.setMyLocationEnabled(true);

        if (!Singleton.getInstance().cookieCheck()) {
            Singleton.getInstance().clearCookies();
            Intent intent = new Intent(getApplicationContext(), CFA.class);
            startActivity(intent);
            finish();
        }

        Intent intent = getIntent();
        myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        myPassword = intent.getStringExtra(Constants.MY_P_KEY);
        
        new HTTPPOSTBroadcastAsyncTask().execute(this);
        new HTTPPOSTGetFollowRequestsAsyncTask().execute(this);
        new GetFollowPositionsTask().execute(this);
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        String msg;
        AlertDialog.Builder builder;
        int id = item.getItemId();
        if (id == R.id.action_help) {
            builder = new AlertDialog.Builder(this);
            msg = "You are broadcasting your location!\n\nOn the map, you're location is indicated by the red-orange marker.  Check the legend of marker color to username for your followers by hitting the top-left icon.\n\nWhen you're done broadcasting, hit Stop Broadcasting!";
            builder.setMessage(msg)
                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
            return true;
        }
        // Handle your other action bar items
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }
    
    // override onPrepareOptionsMenu to hide action items
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.broadcast, menu);
        return true;
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
        handleCleanup();
    	 
	    new HTTPPOSTStopBroadcastingAsyncTask().execute(this);
	    
    }
    
    public void stopBroadcasting(View view) {
        stopBroadcasting();
    }

    @Override
    public void onBackPressed() {
        stopBroadcasting();
    }

    public void stopBroadcasting() {
	    finish();
	}
    
    protected String[] getFollowerTuple() {
    	return followerTuple;
    }
    
    protected void setCurrentLocation(Location currLoc) {
    	progressSuccessful = true;
    	currentLocation = currLoc;
    }
    
    protected void setErrorText(CharSequence errTxt) {
    	progressSuccessful = false;
    	errorText = errTxt;
    }
    
    protected void setFollowRequestUsers(LinkedList<String[]> followers) {
    	progressSuccessful = true;
    	followRequestUsers = followers;
    }
    
    public void invitationResponse() {
    	new HTTPPOSTInvitationResponseAsyncTask().execute(this);
    }
    
    public void showFollowRequestDialog() {
    	if (followRequestUsers.size() <= 0) {
    		return;
    	}
    	followerTuple = followRequestUsers.remove();
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
        
    /* Finds the current location of the application user, who is currently broadcasting,
     * and updates the database.
     */
    private class HTTPPOSTBroadcastAsyncTask extends AsyncTask<BroadcastActivity, BroadcastActivity, Integer> {
    	static final int GET_LOC_SUCCESS = 1;
    	static final int GET_LOC_FAIL = -1;
    	static final int BROADCAST_FREQUENCY = 10 * 1000;
    	private BroadcastActivity bActivity;
    	private String myUsername;
    	private String myPassword;
    		
    	
    	@Override
        protected Integer doInBackground(BroadcastActivity... broadcastActivties) {
    		bActivity = broadcastActivties[0];
    		myUsername = bActivity.myUsername;
    		myPassword = bActivity.myPassword;
    		
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
                Log.d("LATITUDE", "" + debugLatitude);
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
    			obj = Singleton.getInstance().makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + broadcastActionURL, postData);
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
            if (broadcaster == null) {
                broadcaster = bActivity.plot(userLocation, true);
            } else {
                broadcaster.setPosition(userLocation);
            }
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
    
    /* Receives follow requests for the broadcaster. */
    private class HTTPPOSTGetFollowRequestsAsyncTask extends AsyncTask<BroadcastActivity, BroadcastActivity, Integer> {
    	static final int SUCCESS = 1;
    	static final int AUTHENTICATION_FAILED = -1;
    	static final int USER_NOT_BROADCASTING = -3;
    	
    	static final int REQUEST_CHECK_FREQUENCY = 5 * 1000;
    	static final int CHECK_FOLLOW_REQS_SUCCESS = 1;
    	static final int CHECK_FOLLOW_REQS_FAIL = -1;
    	static final int CHECK_FOLLOW_REQS_FAIL_BUT_CONTINUE = -2;
    	private String myUsername;
    	private String myPassword;
    	private BroadcastActivity bActivity;
    	private boolean finishActivity = true;
    	//private boolean foundFollowers = false;
    	    	
    	@Override
        protected Integer doInBackground(BroadcastActivity... broadcastActivties) {
    		bActivity = broadcastActivties[0];
    		myUsername = bActivity.myUsername;
    		myPassword = bActivity.myPassword;
    		
            int result = checkFollowRequests(bActivity);

            return result;
    	}
    	
    	protected int checkFollowRequests(BroadcastActivity bActivity) {
    		String actionURL = "api/get_follow_requests";

            // Preparing information to store in database
            JSONObject postData = new JSONObject();
            
        	try {
        		postData.put("myUsername", myUsername);
        		postData.put("myPassword", myPassword);        		
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
    			obj = Singleton.getInstance().makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + actionURL, postData);
    		} catch (RuntimeException e) {
    			bActivity.setErrorText("Connection Error");
    			finishActivity = false;
        		publishProgress(bActivity);
    			return CHECK_FOLLOW_REQS_FAIL_BUT_CONTINUE;
    		}
    		String JSONString = obj.toString();
    		int checkJSONResult = checkJSONResponse(bActivity, JSONString);
    		if (checkJSONResult == CHECK_FOLLOW_REQS_FAIL) {
    			return CHECK_FOLLOW_REQS_FAIL;
    		} else {
    			try {
    				followers = obj.getJSONArray("follow requests");
    				putFollowReqsInList(bActivity, followers);
    				//publishProgress(bActivity);
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
    				JSONArray followerJSON = followers.getJSONArray(i);
    				String[] follower = new String[2];
    				follower[0] = followerJSON.getString(0);
    				follower[1] = followerJSON.getString(1);
    				bActivity.followRequestUsers.add(follower);
    			} catch (JSONException e) {
    				throw e;
    			}
    		}
    		/*
    		if (bActivity.followRequestUsers.size() > 0) {
    			foundFollowers = true;
    		}
    		*/
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
        		if (statusCode == AUTHENTICATION_FAILED) {
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
        		if (finishActivity) {
        			bActivity.handleError(bActivity.errorText);
        		} else {
        			bActivity.showToast(bActivity.errorText);
        		}
        		finishActivity = true;
        		return;
        	}
        	
        	/*
        	if (bActivity.followRequestUsers.size() > 0) {
        		bActivity.showFollowRequestDialog();
        	}
        	*/
        }
        
        @Override
        protected void onPostExecute(Integer result) {
        	if ((result == CHECK_FOLLOW_REQS_SUCCESS) || (result == CHECK_FOLLOW_REQS_FAIL_BUT_CONTINUE)) {
        		if (bActivity.followRequestUsers.size() > 0) {
        			bActivity.showFollowRequestDialog();
        			return;
        		}
        		bActivity.followRequestHandler.postDelayed(new Runnable() {
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
    			obj = Singleton.getInstance().makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/stop_broadcast", postData);
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
    			bActivity.showToast(text);
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
    	BroadcastActivity bActivity;
    	private boolean finishActivity = true;
    	    	
    	@Override
        protected String doInBackground(BroadcastActivity... broadcastActivities) {
    		bActivity = broadcastActivities[0];
    		if (bActivity.invitationAccepted == false) {
    			return null;
    		}
    		String actionURL = "/api/invitation_response";
    		
    		JSONObject postData = new JSONObject();
    		try {
        		postData.put("myUsername", bActivity.myUsername);
        		postData.put("myPassword", bActivity.myPassword);
        		postData.put("username", bActivity.followerTuple[0]);
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
    			obj = Singleton.getInstance().makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + actionURL, postData);
    		} catch (RuntimeException e) {
    			Log.e("HTTPPOSTInvitationResponseAsyncTask", e.getMessage());
        	    bActivity.setErrorText("Connection error");
        	    finishActivity = false;
        		publishProgress(bActivity);
        		return null;
    		}

    		checkJSONResponse(bActivity, obj.toString());
    		return null;
    	}
    	
    	protected void onProgressUpdate(BroadcastActivity... broadcastActivties) {
        	BroadcastActivity bActivity = broadcastActivties[0];
        	
        	if (bActivity.progressSuccessful == false) {
        		if (finishActivity) {
        			bActivity.handleError(bActivity.errorText);
        		} else {
        			bActivity.showToast(bActivity.errorText);
        		}
        		finishActivity = true;
        		return;
        	}
        }
    	
    	/* Checks if there were any errors indicated by the JSON response.
    	 * Returns 1 if there were no errors and returns -1 if there was an issue.
    	 */
        protected int checkJSONResponse(BroadcastActivity bActivity, String response) {
        	if (response == null) {
    		    bActivity.setErrorText("Unable to respond to invitation.");
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
        	bActivity.followRequestHandler.postDelayed(new Runnable() {
                public void run() {
                	new HTTPPOSTGetFollowRequestsAsyncTask().execute(bActivity);
                }
            }, 5 * 1000);
        }
    	
    } 
     
    private class GetFollowPositionsTask extends AsyncTask<BroadcastActivity, Void, String> {
        private BroadcastActivity bActivity;
        private String getFollowerPositions = Constants.BASE_SERVER_URL + "api/get_follower_positions";
        private int frequency = 10 * 1000;
        
        static final int SUCCESS = 1;
        static final int AUTHENTICATION_FAILED = -1;
        static final int USER_NOT_BROADCASTING = -3;
        
        @Override
        protected String doInBackground(BroadcastActivity... followActivities) {
            bActivity = followActivities[0];
            try {
                JSONObject obj = Singleton.getInstance().makeHTTPGETRequest(getFollowerPositions);
                return obj.toString();
            } catch (RuntimeException e) {
                Log.d("runtimeException", e.getLocalizedMessage());
                return "RUNTIME_EXCEPTION";
            }
        }
        
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(),result, Toast.LENGTH_LONG).show();
            JSONObject fin;
            try {
                if (result == "RUNTIME_EXCEPTION") {
                    bActivity.showToast("Connection Error");
                }
                fin = new JSONObject(result);
                int errCode = fin.getInt("status code");
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case SUCCESS:
                        JSONObject followerPositionsDict = fin.getJSONObject("user positions");
                        plotFollowers(followerPositionsDict);
                        getFollowPositionsHandler.postDelayed(new Runnable() {
                            public void run() {
                                new GetFollowPositionsTask().execute(bActivity);
                            }
                        }, frequency);
                        break;
                    case AUTHENTICATION_FAILED:
                        bActivity.handleError("Authentication failed.");
                        break;
                    case USER_NOT_BROADCASTING:
                        bActivity.handleError("User not broadcasting.");
                        break;
                    default:
                        bActivity.handleError("Unknown errCode.");
                        break;
                }
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
                bActivity.handleError(e.getMessage());
            }
        }
        
        void plotFollowers(JSONObject followerPositionsDict) {
        	Iterator<String> followers = (Iterator<String>) followerPositionsDict.keys();
        	try {
        		HashSet<String> currentFollowers = new HashSet<String>();
        		while (followers.hasNext()) {
        			String follower = followers.next();
        			currentFollowers.add(follower);
        			JSONArray positionArr = followerPositionsDict.getJSONArray(follower);
        			Double lat = positionArr.getDouble(0);
        			Double lng = positionArr.getDouble(1);
        			LatLng position = new LatLng(lat, lng);
        			
        			if (followerCircleDict.containsKey(follower)) {
        				Marker m =  followerCircleDict.get(follower);
        				m.setPosition(position);
        			} else {
        				MarkerOptions marcOpt = new MarkerOptions().position(position);
            			nextColorNum = (nextColorNum + 1) % colors.length;
                        marcOpt.icon(BitmapDescriptorFactory.defaultMarker(colors[nextColorNum]));
        				Marker m = bActivity.map.addMarker(marcOpt);
        				followerColorMap.put(follower, new Float(colors[nextColorNum]));
        				followerCircleDict.put(follower, m);
        			}
        		}
        		deleteExFollowers(currentFollowers);
        	} catch (JSONException e) {
        	    Log.d("plotFollowers", e.getLocalizedMessage());
        		handleError("JSON exception.");
        	}
        }
        
        void deleteExFollowers(Set<String> currentFollowers) {
        	/* Must make a new HashSet in order to not affect followerMarkers.Dict */
        	HashSet<String> oldFollowers = new HashSet<String>(followerCircleDict.keySet());
        	oldFollowers.removeAll(currentFollowers);
        	Iterator<String> iter  = oldFollowers.iterator();
        	while (iter.hasNext()) {
        		String exFollower = iter.next();
        		Marker m = followerCircleDict.get(exFollower);
        		m.remove();
        		followerCircleDict.remove(exFollower);
        		followerColorMap.remove(exFollower);
        	}
        }
        
    }
    
}