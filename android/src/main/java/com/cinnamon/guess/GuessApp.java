package com.cinnamon.guess;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.ArrayList;

public class GuessApp extends Application {

    private static GuessApp sSelf = null;
    public GuessApp() {
        sSelf = this;
    }
    public static GuessApp getInstance() {
        return sSelf;
    }
    @Override
    public void onCreate() {
        super.onCreate();

        mConnectivityListeners = new ArrayList<>();
        mConnMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    public void toggleTheme() {
        boolean dark = SharedPrefs.getDarkTheme(getApplicationContext());
        SharedPrefs.setDarkTheme(getApplicationContext(), !dark);
    }

    private ConnectivityManager mConnMgr;
    private ArrayList<ConnectivityListener> mConnectivityListeners;
    public interface ConnectivityListener {
        public void onConnectivityChanged(boolean connected);
    }
    public void registerConnectivityListener(ConnectivityListener listener) {
        if (listener != null) {
            mConnectivityListeners.add(listener);
            listener.onConnectivityChanged(isConnected());
            if (mConnectivityListeners.size() == 1) {
                registerReceiver(mConnectivityReceiver,
                        new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
            }
        }
    }
    public void unregisterConnectivityListener(ConnectivityListener listener) {
        if (listener != null) {
            mConnectivityListeners.remove(listener);
            if (mConnectivityListeners.size() == 0) {
                unregisterReceiver(mConnectivityReceiver);
            }
        }
    }
    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mConnectivityListeners.size() > 0) {
                boolean connected = isConnected();
                for (ConnectivityListener listener : mConnectivityListeners) {
                    listener.onConnectivityChanged(connected);
                }
            }
        }
    };
    public boolean isConnected() {
        NetworkInfo info = mConnMgr.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            return true;
        }
        return false;
    }
}
