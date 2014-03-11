package cs169.carfollowingapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

public class SimpleHTTPGETRequester {
	public static JSONObject makeHTTPGETRequest(String url) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			HttpResponse response = client.execute(request);
			HttpEntity ent = response.getEntity();
			InputStream is = ent.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
			} catch (IOException e) {
				throw new IOException("asd");
			} finally {
					is.close();
			}
			String string = sb.toString();
			is.close();
			String result = string.substring(0,string.length()-1);
			JSONObject jsonResult = new JSONObject(result);
			return jsonResult;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("I should not see this.");
		}
	    
	}
	
	
}
