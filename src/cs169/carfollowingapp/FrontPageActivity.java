
package cs169.carfollowingapp;

import java.io.File;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
        
        setContentView(R.layout.activity_front_page);
        Intent intent = getIntent();
        myUsername = intent.getStringExtra(Constants.MY_U_KEY);
        myPassword = intent.getStringExtra(Constants.MY_P_KEY);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.front_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        String msg;
        AlertDialog.Builder builder;
        switch (item.getItemId()) {
            case R.id.action_info:
                builder = new AlertDialog.Builder(this);
                msg = "Caravan is an application made to streamline the experience of driving as a group!  One user can broadcast his location and have other his friends follow him.\n\nUsers can track each others locations and navigate toward the main broadcaster!";
                builder.setMessage(msg)
                        .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
                return true;
            case R.id.action_help:
                builder = new AlertDialog.Builder(this);
                msg = "Select Broadcast if you wish the broadcast your location.\n\nSelect Follow if you wish to follow another broadcasting user.";
                builder.setMessage(msg)
                        .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void broadcast(View view) {
        // Get username and password from Login.
        Intent intent = new Intent(getApplicationContext(), BroadcastActivity.class);
        intent.putExtra(Constants.MY_U_KEY, myUsername);
        intent.putExtra(Constants.MY_P_KEY, myPassword);
        startActivity(intent);
    }
    
    public void follow(View view) {
    	Intent intent = new Intent(getApplicationContext(), SelectBroadcasterActivity.class);
        intent.putExtra(Constants.MY_U_KEY, myUsername);
        intent.putExtra(Constants.MY_P_KEY, myPassword);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
    }

    public void logout(View view) {
        Singleton.getInstance().clearCookies();
        
        // Delete files storing username and password
        
        Context context = getApplicationContext();
        /*
        boolean deletedUsernameFile = context.deleteFile(context.getFilesDir().getAbsolutePath() + "/" + Constants.U_FILE_NAME);
        boolean deletedPasswordFile = context.deleteFile(context.getFilesDir().getAbsolutePath() + "/" + Constants.P_FILE_NAME);
        */
        
        String usernameFilePath = context.getFilesDir().getAbsolutePath() + "/" + Constants.U_FILE_NAME;
        String passwordFilePath= context.getFilesDir().getAbsolutePath() + "/" + Constants.P_FILE_NAME;
        File usernameFile = new File(usernameFilePath);
        File passwordFile = new File(passwordFilePath);
        
        boolean deletedUsernameFile = usernameFile.delete();
        boolean deletedPasswordFile = passwordFile.delete();
        
        // For debugging
        if (!deletedUsernameFile) {
        	showToast("Could not delete username file");
        }
        
        if (!deletedPasswordFile) {
        	showToast("Could not delete password file");
        }
        
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
 // Displays toast showing the text argument.
 	protected void showToast(CharSequence text) {
 	    Context context = getApplicationContext();
 	    int duration = Toast.LENGTH_SHORT;
 	    
 	    Toast toast = Toast.makeText(context, text, duration);
 	    toast.show();
 	}

}

