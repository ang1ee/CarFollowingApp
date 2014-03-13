package cs169.carfollowingapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;


/*
 * Source: http://www.gnovus.com/blog/programming/making-http-post-request-json-using-apaches-httpclient
 */

public class SimpleHTTPPOSTRequester {

	public static JSONObject makeHTTPPOSTRequest(String url,
			JSONObject postData) {
		try {
			HttpClient c = new DefaultHttpClient(); 
			HttpPost p = new HttpPost(url);
			StringEntity se = new StringEntity(postData.toString());
			p.setEntity(se);
			p.setHeader("Accept", "application/json");
			p.setHeader("Content-type", "application/json");
			
			HttpResponse r = (HttpResponse) c.execute(p);
			HttpEntity ent = r.getEntity();
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
			throw new RuntimeException("Connection Error.");
		}
		
	}

	
}
