
package cs169.carfollowingapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity {

    EditText etUsername,etPassword,etEmail;
    Button btnRegister;
    String username, password, email;

    //Destination addresses for the login and add location of the server
    String registerUrl = Constants.BASE_SERVER_URL + "api/create_user";

    //the errCode and count returned in the JSON response by the database.
    int errCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.container, new PlaceholderFragment())
//                    .commit();
//        }

        // get reference to the views
        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);
        etEmail = (EditText) findViewById(R.id.etEmail);
        btnRegister = (Button) findViewById(R.id.btnRegister);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // add click listener to Button "Register". Sends a POST request to users/register
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = etUsername.getText().toString();
                password = etPassword.getText().toString();
                email = etEmail.getText().toString();
                Toast.makeText(getBaseContext(), "Creating user...", Toast.LENGTH_LONG).show();
                new HttpAsyncTask().execute(registerUrl);
            }
        });
    }

    // Displays toast showing the text argument.
 	protected void showToast(CharSequence text) {
 	    Context context = getApplicationContext();
 	    int duration = Toast.LENGTH_SHORT;
 	    
 	    Toast toast = Toast.makeText(context, text, duration);
 	    toast.show();
 	}
    
    //Responsible for sending out the Post request on a different thread.
    //Takes the Username and Password information from the text field
    //to send it out to the server for add or login.
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            JSONObject postData = new JSONObject();
            try {
                postData.put(Constants.U_KEY, username);
                postData.put(Constants.P_KEY, password);
                postData.put(Constants.E_KEY, email);
                JSONObject obj = Singleton.getInstance().makeHTTPPOSTRequest(urls[0], postData);
                return obj.toString();
            } catch (JSONException e) {
                return "JSON_EXCEPTION";
            } catch (RuntimeException e) {
                return "RUNTIME_EXCEPTION";
            } catch (Exception e) {
                return "ERROR";
            }
//            String result = POST(urls[0],user);
//            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(),result, Toast.LENGTH_LONG).show();
            JSONObject fin;
            try {
            	if (result == "JSON_EXCEPTION") {
                    showToast("JSON Error");
                    return;
                } else if (result == "RUNTIME_EXCEPTION") {
                    showToast("Connection Error");
                    return;
                } else if (result == "ERROR") {
                    showToast("Error");
                    return;
                }
                fin = new JSONObject(result);
                errCode = fin.getInt("status code");
                String message = "";
                
                //Updates the message on the Log In page, depending on the database response.
                switch (errCode) {
                    case -4:
                        message = "Malformed Email";
                        break;
                    case -3:
                        message = "Password must have 1 to 128 characters";
                        break;
                    case -2:
                        message = "Username must have 1 to 128 characters";
                        break;
                    case -1:
                        message = "User already exists";
                        break;
                    case 0:
                        message = "Error";
                        break;
                    default:
                        message = "User registered successfully" + errCode;
                        break;
                }
                Context context = getApplicationContext();
                CharSequence text = message;
                int duration = Toast.LENGTH_SHORT;

                Toast.makeText(context, text, duration).show();
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }

          //Opens a new page in response to a successful add user.
            if (errCode == 1) { 
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

//    //Packages and sends the POST request to the database url, with information from user.
//    public static String POST(String url, User user){
//        InputStream inputStream = null;
//        String result = "";
//        try {
//
//            // create HttpClient
//            HttpClient httpclient = new DefaultHttpClient();
//
//            // make POST request to the given URL
//            HttpPost httpPost = new HttpPost(url);
//
//            String json = "";
//
//            // build jsonObject
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("username", user.getUsername());
//            jsonObject.put("password", user.getPassword());
//
//            //  convert JSONObject to JSON to String
//            json = jsonObject.toString();
//
//            // ** Alternative way to convert Person object to JSON string usin Jackson Lib
//            // ObjectMapper mapper = new ObjectMapper();
//            // json = mapper.writeValueAsString(person);
//
//            // set json to StringEntity
//            StringEntity se = new StringEntity(json);
//            //se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
//
//            // set httpPost Entity
//            httpPost.setEntity(se);
//
//            // Set some headers to inform server about the type of the content
//            httpPost.setHeader("Accept", "application/json");
//            httpPost.setHeader("Content-type", "application/json");
//
//            // Execute POST request to the given URL
//            HttpResponse httpResponse = httpclient.execute(httpPost);
//
//            // receive response as inputStream
//            inputStream = httpResponse.getEntity().getContent();
//
//            // convert inputstream to string
//            if(inputStream != null)
//                result = convertInputStreamToString(inputStream);
//            else
//                result = "Did not work!";
//
//        } catch (Exception e) {
//            Log.d("InputStream", e.getLocalizedMessage());
//        }
//        // 11. return result
//        return result;
//    }
//
//    //Converts the data in the InputStream into the result string. Used to change the Http response
//    //from the database into an easier to read format.
//    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
//        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
//        String line = "";
//        String result = "";
//        while((line = bufferedReader.readLine()) != null)
//            result += line;
//
//        inputStream.close();
//        return result;
//
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
//    public static class PlaceholderFragment extends Fragment {
//
//        public PlaceholderFragment() {
//        }
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                                 Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_login, container, false);
//            return rootView;
//        }
//    }

}

