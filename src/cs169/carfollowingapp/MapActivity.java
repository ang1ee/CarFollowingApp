package cs169.carfollowingapp;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;



public class MapActivity extends FragmentActivity {

    protected GoogleMap map;
    
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    public void plot(ArrayList<LatLng> coords) {
        if (coords.size() < 1) {
            return;
        }
        
        // I don't know what 13 encodes yet... i'll make a class variable eventually
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(coords.get(0), 13));
        
        for (int i = 0; i < coords.size(); i++) {
            map.addMarker(new MarkerOptions().position(coords.get(i)));
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
