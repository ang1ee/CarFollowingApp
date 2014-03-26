package cs169.carfollowingapp;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


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
	private int errCode;
	private ArrayList<LatLng> coords = new ArrayList<LatLng>();
	private String username;
    private Timer followTimer = new Timer();
    private int frequency = 100000;
	protected static final int SUCCESS = 1;
    protected static final int NO_SUCH_USER = -1;
    protected static final int USER_NOT_BROADCASTING = -2;
    protected static final int JSON_EXCEPTION = -3;
    protected static final int CONNECTION_ERROR = -4;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.follow_map)).getMap();
        this.map.setMyLocationEnabled(true);

        Intent intent = getIntent();
        myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        myPassword = intent.getStringExtra(Constants.MY_P_KEY);
        username = intent.getStringExtra(Constants.U_KEY);

        //new HttpAsyncTask().execute(loginUrl);

        FollowTask follow = new FollowTask();
//        public void schedule (TimerTask task, long delay, long period)
//        Schedule a task for repeated fixed-delay execution after a specific delay.
//
//        Parameters
//        task  the task to schedule.
//        delay  amount of time in milliseconds before first execution.
//        period  amount of time in milliseconds between subsequent executions.

        followTimer.schedule(follow, 0, frequency);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.follow, menu);
        return true;
    }

    class FollowTask extends TimerTask {
        public void run() {
            JSONObject postData = new JSONObject();
            String result;
            try {
                postData.put("username", username);
                JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/follow", postData);
                result = obj.toString();
            } catch (JSONException e) {
                result = "JSON_EXCEPTION";
            } catch (RuntimeException e) {
                result = "RUNTIME_EXCEPTION";
            } catch (Exception e) {
                result = "ERROR";
            }

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

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

        		JSONObject postData = new JSONObject();
        		try {
					postData.put("username", username);
					JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(urls[0], postData);
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

            if (errCode == SUCCESS) {
                Intent intent = new Intent(getApplicationContext(), FrontPageActivity.class);
                intent.putExtra(Constants.MY_U_KEY, myUsername);
                intent.putExtra(Constants.MY_P_KEY, myPassword);
                followTimer.cancel();
                startActivity(intent);
                finish();
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
        new HttpAsyncTask().execute(Constants.BASE_SERVER_URL + "api/follow_cancellation");
	}

    @Override
    public void onBackPressed() {
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
