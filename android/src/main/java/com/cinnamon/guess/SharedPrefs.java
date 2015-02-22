package com.cinnamon.guess;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchEntity;

import java.util.ArrayList;
import java.util.Set;

public class SharedPrefs {

    private static final String SHARED_PREFS_FILE = SharedPrefs.class.getSimpleName();
    private static final String TAG = GuessApp.class.getSimpleName() + SHARED_PREFS_FILE;
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
    }


    private static final String PROPERTY_REG_ID = "key_reg_id";
    static String getRegistrationId(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, null);
        if (registrationId == null || registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return null;
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        // FIXME add below code
        /*int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return null;
        }*/
        return registrationId;
    }
    static void storeRegistrationId(Context context, String regId) {
        final SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PROPERTY_REG_ID, regId).commit();
    }


    private static final String PREF_ACCOUNT_NAME = "key_pref_acc_name";
    static String getPrefAccountName(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getString(PREF_ACCOUNT_NAME, null);
    }
    static void setPrefAccountName(Context context, String accountName) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREF_ACCOUNT_NAME, accountName).commit();
    }


    private static final String PROPERTY_APP_VERSION = "key_app_version";
    static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }


    private static final String PREF_DEVICE_REGISTERED = "key_pref_device_registered";
    static boolean getDeviceRegistered(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getBoolean(PREF_DEVICE_REGISTERED, false);
    }
    static void setDeviceRegistered(Context context, boolean registered) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(PREF_DEVICE_REGISTERED, registered).commit();
    }


    private static final String PREF_THEME = "key_pref_theme";
    public static boolean getDarkTheme(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getBoolean(PREF_THEME, true);
    }
    public static void setDarkTheme(Context context, boolean dark) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(PREF_THEME, dark).commit();
    }


    private static final String PREF_IMAGES = "key_images";
    public static Set<String> getImages(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getStringSet(PREF_IMAGES, null);
    }
    public static void setImages(Context context, Set<String> images) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putStringSet(PREF_IMAGES, images).commit();
    }
}
