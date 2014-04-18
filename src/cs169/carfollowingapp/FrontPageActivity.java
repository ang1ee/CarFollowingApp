
package cs169.carfollowingapp;


import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class FrontPageActivity extends Activity {
    

    
    private String myUsername;
    private String myPassword;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Singleton.getInstance().cookieCheck()) {
            Singleton.getInstance().clearCookies();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        Context context = getApplicationContext();
        CharSequence text = "Cookie: " + Singleton.getInstance().getCookie();
        int duration = Toast.LENGTH_SHORT;
        Toast.makeText(context, text, duration).show();


        setContentView(R.layout.activity_front_page);
        Intent intent = getIntent();
        myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        myPassword = intent.getStringExtra(Constants.MY_P_KEY);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.front_page, menu);
        return true;
    }
    
    public void broadcast(View view) {
        // Get username and password from Login.
        Intent intent = new Intent(getApplicationContext(), BroadcastActivity.class);
        intent.putExtra(Constants.MY_U_KEY, myUsername);
        intent.putExtra(Constants.MY_P_KEY, myPassword);
        startActivity(intent);
        finish();
    }
    
    public void follow(View view) {
    	Intent intent = new Intent(getApplicationContext(), SelectBroadcasterActivity.class);
        intent.putExtra(Constants.MY_U_KEY, myUsername);
        intent.putExtra(Constants.MY_P_KEY, myPassword);
        startActivity(intent);
        finish();
        

    }

    @Override
    public void onBackPressed() {
    }

    public void logout(View view) {
        Singleton.getInstance().clearCookies();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

}

