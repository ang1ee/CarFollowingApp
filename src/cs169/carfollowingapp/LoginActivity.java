package cs169.carfollowingapp;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class LoginActivity extends Activity {

    TextView tvMessage;
    EditText etUsername,etPassword;
    Button btnLogin, btnRegister, btnForgotPassword;
    String username, password;

    //Destination addresses for the login and add location of the server
    String loginUrl = Constants.BASE_SERVER_URL + "api/login";

    //the errCode and count returned in the JSON response by the database.
    int errCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /*
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        */

        //XXX Auto login code
        
        /*
        Context context = getApplicationContext();
        String usernameFilePath = context.getFilesDir().getAbsolutePath()+"/" + Constants.U_FILE_NAME;
        String passwordFilePath= context.getFilesDir().getAbsolutePath()+"/" + Constants.U_FILE_NAME;
        File usernameFile = new File(usernameFilePath);
        File passwordFile = new File(passwordFilePath);
        
        try {
        	if (usernameFile.exists()) {
        		if (passwordFile.exists()) {
        			username = readInternalFile(Constants.U_FILE_NAME);
        			password = readInternalFile(Constants.P_FILE_NAME);
        			new HttpAsyncTask().execute(loginUrl);
        		} else {
        			usernameFile.delete();
        		}
        	} else {
        		passwordFile.delete();
        	}
        } catch (IOException e) {
        	showToast("Cannot read login info files");
        }
        */
        
        // get reference to the views
        tvMessage = (TextView) findViewById(R.id.tvMessage);
        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnForgotPassword = (Button) findViewById(R.id.btnForgotPassword);

        // add click listener to Button "Login". Sends a POST request to users/login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = etUsername.getText().toString();
                password = etPassword.getText().toString();
                new HttpAsyncTask().execute(loginUrl);
            }
        });
        
     // add click listener to Button "Forgot Password?". Routes to ForgotPassword Activity
        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	Intent intent = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        // add click listener to Button "Login". Routes to Register Activity
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
            }
        });    
        
    }
    
    String readInternalFile(String filename) throws IOException {
    	ArrayList<Byte> bytes = new ArrayList<Byte>();
    	Integer currByte = openFileInput(filename).read();
    	while (currByte != -1) {
    		bytes.add(currByte.byteValue());
    	}
    	byte[] byteArr = new byte[bytes.size()];
    	for (int i = 0; i < bytes.size(); i++) {
    		byteArr[i] = bytes.get(i);
    	}
    	String contents =  new String(byteArr);
    	return contents;
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
//            User user = new User();
//            user.setUsername(etUsername.getText().toString());
//            user.setPassword(etPassword.getText().toString());
//            String result = POST(urls[0],user,getApplicationContext());
            JSONObject postData = new JSONObject();
            try {
                postData.put(Constants.U_KEY, username);
                postData.put(Constants.P_KEY, password);
                JSONObject obj = Singleton.getInstance().makeHTTPPOSTRequest(urls[0], postData);
                return obj.toString();
            } catch (JSONException e) {
                return "JSON_EXCEPTION";
            } catch (RuntimeException e) {
                return "RUNTIME_EXCEPTION";
            } catch (Exception e) {
                return "ERROR";
            }
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
                String message;
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case -2:
                        message = "Incorrect password.";
                        break;
                    case -1:
                        message = "User does not exist.";
                        break;
                    case 0:
                        message = "Error";
                        break;
                    default:
                        message = "Login successful.";
                        
                        /*
                        Context context = getApplicationContext();
                        String usernameFilePath = context.getFilesDir().getAbsolutePath()+"/" + Constants.U_FILE_NAME;
                        String passwordFilePath= context.getFilesDir().getAbsolutePath()+"/" + Constants.U_FILE_NAME;
                        File usernameFile = new File(usernameFilePath);
                        File passwordFile = new File(passwordFilePath);
                        
                        if (!usernameFile.exists()) {
                        	if (!passwordFile.exists()) {
                        		FileOutputStream usernameOutStream;
                        		FileOutputStream passwordOutStream;
                        
                        		try {
                        			usernameOutStream = openFileOutput(Constants.U_FILE_NAME, Context.MODE_PRIVATE);
                        			usernameOutStream.write(username.getBytes());
                        			usernameOutStream.close();
                        	
                        			passwordOutStream = openFileOutput(Constants.P_FILE_NAME, Context.MODE_PRIVATE);
                        			passwordOutStream.write(password.getBytes());
                        			passwordOutStream.close();
                        		} catch (Exception e) {
                        			String text = "Could not save login info";
                        			context.deleteFile(Constants.U_FILE_NAME);
                        			context.deleteFile(Constants.P_FILE_NAME);
                        	
                        			int duration = Toast.LENGTH_SHORT;
                    	    
                        			Toast toast = Toast.makeText(context, text, duration);
                        			toast.show();
                        			e.printStackTrace();
                        		}
                        	} else {
                        		// If only one file exists, makes it so neither exist for simplicity.
                        		passwordFile.delete();
                        	}
                        } else if (passwordFile.exists()) {
                        	// If only one file exists, makes it so neither exist for simplicity.
                        	passwordFile.delete();
                        }
                        */
                        
                        break;
                }
                tvMessage.setText(message);
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }

            if (errCode == 1) { //Opens a new page in response to a successful log in/add user.
                Intent intent = new Intent(getApplicationContext(), FrontPageActivity.class);
                intent.putExtra(Constants.MY_U_KEY, username);
                intent.putExtra(Constants.MY_P_KEY, password);
                startActivity(intent);
                finish();
            }
        }
    }

//    //Packages and sends the POST request to the database url, with information from user.
//    public static String POST(String url, User user, Context context){
//        InputStream inputStream = null;
//        String result = "";
//        try {
//            // create HttpClient
//            DefaultHttpClient httpclient = new DefaultHttpClient();
//            // make POST request to the given URL
//            HttpPost httpPost = new HttpPost(url);
//            String json = "";
//            // build jsonObject
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("username", user.getUsername());
//            jsonObject.put("password", user.getPassword());
//            //  convert JSONObject to JSON to String
//            json = jsonObject.toString();
//            // set json to StringEntity
//            StringEntity se = new StringEntity(json);
//            //se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
//            // set httpPost Entity
//            httpPost.setEntity(se);
//            // Set some headers to inform server about the type of the content
//            httpPost.setHeader("Accept", "application/json");
//            httpPost.setHeader("Content-type", "application/json");
//            // Execute POST request to the given URL
//            HttpResponse httpResponse = httpclient.execute(httpPost);
//            // receive response as inputStream
//            inputStream = httpResponse.getEntity().getContent();
//            // convert inputstream to string
//            if(inputStream != null)
//                result = convertInputStreamToString(inputStream);
//            else
//                result = "Did not work!";
//
//           //get cookie from response
//            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
//            SharedPreferences.Editor editor = pref.edit();
//            List<Cookie> cookies = httpclient.getCookieStore().getCookies();
//            for (int i = 0; i < cookies.size(); i++) {
//                Cookie cookie = cookies.get(i);
//                editor.putString(Constants.COOKIE, cookie.toString());
//            }
//            editor.apply();
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
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_login, container, false);
            return rootView;
        }
    }

}
