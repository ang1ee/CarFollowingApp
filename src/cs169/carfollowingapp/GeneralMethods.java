package cs169.carfollowingapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Steven on 4/13/14.
 */
public class GeneralMethods {
    static public boolean cookieCheck(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (pref.contains(Constants.COOKIE)) {
            return true;
        }
        return false;
    }

    static public void clearCookies(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.commit();
    }
}
