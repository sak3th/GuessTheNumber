package com.cinnamon.guess.utils;

import android.os.AsyncTask;
import android.view.View;

public abstract class NetworkTask<Params, Result> extends AsyncTask<Params, Void, Result> {
    // empty declaration
    private View mProgressBar;
    private AsyncTaskListener mListener;
    protected NetworkTask(View v, AsyncTaskListener listener) {
        mProgressBar = v;
        mListener = listener;
    }

    @Override
    protected void onPreExecute() {
        if (!isCancelled()) {
            mProgressBar.setVisibility(View.VISIBLE);
            mListener.onTaskCreated(this);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        if (!isCancelled()) {
            mProgressBar.setVisibility(View.GONE);
        }
        mListener.onTaskDestroyed(this);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mProgressBar.setVisibility(View.GONE);
        mListener.onTaskDestroyed(this);
    }

    @Override
    protected void onCancelled(Result result) {
        super.onCancelled(result);
        mProgressBar.setVisibility(View.GONE);
        mListener.onTaskDestroyed(this);
    }

    /*public interface NetworkTaskListener {
        public void onTaskCreated(NetworkTask task);
        public void onTaskDestroyed(NetworkTask task);
    }*/
}
