package com.cinnamon.guess.utils;

import android.os.AsyncTask;

public interface AsyncTaskListener {
    public void onTaskCreated(AsyncTask task);
    public void onTaskDestroyed(AsyncTask task);
}
