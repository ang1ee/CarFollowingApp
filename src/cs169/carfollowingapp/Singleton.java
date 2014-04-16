package cs169.carfollowingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by Steven on 4/15/14.
 */
public class Singleton {
    private static Singleton mInstance = null;

    private DefaultHttpClient mClient;

    private Singleton(){
        HttpParams params = new BasicHttpParams();
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, registry);
        mClient = new DefaultHttpClient(cm, params);
    }

    public static Singleton getInstance(){
        if(mInstance == null)
        {
            mInstance = new Singleton();
        }
        return mInstance;
    }

    public JSONObject makeHTTPPOSTRequest(String url, JSONObject postData) {
        try {
            HttpPost p = new HttpPost(url);
            StringEntity se = new StringEntity(postData.toString());
            p.setEntity(se);
            p.setHeader("Accept", "application/json");
            p.setHeader("Content-type", "application/json");

            HttpResponse r = (HttpResponse) mClient.execute(p);
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

    public boolean cookieCheck(Context context) {
        List<Cookie> cookies = mClient.getCookieStore().getCookies();
        if (cookies != null) {
            return true;
        }
        return false;
    }

    public void clearCookies(Context context) {
        mClient.getCookieStore().clear();
    }

//    public String getString(){
//        return this.mString;
//    }
//
//    public void setString(String value){
//        mString = value;
//    }
}

