package com.cinnamon.guess;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.cinnamon.guess.matchApi.MatchApi;
import com.cinnamon.guess.matchApi.model.Match;
import com.cinnamon.guess.registration.Registration;
import com.cinnamon.guess.utils.BaseActivity;
import com.cinnamon.guess.utils.NetworkTask;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.Plus;

import java.io.IOException;
import java.util.List;


public class MainActivity extends BaseActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MatchAdapter.OnMatchUpdateListener {
    private static final String TAG = "GuessMainActivity";

    public static final String ACTION_NEW_MATCH = "com.cinnamon.guess.ACTION_NEW_MATCH";
    public static final String ACTION_MATCH_UPDATE = "com.cinnamon.guess.ACTION_MATCH_UPDATE";


    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    private GoogleApiClient mGoogleApiClient;
    private boolean mIsInResolution;
    private GoogleCloudMessaging mGcm;

    private Context mContext;

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String matchId = intent.getStringExtra(GuessApp.KEY_MATCH_ID);
        Log.d(TAG, "onNewIntent: " + intent.getAction());
        Log.d(TAG, "onNewIntent: " + matchId);
        if (ACTION_NEW_MATCH.equals(intent.getAction())) {
            if (matchId !=  null) {
                getMatchAsync(Long.valueOf(matchId));
            }
        } else if (ACTION_MATCH_UPDATE.equals(intent.getAction())){
            if (matchId != null) {
                getMatchAsync(Long.valueOf(matchId));
            }
        } else {
            Log.d(TAG, "invalid intent");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initViews();
        mGoogleApiClient.connect();
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

        //setProfilePic();

        /* // TODO registerGcm
        mGcm = GoogleCloudMessaging.getInstance(mContext);
        String regId = SharedPrefs.getRegistrationId(mContext);
        Log.d(TAG, "onConnected " + regId);
        if (regId == null) {
            Log.d(TAG, "onConnected called registerGcmAsync" );
            registerGcmAsync(); //FIXME wait to see if gcm id is registered
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
            updateMatchAsync(match.getId(), -1);
        }
    }

    @Override
    public void onNumSelected(Match match, int num) {
        //updateMatchAsync(match.getId(), num);
        toast("Num " + num + " selected in match " + match.getId());
        updateMatchAsync(match.getId(), num);
    }

    @Override
    public void onAutoNewMatchClicked() {
        //toast("onAutoNewMatchClicked");
        createMatchAsync();
    }

    @Override
    public void onFriendNewMatchClicked() {

    }

    private void startApp() {
        if (mGoogleApiClient.isConnected()) {
            String regId = SharedPrefs.getRegistrationId(mContext);
            if (regId == null) {
                registerGcmAsync();
            } else {
                if (!SharedPrefs.getDeviceRegistered(mContext)) {
                    toast("Registering device");
                    registerDevice(regId);
                } else {
                    toast("Device is registered");
                    mAdapter.setDeviceRegistered(true);
                    getMatchesAsync();
                }
            }
        } else {
            mGoogleApiClient.connect();
        }
    }

    private void setSelectedAccountName(String accountName) {
        SharedPrefs.setPrefAccountName(mContext, accountName);
        GuessApp.getInstance().getCredential().setSelectedAccountName(accountName);
    }

    private void registerGcmAsync() {
        new NetworkTask<Void, String>(mProgressBarCenter, this) {
            @Override
            protected String doInBackground(Void... params) {
                String regid = "";
                try {
                    if (mGcm == null) {
                        mGcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }
                    regid = mGcm.register(getResources().getString(R.string.app_id));
                    Log.d(TAG, "GCM resgitration complete!");
                    //toast("GCM resgitration complete!");
                    SharedPrefs.storeRegistrationId(getApplicationContext(), regid);
                } catch (IOException ex) {
                    //toast("registerGcmAsync " + ex.getMessage());
                    Log.e(TAG, "registerGcmAsync " + ex.getMessage());
                    // TODO fix this
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                } finally {
                    return regid;
                }
            }
            @Override
            protected void onPostExecute(String regId) {
                registerDevice(regId);
            }
        }.execute();
    }
    private void registerDevice(String regid) {
        new NetworkTask<String, Boolean>(mProgressBarCenter, this) {
            @Override
            protected Boolean doInBackground(String[] params) {
                Log.d(TAG, "registerDevice: " + params[0]);
                boolean res = false;
                Registration mGcmRegApi = GuessApp.getInstance().getGcmRegistrationApi();
                if (params[0] == null) return false;
                try {
                    Log.d(TAG, "registering device with " + params[0]);
                    mGcmRegApi.register(params[0]).execute();
                    res = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "registerDevice " + e.getStackTrace());
                } finally {
                    return res;
                }

            }
            @Override
            protected void onPostExecute(Boolean res) {
                if (!isCancelled()) {
                    mAdapter.setDeviceRegistered(true);
                    Log.d(TAG, "registerDevice(post): " + res);
                    SharedPrefs.setDeviceRegistered(mContext, res);
                    if (res) {
                        getMatchesAsync();
                    }
                }
                super.onPostExecute(null);
            }
        }.execute(regid);
    }

    private void createMatchAsync() {
        new NetworkTask<Void, Match>(mProgressBar, this) {
            protected Match doInBackground(Void... params) {
                MatchApi matchApi = GuessApp.getInstance().getMatchApi();
                Match match = null;
                try {
                    match = matchApi.createMatch().execute();
                } catch (Exception e) {
                    Log.e(TAG, "createMatchAsync: " + e.getMessage());
                } finally {
                    return match;
                }
            }
            @Override
            protected void onPostExecute(Match match) {
                if (! isCancelled()) {
                    Log.d(TAG, "createMatchAsync - " + match);
                    if (match != null) {
                        mAdapter.addMatch(match);
                    } else {
                        toast("Matches not available now. Try again later.");
                    }
                }
                super.onPostExecute(null);
            }
        }.execute();
    }

    private void getMatchesAsync() {
        new NetworkTask<Void, List<Match>>(mProgressBar, this) {
            @Override
            protected List<Match> doInBackground(Void... params) {
                MatchApi matchApi = GuessApp.getInstance().getMatchApi();
                List<Match> matches = null;
                try {
                    matches = matchApi.getMatches().execute().getItems();
                } catch (IOException e) {
                    Log.e(TAG, "getMatchesAsync "  + e.getMessage());
                } finally {
                    return matches;
                }
            }
            @Override
            protected void onPostExecute(List<Match> matches) {
                if (!isCancelled()) {
                    int size = (matches == null) ? 0 : matches.size();
                    toast( size + " matches retrieved");
                    // FIXME change code to use change timestamps
                    if (matches != null) {
                        mAdapter.clear();
                        mAdapter.setMatches(matches);
                    }
                }
                super.onPostExecute(null);
            }
        }.execute();
    }

    private void getMatchAsync(Long id) {
        new NetworkTask<Long, Match>(mProgressBar, this) {
            @Override
            protected Match doInBackground(Long... params) {
                MatchApi matchApi = GuessApp.getInstance().getMatchApi();
                Match match = null;
                try {
                    match = matchApi.getMatch(params[0]).execute();
                } catch (Exception e) {
                    Log.e(TAG, "getMatchAsync: " + e.getMessage());
                } finally {
                    return match;
                }
            }
            @Override
            protected void onPostExecute(Match match) {
                if (!isCancelled()) {;
                    Log.d(TAG, "getMatchAsync - " + match);
                    // FIXME update based on timestamps
                    mAdapter.updateMatch(match);
                }
                super.onPostExecute(null);
            }
        }.execute(id);
    }

    private void updateMatchAsync(Long id, int guess) {
        new NetworkTask<Long, Match>(mProgressBar, this) {
            @Override
            protected Match doInBackground(Long... params) {
                MatchApi matchApi = GuessApp.getInstance().getMatchApi();
                Match match = null;
                try {
                    match = matchApi.addMove(params[0], params[1].intValue()).execute();
                } catch (Exception e) {
                    Log.e(TAG, "updateMatchAsync: " + e.getMessage());
                } finally {
                    return match;
                }
            }
            @Override
            protected void onPostExecute(Match match) {
                if (!isCancelled()) {;
                    Log.d(TAG, "updateMatchAsync - " + match);
                    if (match != null) {
                        mAdapter.updateMatch(match);
                    }
                }
                super.onPostExecute(null);
            }
        }.execute(id, Long.valueOf(guess));
    }

    /*private void setProfilePic() {
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
    }*/
}
