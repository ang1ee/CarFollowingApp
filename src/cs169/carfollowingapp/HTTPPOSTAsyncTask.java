package cs169.carfollowingapp;

import org.json.JSONObject;

import android.os.AsyncTask;

/* Send an HTTP POST request through AsyncTask. */
public abstract class HTTPPOSTAsyncTask extends AsyncTask<JSONObject, Void, String> {

	@Override
    protected String doInBackground(JSONObject... jsonObjects) {
		JSONObject postData = jsonObjects[0];
		JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/stop_broadcast", postData);
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
	
}
