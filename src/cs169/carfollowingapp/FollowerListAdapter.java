package cs169.carfollowingapp;

import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FollowerListAdapter extends ArrayAdapter<Map.Entry<String, Float>> {
    
    Map.Entry<String, Float>[] followerColorTuples;
    
    public FollowerListAdapter(Context context, int textViewResourceId,
            Entry<String, Float>[] objects) {
        super(context, textViewResourceId, objects);
        followerColorTuples = objects;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        Map.Entry<String, Float> currentTuple = followerColorTuples[position];
        String follower = currentTuple.getKey();
        float bitmapColor = currentTuple.getValue().floatValue();
        int color = Color.HSVToColor(new float[] { bitmapColor, 1.0f, 1.0f });
        
        
        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            v = inflater.inflate(R.layout.item_follower, null, false);
        }

        TextView textView = (TextView) v.findViewById(R.id.follower_name);
        textView.setText(follower);
        textView.setTextColor(color);
        
        Log.d("FOLLOWERLISTADAPTER", follower + " " + color);

        return v;
    }
    
    
    
}
