package com.cinnamon.guess.utils;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.cinnamon.guess.GuessApp;
import com.cinnamon.guess.R;
import com.cinnamon.guess.SharedPrefs;

public abstract class BaseActivity  extends Activity  implements GuessApp.ConnectivityListener {
    private static final String TAG = "GuessBaseActivity";

    protected static final int TOAST_LONG = Toast.LENGTH_LONG;
    protected static final int TOAST_SHORT = Toast.LENGTH_SHORT;
    private static final int SYSTEM_UI_FULLSCREEN_FLAGS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    protected int mCurrentApiVersion;
    protected boolean mConnected;

    private View mDecorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SharedPrefs.getDarkTheme(getApplicationContext())? R.style.AppThemeDark : R.style.AppThemeLight);
        super.onCreate(savedInstanceState);
        mCurrentApiVersion = Build.VERSION.SDK_INT;
        setupDecorView();
        mConnected = GuessApp.getInstance().isConnected();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GuessApp.getInstance().registerConnectivityListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GuessApp.getInstance().unregisterConnectivityListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(mCurrentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus) {
            mDecorView.setSystemUiVisibility(SYSTEM_UI_FULLSCREEN_FLAGS);
        }
    }

    private void setupDecorView() {
        mDecorView = getWindow().getDecorView();
        if(mCurrentApiVersion >= Build.VERSION_CODES.KITKAT) {
            mDecorView.setSystemUiVisibility(SYSTEM_UI_FULLSCREEN_FLAGS);
            mDecorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        mDecorView.setSystemUiVisibility(SYSTEM_UI_FULLSCREEN_FLAGS);
                    }
                }
            });
        }
    }

    protected void showSystemUI() {
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onConnectivityChanged(boolean connected) {
        mConnected = connected;
        Log.d(TAG, "onConnectivityChanged: " + connected);
        // deal with connectivity changes
    }

}