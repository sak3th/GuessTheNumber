package com.cinnamon.guess.utils;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.cinnamon.guess.GuessApp;
import com.cinnamon.guess.R;
import com.cinnamon.guess.SharedPrefs;

import java.util.ArrayList;

public abstract class BaseActivity extends Activity implements
        GuessApp.ConnectivityListener,
        AsyncTaskListener {
    private static final String TAG = "GuessBaseActivity";

    protected static final int TOAST_LONG = Toast.LENGTH_LONG;
    protected static final int TOAST_SHORT = Toast.LENGTH_SHORT;
    private static final int SYSTEM_UI_FULLSCREEN_FLAGS =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    protected int mCurrentApiVersion;
    protected boolean mConnected;

    private View mDecorView;

    protected ArrayList<AsyncTask> mAsyncTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SharedPrefs.getDarkTheme(getApplicationContext())? R.style.AppThemeDark : R.style.AppThemeLight);
        super.onCreate(savedInstanceState);
        mCurrentApiVersion = Build.VERSION.SDK_INT;
        setupDecorView();
        mConnected = GuessApp.getInstance().isConnected();
        mAsyncTasks = new ArrayList<>();
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
    protected void onDestroy() {
        super.onDestroy();
        for (AsyncTask task : mAsyncTasks) {
            task.cancel(true);
        }
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
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onConnectivityChanged(boolean connected) {
        mConnected = connected;
        Log.d(TAG, "onConnectivityChanged: " + connected);
        // deal with connectivity changes
    }

    public void onTaskCreated(AsyncTask task) {
        mAsyncTasks.add(task);
    }

    public void onTaskDestroyed(AsyncTask task) {
        mAsyncTasks.remove(task);
    }

    protected void toast(String str) {
        Toast.makeText(getApplicationContext(), str, TOAST_SHORT).show();
    }
}
