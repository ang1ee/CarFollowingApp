package cs169.carfollowingapp;

import org.json.JSONObject;

import android.os.AsyncTask;

public abstract class HTTPPOSTAsyncTask extends AsyncTask<JSONObject, Void, String> {

	@Override
    protected String doInBackground(JSONObject... jsonObjects) {
		JSONObject postData = jsonObjects[0];
		JSONObject obj = SimpleHTTPPOSTRequester.makeHTTPPOSTRequest(Constants.BASE_SERVER_URL + "api/stop_broadcast", postData);
		return obj.toString();
    }
	
}
