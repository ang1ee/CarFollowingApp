package cs169.carfollowingapp;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class FrontPageActivity extends Activity {
    
    final public static String U_KEY = "username";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_page);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.front_page, menu);
        return true;
    }
    
    public void broadcast(View view) {
        Intent intent = new Intent(getApplicationContext(), MapActivity.class);
        startActivity(intent);
    }
    
    public void follow(View view ) {
        EditText usernameEditText = (EditText) findViewById(R.id.input_username);
        String username = usernameEditText.getText().toString();
        
        Intent intent = new Intent(getApplicationContext(), FollowActivity.class);
        intent.putExtra(U_KEY, username);
        startActivity(intent);
    }
    

}
