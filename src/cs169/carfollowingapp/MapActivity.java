package cs169.carfollowingapp;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends FragmentActivity {

    private GoogleMap map;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        
        this.map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        this.map.setMyLocationEnabled(true);
        
        LatLng location = this.getLocation();
        ArrayList<LatLng> coords = new ArrayList<LatLng>();
        coords.add(location);
        this.plot(coords);
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
    
    public LatLng getLocation() {
        String defaultUser = "";
        return getLocation(defaultUser);
    }
    
    // TODO: make sure non-authenticated users can't get anyone's location
    /* 
     * username: who to obtain the location point from
     * returns the obtained latitude/longitude in the form of LatLng, or null otherwise
     */
    public LatLng getLocation(String username) {
        return new LatLng(-33.867, 151.206);   
    }
    
}
