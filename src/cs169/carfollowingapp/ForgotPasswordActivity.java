package cs169.carfollowingapp;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class ForgotPasswordActivity extends Activity {
	
    TextView tvMessage;
	EditText etUsername;
    Button btnForgotPassword;
    String username;
    
    //Destination addresses for the reseting the password
    String resetPasswordUrl = Constants.BASE_SERVER_URL + "api/reset_password";

  //the errCode and count returned in the JSON response by the database.
    int errCode;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        /*
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
        */
        
        etUsername = (EditText) findViewById(R.id.etUsernameForgotPW);
        btnForgotPassword = (Button) findViewById(R.id.btnForgotPassword);
        
     // add click listener to Button "Register". Sends a POST request to users/register
        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = etUsername.getText().toString();
                Toast.makeText(getBaseContext(), "Sending email...", Toast.LENGTH_LONG).show();
                new ResetPasswordTask().execute(resetPasswordUrl);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.forgot_password, menu);
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
    /*
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_forgot_password,
                    container, false);
            return rootView;
        }
    }
    */
    
    //Responsible for sending out the Post request on a different thread.
    //Takes the Username information from the text field
    //to send it out to the server for resetting the password.
    private class ResetPasswordTask extends AsyncTask<String, Void, String> {
    	
    	static final int NO_SUCH_USERNAME = -1;
    	
        @Override
        protected String doInBackground(String... urls) {
            JSONObject postData = new JSONObject();
            try {
                postData.put(Constants.U_KEY, username);
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
            JSONObject fin;
            try {
                fin = new JSONObject(result);
                errCode = fin.getInt("status code");
                String message;
                switch (errCode) { //Updates the message on the ForgotPassword page, depending on the database response.
                    case NO_SUCH_USERNAME:
                        message = "User does not exist.";
                        break;
                    default:
                        message = "Reset password request successful.";
                        break;
                }
                tvMessage.setText(message);
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }

            /*
            if (errCode == 1) { //Opens a new page in response to a successful password reset.
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
            */
        }
    }

}
