package com.cinnamon.guess;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.cinnamon.guess.matchApi.MatchApi;
import com.cinnamon.guess.playerApi.PlayerApi;
import com.cinnamon.guess.registration.Registration;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpTransport;

import java.io.IOException;
import java.util.ArrayList;

public class GuessApp extends Application {

    public static final boolean DEVELOPMENT = false;
    //public static final boolean DEVELOPMENT = true;

    public static final String PROJECT_ID = "cinn-guess-number";
    public static final String DEV_API_URL = "http://" + "10.0.1.10:8080" + "/_ah/api/";
    public static final String PROD_API_URL = "https://" + PROJECT_ID + ".appspot.com/_ah/api/";

    // should match WEB_CLIENT_ID in backend
    private static final String AUDIENCE = "server:client_id:" +
            "221520974985-urrr9685qitjvdj4c5i7je37t53eqa59.apps.googleusercontent.com";

    // should match com.cinn.guess.Constants in appengine
    public static final String KEY_MSG = "guess-msg";
    public static final String KEY_MATCH_ID = "guess-match-id";

    public static final String MSG_NEW_MATCH = "new match";
    public static final String MSG_MATCH_UPDATE = "match update";

    private Registration mRegistrationApi = null;
    private MatchApi mMatchApi = null;
    private PlayerApi mPlayerApi = null;

    private GoogleAccountCredential mCredential = null;
    HttpTransport mAndroidHttp = null;
    GoogleClientRequestInitializer mGoogleClientRequestInitializer = null;
    AndroidJsonFactory mAndroidJsonFactory = null;

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
        mAndroidHttp = AndroidHttp.newCompatibleTransport();
        mAndroidJsonFactory = new AndroidJsonFactory();
        mGoogleClientRequestInitializer = new GoogleClientRequestInitializer() {
            @Override
            public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                abstractGoogleClientRequest.setDisableGZipContent(true);
            }
        };
        mCredential = GoogleAccountCredential.usingAudience(getApplicationContext(), AUDIENCE);
    }

    public GoogleAccountCredential getCredential() {
        return mCredential;
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

    public Registration getGcmRegistrationApi() {
        if (mRegistrationApi == null) {
            if (DEVELOPMENT) {
                mRegistrationApi = new Registration.Builder(mAndroidHttp, mAndroidJsonFactory, mCredential)
                        .setRootUrl(DEV_API_URL)
                        .setGoogleClientRequestInitializer(mGoogleClientRequestInitializer)
                        .setApplicationName("Guess")
                        .build();
            } else {
                mRegistrationApi = new Registration.Builder(mAndroidHttp, mAndroidJsonFactory, mCredential)
                        .setRootUrl(PROD_API_URL)
                        .setApplicationName("Guess")
                        .build();
            }
        }
        return mRegistrationApi;
    }
    
    public MatchApi getMatchApi() {
        if(mMatchApi == null) {  // Only do this once
            if (DEVELOPMENT) {
                mMatchApi = new MatchApi.Builder(mAndroidHttp, mAndroidJsonFactory, mCredential)
                        .setRootUrl(DEV_API_URL)
                        .setGoogleClientRequestInitializer(mGoogleClientRequestInitializer)
                        .setApplicationName("Guess")
                        .build();
            } else {
                mMatchApi = new MatchApi.Builder(mAndroidHttp, mAndroidJsonFactory, mCredential)
                        .setRootUrl(PROD_API_URL)
                        .setApplicationName("Guess")
                        .build();
            }
        }
        return mMatchApi;
    }
}
