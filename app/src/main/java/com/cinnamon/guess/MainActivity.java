package com.cinnamon.guess;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cinnamon.guess.model.Match;
import com.cinnamon.guess.utils.BaseActivity;
import com.cinnamon.guess.utils.NetworkTask;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import java.io.IOException;
import java.net.URL;
import java.util.Random;


public class MainActivity extends BaseActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MatchAdapter.OnMatchUpdateListener {
    private static final String TAG = "GuessMainActivity";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private boolean mIsInResolution;

    private ProgressBar mProgressBar;
    private ProgressBar mProgressBarCenter;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private MatchAdapter mAdapter;

    private ImageView mProfilePic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        setContentView(R.layout.activity_main);
        setupViews();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addApi(Games.API)
                .addScope(Games.SCOPE_GAMES)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void setProfilePic() {
        new NetworkTask<Void, Drawable>() {
            @Override
            protected Drawable doInBackground(Void... params) {
                Person p = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
                if (p != null) {
                    if (p.getImage().hasUrl()) {
                        String imgUrl = p.getImage().getUrl();
                        Log.d(TAG, "url: " + imgUrl.replace("50", "200"));
                        imgUrl = imgUrl.replace("50", "200");
                        try {
                            final RoundedBitmapDrawable d =
                                    RoundedBitmapDrawableFactory.create(getResources(), new URL(imgUrl).openConnection().getInputStream());
                            d.setAntiAlias(true);
                            d.setCornerRadius(Math.max(d.getMinimumWidth(), d.getMinimumHeight()) / 2.0f);
                            return  d;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    } else {
                        toast("does not have url");
                    }
                } else {
                    toast("person is null");
                }
                return null;
            }
            @Override
            protected void onPostExecute(Drawable drawable) {
                //mProfilePic.setImageDrawable(drawable);
            }
        }.execute();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initViews();
        mGoogleApiClient.connect();
        if (!mConnected) {
            //mAdapter.setMessage("Not connected to Internet.");
        }
    }

    @Override
    protected void onStop() {
        // Connection to Play Services needs to be disconnected as soon as an activity is invisible.
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                retryConnecting();
                break;
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting() && mConnected) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        final String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
        setSelectedAccountName(accountName);

        startApp();

        setProfilePic();

        /* // TODO registerGcm
        mGcm = GoogleCloudMessaging.getInstance(mContext);
        String regId = SharedPrefs.getRegistrationId(mContext);
        Log.d(TAG, "onConnected " + regId);
        if (regId == null) {
            Log.d(TAG, "onConnected called registerGcmidAsync" );
            registerGcmidAsync(); //FIXME wait to see if gcm id is registered
        }*/

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    @Override
    public void onConnectivityChanged(boolean connected) {
        super.onConnectivityChanged(connected);
        mAdapter.setConnected(connected);
        if (connected) {
            mAdapter.removeMessage();
            startApp();
        } else {
            mAdapter.setMessage("Not connected to Internet.");
        }
    }

    private void setupViews() {
        mProfilePic = (ImageView) findViewById(R.id.imageViewProfilePic);
        mProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GuessApp.getInstance().toggleTheme();
                recreate();
            }
        });
        mProgressBar = (ProgressBar) findViewById(R.id.progressSpinnerTop);
        mProgressBarCenter = (ProgressBar) findViewById(R.id.progressSpinnerCenter);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerviewMatches);
        LinearLayoutManager linearLayoutMgr = new LinearLayoutManager(mContext);
        linearLayoutMgr.setReverseLayout(true);
        mRecyclerView.setLayoutManager(linearLayoutMgr);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new MatchAdapter(SharedPrefs.getPrefAccountName(mContext), this);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void initViews() {
        mProgressBar.setVisibility(View.GONE);
        mProgressBarCenter.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNumGuessed(Match match, boolean guess) {
        if (guess) {
            // TODO complete match
        } else {
            // FIXME correct below code
            //updateMatchAsync(match.getId(), -1);
        }
    }

    @Override
    public void onNumSelected(Match match, int num) {
        //updateMatchAsync(match.getId(), num);
        toast("Num " + num + " selected in match " + match.getId() );
    }

    @Override
    public void onAutoNewMatchClicked() {
        //toast("onAutoNewMatchClicked");
        startNewMatchAsync();
    }

    @Override
    public void onFriendNewMatchClicked() {

    }

    private void startApp() {
        if (mGoogleApiClient.isConnected()) {
            if (!SharedPrefs.getDeviceRegistered(mContext)) {
                toast("Registering device");
                registerDevice();
            } else {
                toast("Device is registered");
                mAdapter.setDeviceRegistered(true);
            }
        } else {
            mGoogleApiClient.connect();
        }
    }

    private void setSelectedAccountName(String accountName) {
        SharedPrefs.setPrefAccountName(mContext, accountName);
    }

    private void registerDevice() {
        new NetworkTask<String, String>() {
            @Override
            protected void onPreExecute() {
                mProgressBarCenter.setVisibility(View.VISIBLE);
            }
            @Override
            protected String doInBackground(String[] params) {
                Log.d(TAG, "registerDevice: " + params[0]);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "saketh";
                //return null;
            }
            @Override
            protected void onPostExecute(String s) {
                mProgressBarCenter.setVisibility(View.GONE);
                mAdapter.setDeviceRegistered(true);
                Log.d(TAG, "registerDevice(post): " + s);
                SharedPrefs.setDeviceRegistered(mContext, true);
            }
        }.execute("saketh");
    }

    private void startNewMatchAsync() {
        new NetworkTask<String, String>() {
            @Override
            protected void onPreExecute() {
                mProgressBar.setVisibility(View.VISIBLE);
            }
            @Override
            protected String doInBackground(String[] params) {
                Log.d(TAG, "registerDevice: " + params[0]);
                try {
                    //Thread.sleep(3000);
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "saketh";
                //return null;
            }
            @Override
            protected void onPostExecute(String s) {
                mProgressBar.setVisibility(View.GONE);
                //Log.d(TAG, "matches not available ");
                Match match = new Match(new Random().nextInt(2));
                mAdapter.addMatch(match);
            }
        }.execute("saketh");
    }

    private void toast(String str) {
        Toast.makeText(mContext, str, TOAST_LONG).show();
    }
}
