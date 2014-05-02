package cs169.carfollowingapp;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;





import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectBroadcasterActivity extends Activity {
	private String myUsername;
    private String myPassword;
    private String username;
    private String message;
    private TextView tvMessage;
    private List<String> broadcasterHistoryList;
    private ListView listView;
    private EditText broadcasterEditText;
    private EditText messageEditText;
        
    private String followRequestUrl = Constants.BASE_SERVER_URL + "api/follow_request";
    private String checkPermissionUrl = Constants.BASE_SERVER_URL + "api/check_permission";
    private String cancelUrl = Constants.BASE_SERVER_URL + "api/follow_cancellation";
    private String broadcasterHistoryUrl = Constants.BASE_SERVER_URL + "api/get_recently_followed";
    
    private Handler handler = new Handler();
    private int frequency = 5000;
    private AlertDialog.Builder builder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_broadcaster);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (!Singleton.getInstance().cookieCheck()) {
            Singleton.getInstance().clearCookies();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }

        Intent intent = getIntent();
        myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        myPassword = intent.getStringExtra(Constants.MY_P_KEY);
        tvMessage = (TextView) findViewById(R.id.tvMessage);
        broadcasterEditText = (EditText) findViewById(R.id.input_username);
        messageEditText = (EditText) findViewById(R.id.input_message);
        
        listView = (ListView) findViewById(R.id.broadcaster_history_list);
        broadcasterHistoryList = new ArrayList<String>();
        getBroadcasterHistory();
        listView.setAdapter(new BroadcasterHistoryAdapter(getApplicationContext(), 0, broadcasterHistoryList));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String broadcaster = (String) adapterView.getItemAtPosition(position);
                broadcasterEditText.setText(broadcaster);
                
            }
        });
    }
    

    @Override
    public void onBackPressed() {
        handler.removeCallbacksAndMessages(null);
        finish();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.select_broadcaster, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml
        String msg;
        AlertDialog.Builder builder;
        int id = item.getItemId();
        if (id == R.id.action_help) {
            builder = new AlertDialog.Builder(this);
            msg = "Step 1: Type in the username of the user you would like to follow\n\nStep 1.5: Alternatively, select a username from the list of recently followed users.\n\nStep 2: Hit Follow! When the broadcaster accepts your request, you will be automatically moved to the follow page.";
            builder.setMessage(msg)
                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    public void getBroadcasterHistory() {
    	//TODO: get this from somewhere.
    	new BroadcasterHistoryTask().execute(broadcasterHistoryUrl);

    }
    
    public void follow(View view) {
        EditText usernameEditText = (EditText) findViewById(R.id.input_username);
        
        username = usernameEditText.getText().toString();
        message = messageEditText.getText().toString();
        builder = new AlertDialog.Builder(this);
        new FollowRequestTask().execute(followRequestUrl);
        
        
    }
    
    private class BroadcasterHistoryTask extends AsyncTask<String, Void, String> {
    	private static final int SUCCESS = 1;
    	private static final int AUTHENTICATION_FAILED = -1;
    	
        @Override
        protected String doInBackground(String... urls) {

            JSONObject postData = new JSONObject();
            try {
                postData.put(Constants.MY_U_KEY, myUsername);//TODO:Cookie
                postData.put(Constants.MY_P_KEY, myPassword);
                JSONObject obj = Singleton.getInstance().makeHTTPPOSTRequest(urls[0], postData);;
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
                    showToast("JSON Error");
                } else if (result == "RUNTIME_EXCEPTION") {
                    showToast("Connection Error");
                } else if (result == "ERROR") {
                    showToast("Error");
                }
                fin = new JSONObject(result);
                int errCode = fin.getInt("status code");
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case SUCCESS:
                    	JSONArray jsonArray = fin.getJSONArray("history");
                    	if (jsonArray != null) { 
                    		   for (int i=0;i<jsonArray.length();i++){ 
                    		    broadcasterHistoryList.add(jsonArray.get(i).toString()); 
                    		}
                    	}
                        break;
                    case AUTHENTICATION_FAILED:
                        showToast("Authentication failed.");
                        tvMessage.setText("Authentication failed.");
                        break;
                    default:
                        showToast("Unknown errCode.");
                        tvMessage.setText("Unknown errCode.");
                        break;
                }

            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
                showToast(e.getMessage());
            }
        }
    }

    private class FollowRequestTask extends AsyncTask<String, Void, String> {
    	private static final int SUCCESS = 1;
    	private static final int AUTHENTICATION_FAILED = -1;
    	private static final int INVALID_MESSAGE = -2;
    	private static final int NO_SUCH_BROADCASTER_USERNAME = -3;
    	private static final int USER_NOT_BROADCASTING = -4;
    	
        @Override
        protected String doInBackground(String... urls) {

            JSONObject postData = new JSONObject();
            try {
                postData.put(Constants.MY_U_KEY, myUsername);
                postData.put(Constants.MY_P_KEY, myPassword);
                postData.put(Constants.U_KEY, username);
                postData.put("message", message);
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
            //Toast.makeText(getBaseContext(),result, Toast.LENGTH_LONG).show();
            JSONObject fin;
            try {
                if (result == "JSON_EXCEPTION") {
                    showToast("JSON Error");
                } else if (result == "RUNTIME_EXCEPTION") {
                    showToast("Connection Error");
                } else if (result == "ERROR") {
                    showToast("Error");
                }
                fin = new JSONObject(result);
                int errCode = fin.getInt("status code");
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case SUCCESS:
                    	builder.setMessage("Requesting to follow.")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            	new CancelTask().execute(cancelUrl);
                                dialog.dismiss();
                            }
                        });
                    	builder.show();
                    	
                    	new CheckPermissionTask().execute(checkPermissionUrl);
                        break;
                    case AUTHENTICATION_FAILED:
                    	showToast("Authentication failed.");
                    	tvMessage.setText("Authentication failed.");
                        break;
                    case INVALID_MESSAGE:
                    	showToast("Invalid message.");
                    	tvMessage.setText("Invalid message.");
                        break;
                    case NO_SUCH_BROADCASTER_USERNAME:
                        showToast("No such broadcasting user.");
                        tvMessage.setText("No such broadcasting user.");
                        break;
                    case USER_NOT_BROADCASTING:
                        showToast("User not broadcasting.");
                        tvMessage.setText("User not broadcasting.");
                        break;
                    default:
                        showToast("Unknown errCode.");
                        tvMessage.setText("Unknown errCode.");
                        break;
                }

            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
                showToast(e.getMessage());
            }
        }
    }
    
    private class CheckPermissionTask extends AsyncTask<String, Void, String> {
    	private static final int PERMITTED = 1;
    	private static final int NOT_PERMITTED = 2;
    	private static final int AUTHENTICATION_FAILED = -1;
    	private static final int USER_NOT_BROADCASTING = -3;
    	private static final int NO_SUCH_BROADCASTER_USERNAME = -4;
    	
        @Override
        protected String doInBackground(String... urls) {

            JSONObject postData = new JSONObject();
            try {
                postData.put(Constants.MY_U_KEY, myUsername);
                postData.put(Constants.MY_P_KEY, myPassword);
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
            //Toast.makeText(getBaseContext(),result, Toast.LENGTH_LONG).show();
            JSONObject fin;
            try {
                if (result == "JSON_EXCEPTION") {
                    showToast("JSON Error");
                } else if (result == "RUNTIME_EXCEPTION") {
                    showToast("Connection Error");
                } else if (result == "ERROR") {
                    showToast("Error");
                }
                fin = new JSONObject(result);
                int errCode = fin.getInt("status code");
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                	case PERMITTED:
                		handler.removeCallbacksAndMessages(null);
                		Intent intent = new Intent(getApplicationContext(), FollowActivity.class);
                        intent.putExtra(Constants.U_KEY, username);
                        intent.putExtra(Constants.MY_U_KEY, myUsername);
                        intent.putExtra(Constants.MY_P_KEY, myPassword);
                        startActivity(intent);
                        finish();
                		break;
                	case NOT_PERMITTED:
                    	handler.postDelayed(new Runnable() {
                            public void run() {
                                new CheckPermissionTask().execute(checkPermissionUrl);
                            }
                        }, frequency);
                        break;
                    case AUTHENTICATION_FAILED:
                        showToast("Incorrect user credential.");
                        break;
                    case USER_NOT_BROADCASTING:
                        showToast("User not broadcasting.");
                        break;
                    case NO_SUCH_BROADCASTER_USERNAME:
                        showToast("No such broadcasting user.");
                        break;
                    default:
                        showToast("Unknown errCode.");
                        break;
                }

            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
                showToast(e.getMessage());
            }
        }
    }
    private class CancelTask extends AsyncTask<String, Void, String> {
    	private static final int SUCCESS = 1;
    	private static final int AUTHENTICATION_FAILED = -1;
    	private static final int NO_SUCH_BROADCASTER_USERNAME = -3;
    	private static final int USER_NOT_BROADCASTING = -4;
    	
        @Override
        protected String doInBackground(String... urls) {

            JSONObject postData = new JSONObject();
            try {
                //TODO: Adjust JSON parameters to match spec
                postData.put(Constants.MY_U_KEY, myUsername);
                postData.put(Constants.MY_P_KEY, myPassword);
                postData.put("username", username);
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
            //Toast.makeText(getBaseContext(),result, Toast.LENGTH_LONG).show();
            JSONObject fin;
            try {
                if (result == "JSON_EXCEPTION") {
                    showToast("JSON Error");
                } else if (result == "RUNTIME_EXCEPTION") {
                    showToast("Connection Error");
                } else if (result == "ERROR") {
                    showToast("Error");
                }
                fin = new JSONObject(result);
                int errCode = fin.getInt("status code");
                switch (errCode) { //Updates the message on the Log In page, depending on the database response.
                    case SUCCESS:
                
                        handler.removeCallbacksAndMessages(null);

                        break;
                    case AUTHENTICATION_FAILED:
                        showToast("AUTHENTICATION_FAILED");
                        break;
                    case NO_SUCH_BROADCASTER_USERNAME:
                        showToast("No such broadcaster username.");
                        break;
                    case USER_NOT_BROADCASTING:
                    	showToast("User not broadcasting.");
                        break;
                    default:
                        showToast("Unknown errCode.");
                        break;
                }
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
                showToast(e.getMessage());
            }
        }
    }

    
    // Displays toast showing the text argument.
 	protected void showToast(CharSequence text) {
 	    Context context = getApplicationContext();
 	    int duration = Toast.LENGTH_SHORT;
 	    
 	    Toast toast = Toast.makeText(context, text, duration);
 	    toast.show();
 	}
}
