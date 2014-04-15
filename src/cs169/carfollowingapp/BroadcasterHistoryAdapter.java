package cs169.carfollowingapp;

import java.util.List;




import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;



public class BroadcasterHistoryAdapter extends ArrayAdapter<String>{
	private List<String> broadcasterList;
	public BroadcasterHistoryAdapter(Context context, int resource, List<String> broadcasterList) {
        super(context, resource, broadcasterList);
        this.broadcasterList = broadcasterList;
    }
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        final String currentBroadcaster = broadcasterList.get(position);
        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            v = inflater.inflate(R.layout.item_broadcaster, null, false);
        }

        TextView textView = (TextView) v.findViewById(R.id.broadcaster_name);
        textView.setText(currentBroadcaster);

        return v;
    }

}
