package com.cinnamon.guess;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.cinnamon.guess.model.MatchData;
import com.cinnamon.guess.registration.Registration;
import com.cinnamon.guess.utils.BaseActivity;
import com.cinnamon.guess.utils.ImageLoader;
import com.cinnamon.guess.utils.NetworkTask;

import com.cinnamon.guess.utils.PlayerDrawableResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationBuffer;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchBuffer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchEntity;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.Plus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;


public class MainActivity extends BaseActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnInvitationReceivedListener,
        OnTurnBasedMatchUpdateReceivedListener,
        MatchAdapter.MatchEventsListener {
    private static final String TAG = "GuessMainActivity";

    public static final String ACTION_NEW_MATCH = "com.cinnamon.guess.ACTION_NEW_MATCH";
    public static final String ACTION_MATCH_UPDATE = "com.cinnamon.guess.ACTION_MATCH_UPDATE";


    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    private final static int RC_SELECT_PLAYERS = 10000;
    private final static int RC_LOOK_AT_MATCHES = 10001;

    private GoogleApiClient mGoogleApiClient;
    private boolean mIsInResolution;
    private GoogleCloudMessaging mGcm;

    private Context mContext;

    private AlertDialog mAlertDialog;

    //private ProgressBar mProgressBar;
    private ProgressBar mProgressBarCenter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private MatchAdapter mAdapter;

    private ImageView mProfilePic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        setContentView(R.layout.activity_main);
        initViews();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_PROFILE)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
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
        setupViews();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<TurnBasedMatchEntity> matches = savedInstanceState.getParcelableArrayList("matches");
        for (TurnBasedMatchEntity tbme : matches) {
            updateMatch(tbme);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Connection to Play Services needs to be disconnected as soon as an activity is invisible.
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPrefs.setImages(mContext, ImageLoader.getImages());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("matches", mAdapter.getItems());
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
            /*case RC_SIGN_IN:
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (resultCode == Activity.RESULT_OK) {
                    mGoogleApiClient.connect();
                } else {
                    // FIXME
                    toast("fixme: RC_SIGN_IN ");
                    //BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_other_error);
                }
                break;*/
            case RC_LOOK_AT_MATCHES:
                // Returning from the 'Select Match' dialog
                if (resultCode != Activity.RESULT_OK) {
                    // user canceled
                    return;
                }
                TurnBasedMatch match = data.getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
                if (match != null) {
                    updateMatch(new TurnBasedMatchEntity(match));
                }
                Log.d(TAG, "Match = " + match);
                break;
            case RC_SELECT_PLAYERS:
                // Returned from 'Select players to Invite' dialog
                if (resultCode != Activity.RESULT_OK) {
                    // user canceled
                    return;
                }

                // get the invitee list
                final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
                Bundle autoMatchCriteria = null;
                int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
                int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

                if (minAutoMatchPlayers > 0) {
                    autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                            minAutoMatchPlayers, maxAutoMatchPlayers, 0);
                } else {
                    autoMatchCriteria = null;
                }

                TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                        .addInvitedPlayers(invitees)
                        .setAutoMatchCriteria(autoMatchCriteria).build();

                // Start the match
                Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(
                        new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                            @Override
                            public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                                processResult(result);
                            }
                        });
                showSpinner();
                break;
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");

        final Player p = Games.Players.getCurrentPlayer(mGoogleApiClient);
        ImageLoader.load(p,
                new ImageLoader.ImageLoaderListener() {
                    @Override
                    public void onImageLoaded(Object res) {
                        if (res != null && res instanceof PlayerDrawableResult) {
                            PlayerDrawableResult pdr = (PlayerDrawableResult)res;
                            Drawable d = pdr.drawable;
                            if (d != null && p.getPlayerId().equals(pdr.player.getPlayerId())) {
                                mProfilePic.setImageDrawable(d);
                            }
                        }
                    }
                },
                this);

        mAdapter.setGoogleApiClient(mGoogleApiClient);
        mAdapter.setDeviceRegistered(true);
        getMatchesAsync();

        // Retrieve the TurnBasedMatch from the connectionHint
        if (connectionHint != null) {
            TurnBasedMatch tbm = connectionHint.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);
            if (tbm != null) {
                if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
                    Log.d(TAG, "Warning: accessing TurnBasedMatch when not connected");
                }
                updateMatch(new TurnBasedMatchEntity(tbm));
                return;
            }
        }

        // As a demonstration, we are registering this activity as a handler for
        // invitation and match events.

        // This is *NOT* required; if you do not register a handler for
        // invitation events, you will get standard notifications instead.
        // Standard notifications may be preferable behavior in many cases.
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        // Likewise, we are registering the optional MatchUpdateListener, which
        // will replace notifications you would get otherwise. You do *NOT* have
        // to register a MatchUpdateListener.
        Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, this);
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
            // FIXME start anything in gps based code?
            //startApp();
        } else {
            mAdapter.setMessage("Not connected to Internet.");
        }
    }


    @Override
    public void onAutoNewMatchClicked() {
        //toast("onAutoNewMatchClicked");
        Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    @Override
    public void onFriendNewMatchClicked() {

    }

    @Override
    public void onDismissed(TurnBasedMatchEntity match) {
        Log.d(TAG, "onDismissed: " + match.getMatchId());
        mAdapter.removeMatch(match);
        Games.TurnBasedMultiplayer.dismissMatch(mGoogleApiClient, match.getMatchId());
    }

    @Override
    public void onInvitationAccepted(final Invitation invitation) {
        Log.d(TAG, "onInvitationAccepted: " + invitation.getInvitationId());
        ResultCallback<TurnBasedMultiplayer.InitiateMatchResult> cb = new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
            @Override
            public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                processResult(invitation, result);
            }
        };
        Games.TurnBasedMultiplayer.acceptInvitation(mGoogleApiClient, invitation.getInvitationId()).setResultCallback(cb);
    }

    @Override
    public void onInvitationDeclined(Invitation invitation) {
        Games.TurnBasedMultiplayer.declineInvitation(mGoogleApiClient, invitation.getInvitationId());
    }

    // Displays your inbox. You will get back onActivityResult where
    // you will need to figure out what you clicked on.
    public void onCheckGamesClicked() {
        Intent intent = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient);
        startActivityForResult(intent, RC_LOOK_AT_MATCHES);
    }

    // Open the create-game UI. You will get back an onActivityResult
    // and figure out what to do.
    public void onStartMatchClicked() {
        Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient,
                1, 7, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    // Create a one-on-one automatch game.
    public void onQuickMatchClicked() {
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, 0);
        TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                .setAutoMatchCriteria(autoMatchCriteria).build();

        showSpinner();

        // Start the match
        ResultCallback<TurnBasedMultiplayer.InitiateMatchResult> cb = new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
            @Override
            public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                processResult(result);
            }
        };
        Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(cb);
    }

    /* ---------------- IN GAME CONTROLS START ---------------- */
    // Cancel the game. Should possibly wait until the game is canceled before
    // giving up on the view.
    public void onCancelClicked(TurnBasedMatch match) {
        showSpinner();
        Games.TurnBasedMultiplayer.cancelMatch(mGoogleApiClient, match.getMatchId())
                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.CancelMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.CancelMatchResult result) {
                        processResult(result);
                    }
                });
        setupViews();
    }

    // Leave the game during your turn. Note that there is a separate
    // Games.TurnBasedMultiplayer.leaveMatch() if you want to leave NOT on your turn.
    public void onLeaveClicked(TurnBasedMatch match) {
        showSpinner();
        String nextParticipantId = getNextParticipantId(match);

        Games.TurnBasedMultiplayer.leaveMatchDuringTurn(mGoogleApiClient, match.getMatchId(),
                nextParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.LeaveMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.LeaveMatchResult result) {
                        processResult(result);
                    }
                });
        setupViews();
    }

    // Finish the game. Sometimes, this is your only choice.
    public void onFinishClicked(TurnBasedMatch match) {
        showSpinner();
        Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, match.getMatchId())
                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });
        setupViews();
    }

    @Override
    public void onNumGuessed(TurnBasedMatchEntity match, boolean guess) {
        showSpinner();
        String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
        String myParticipantId = match.getParticipantId(playerId);
        if (guess) {
            ArrayList<ParticipantResult> results = new ArrayList<>();
            results.add(new ParticipantResult(myParticipantId,
                    ParticipantResult.MATCH_RESULT_WIN, ParticipantResult.PLACING_UNINITIALIZED));
            results.add(new ParticipantResult(getNextParticipantId(match),
                    ParticipantResult.MATCH_RESULT_LOSS, ParticipantResult.PLACING_UNINITIALIZED));
            MatchData matchData = MatchData.unpersist(match.getData());
            matchData.guessNum = matchData.selectNum = MatchData.INVALID;
            Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, match.getMatchId(),
                    matchData.persist(), results)
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                            processResult(result);
                        }
                    });
        } else {
            MatchData matchData = MatchData.unpersist(match.getData());
            matchData.guessNum = MatchData.NO_SELECTION;
            matchData.selectNum = MatchData.NO_SELECTION;

            Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(),
                    matchData.persist(), myParticipantId).setResultCallback(
                    new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                            processResult(result);
                        }
                    });
            matchData = null;
        }
    }

    @Override
    public void onNumSelected(TurnBasedMatchEntity match, int num) {
        //Log.d(TAG, "Num " + num + " selected in match " + match.getMatchId());
        showSpinner();
        String nextParticipantId = getNextParticipantId(match);
        MatchData matchData = MatchData.unpersist(match.getData());
        // Create the next turn
        matchData.guessNum = num;
        matchData.selectNum = MatchData.NO_SELECTION;
        matchData.turnCounter += 1;
        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(),
                matchData.persist(), nextParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });
        matchData = null;
    }
    /* ---------------- IN GAME CONTROLS END ---------------- */

    // Handle notification events.
    @Override
    public void onInvitationReceived(Invitation invitation) {
        toast("An invitation has arrived from " + invitation.getInviter().getDisplayName());
        mAdapter.addInvitation(invitation);
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        toast("An invitation was removed.");
        mAdapter.removeInvitation(invitationId);
    }

    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch match) {
        toast("A match was updated.");
        updateMatch(new TurnBasedMatchEntity(match));
    }

    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
        toast("A match was removed.");
    }

    private void initViews() {
        mProfilePic = (ImageView) findViewById(R.id.imageViewProfilePic);
        mProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GuessApp.getInstance().toggleTheme();
                recreate();
            }
        });
        //mProgressBar = (ProgressBar) findViewById(R.id.progressSpinnerTop);
        mProgressBarCenter = (ProgressBar) findViewById(R.id.progressSpinnerCenter);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        //mSwipeRefreshLayout.setOverScrollMode(View.OVER_SCROLL_NEVER);
        if (SharedPrefs.getDarkTheme(mContext)) {
            mSwipeRefreshLayout.setProgressBackgroundColor(R.color.refresh_progress);
            mSwipeRefreshLayout.setColorSchemeResources(R.color.refresh_progress_accent);
        } else {
            mSwipeRefreshLayout.setProgressBackgroundColor(R.color.refresh_progress_light);
            mSwipeRefreshLayout.setColorSchemeResources(R.color.refresh_progress_light_accent);
        }

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getMatchesAsync();
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerviewMatches);
        LinearLayoutManager linearLayoutMgr = new LinearLayoutManager(mContext);
        linearLayoutMgr.setReverseLayout(true);
        mRecyclerView.setLayoutManager(linearLayoutMgr);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new MatchAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void setupViews() {
        // FIXME set views based on app and device state
        //mProgressBar.setVisibility(View.GONE);
        mProgressBarCenter.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }


    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting() && mConnected) {
            mGoogleApiClient.connect();
        }
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

    /**
     * Get the next participant. In this function, we assume that we are
     * round-robin, with all known players going before all automatch players.
     * This is not a requirement; players can go in any order. However, you can
     * take turns in any order.
     *
     * @return participantId of next player, or null if automatching
     */
    public String getNextParticipantId(TurnBasedMatch match) {
        String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
        String myParticipantId = match.getParticipantId(playerId);
        ArrayList<String> participantIds = match.getParticipantIds();

        int desiredIndex = -1;
        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }
        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }
        if (match.getAvailableAutoMatchSlots() <= 0) {
            // You've run out of automatch slots, so we start over.
            return participantIds.get(0);
        } else {
            // You have not yet fully automatched, so null will find a new
            // person to play against.
            return null;
        }
    }

    // startMatch() happens in response to the createTurnBasedMatch() above.
    // This is only called on success, so we should have a valid match object.
    // We're taking this opportunity to setup the game, saving our initial state.
    // Calling takeTurn() will callback to OnTurnBasedMatchUpdated(),
    // which will show the game UI.
    public void startMatch(TurnBasedMatch match) {
        showSpinner();
        MatchData matchData = new MatchData();
        String participantId = null;
        String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
        participantId = (new Random().nextInt(2) == 0) ?
                match.getParticipantId(playerId) : getNextParticipantId(match);

        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(),
                matchData.persist(), participantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });
    }

    private void updateMatch(TurnBasedMatchEntity match) {
        /*int status = match.getStatus();
        int turnStatus = match.getTurnStatus();
        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                showWarning("Canceled!", "This game was canceled!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                showWarning("Expired!", "This game is expired.  So sad!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                showWarning("Waiting for auto-match...",
                        "We're still waiting for an automatch partner.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                    showWarning( "Complete!", "This game is over; " +
                            "someone finished it, and so did you!  There is nothing to be done.");
                    break;
                }

                // Note that in this state, you must still call "Finish" yourself,
                // so we allow this to continue.
                showWarning("Complete!", "This game is over; " +
                        "someone finished it!  You can only finish it now.");
        }*/

        // FIXME take care of MATCH_TURN_STATUS_INVITED
        // OK, it's active. Check on turn status.
        mAdapter.updateMatch(match);
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
        Intent intent =
                Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }
    public void onStartMatchClicked(View view) {
        Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient,
                1, 7, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    // Create a one-on-one automatch game.
    public void onQuickMatchClicked(View view) {
        showSpinner();
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, 0);
        TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                .setAutoMatchCriteria(autoMatchCriteria).build();
        // Start the match
        ResultCallback<TurnBasedMultiplayer.InitiateMatchResult> cb = new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
            @Override
            public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                processResult(result);
            }
        };
        Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(cb);
    }

    private void getMatchesAsync() {
        Log.d(TAG, "getMatchesAsync");
        showSpinner();
        int[] matchStates = new int[] {
                TurnBasedMatch.MATCH_TURN_STATUS_INVITED,
                TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN,
                TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN,
                TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE};
        ResultCallback<TurnBasedMultiplayer.LoadMatchesResult> cb = new ResultCallback<TurnBasedMultiplayer.LoadMatchesResult>() {
            @Override
            public void onResult(TurnBasedMultiplayer.LoadMatchesResult result) {
                processResult(result);
            }
        };
        Games.TurnBasedMultiplayer.loadMatchesByStatus(mGoogleApiClient, matchStates)
                .setResultCallback(cb);
        mSwipeRefreshLayout.setRefreshing(true);
    }

    private void getMatchAsync(Long id) {
        /*new NetworkTask<Long, Match>(mProgressBar, this) {
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
        }.execute(id);*/
    }

    private void showSpinner() {
        mSwipeRefreshLayout.setRefreshing(true);
    }

    private void dismissSpinner() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void processResult(TurnBasedMultiplayer.LoadMatchesResult result) {
        dismissSpinner();
        mSwipeRefreshLayout.setRefreshing(false);
        Log.d(TAG, "getMatchesAsync result");
        if (!checkStatusCode(null, result.getStatus().getStatusCode())) {
            Log.d(TAG, "getMatchesAsync result returning: ");
            return;
        }
        if (result.getMatches().hasData()) {
            InvitationBuffer ib = result.getMatches().getInvitations();
            for (int i = 0; i < ib.getCount(); i++ ) {
                mAdapter.addInvitation(ib.get(i));
            }
            //ib.close();
            TurnBasedMatchBuffer mytbmb = result.getMatches().getMyTurnMatches();
            for (int i = 0; i < mytbmb.getCount(); i++ ) {
                updateMatch(new TurnBasedMatchEntity(mytbmb.get(i)));
            }
            mytbmb.close();
            TurnBasedMatchBuffer opptbmb = result.getMatches().getTheirTurnMatches();
            for (int i = 0; i < opptbmb.getCount(); i++ ) {
                updateMatch(new TurnBasedMatchEntity(opptbmb.get(i)));
            }
            opptbmb.close();
            TurnBasedMatchBuffer comtbmb = result.getMatches().getCompletedMatches();
            for (int i = 0; i < comtbmb.getCount(); i++ ) {
                updateMatch(new TurnBasedMatchEntity(comtbmb.get(i)));
            }
            comtbmb.close();
        }
    }

    private void processResult(Invitation invitation, TurnBasedMultiplayer.InitiateMatchResult result) {
        dismissSpinner();
        TurnBasedMatch match = result.getMatch();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }
        mAdapter.removeInvitation(invitation);
        if (match.getData() != null) {
            // This is a game that has already started, so I'll just start
            updateMatch(new  TurnBasedMatchEntity(match));
            return;
        }
        startMatch(match);
    }

    private void processResult(TurnBasedMultiplayer.InitiateMatchResult result) {
        dismissSpinner();
        TurnBasedMatch match = result.getMatch();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }
        if (match.getData() != null) {
            // This is a game that has already started, so I'll just start
            updateMatch(new TurnBasedMatchEntity(match));
            return;
        }
        startMatch(match);
    }

    public void processResult(TurnBasedMultiplayer.UpdateMatchResult result) {
        Log.d(TAG, "UpdateMatchResult: " + result);
        showSpinner();
        TurnBasedMatch match = result.getMatch();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            Log.d(TAG, "UpdateMatchResult: error returning");
            return;
        }
        // FIXME
        /*if (match.canRematch()) {
            askForRematch();
        }*/
        updateMatch(new TurnBasedMatchEntity(match));
    }

    private void processResult(TurnBasedMultiplayer.CancelMatchResult result) {
        /*dismissSpinner();
        if (!checkStatusCode(null, result.getStatus().getStatusCode())) {
            return;
        }
        showWarning("Match", "This match is canceled.  All other players will have their game ended.");*/
    }

    private void processResult(TurnBasedMultiplayer.LeaveMatchResult result) {
        /*TurnBasedMatch match = result.getMatch();
        dismissSpinner();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }
        isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
        showWarning("Left", "You've left this match.");*/
    }

    // Returns false if something went wrong, probably. This should handle
    // more cases, and probably report more accurate results.
    private boolean checkStatusCode(TurnBasedMatch match, int statusCode) {
        switch (statusCode) {
            case GamesStatusCodes.STATUS_OK:
                return true;
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
                // This is OK; the action is stored by Google Play Services and will
                // be dealt with later.
                toast("Stored action for later.  (Please remove this toast before release.)");
                // NOTE: This toast is for informative reasons only; please remove
                // it from your final application.
                return true;
            case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                showErrorMessage(match, statusCode, R.string.status_multiplayer_error_not_trusted_tester);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
                showErrorMessage(match, statusCode, R.string.match_error_already_rematched);
                break;
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED:
                showErrorMessage(match, statusCode, R.string.network_error_operation_failed);
                break;
            case GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED:
                showErrorMessage(match, statusCode, R.string.client_reconnect_required);
                break;
            case GamesStatusCodes.STATUS_INTERNAL_ERROR:
                showErrorMessage(match, statusCode, R.string.internal_error);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH:
                showErrorMessage(match, statusCode, R.string.match_error_inactive_match);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
                showErrorMessage(match, statusCode, R.string.match_error_locally_modified);
                break;
            default:
                showErrorMessage(match, statusCode, R.string.unexpected_status);
                Log.d(TAG, "Did not have warning or string to deal with: "
                        + statusCode);
        }

        return false;
    }

    public void showErrorMessage(TurnBasedMatch match, int statusCode, int stringId) {
        showWarning("Warning", getResources().getString(stringId));
    }

    // Generic warning/info dialog
    public void showWarning(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title).setMessage(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                    }
                });

        // create alert dialog
        mAlertDialog = alertDialogBuilder.create();

        // show it
        mAlertDialog.show();
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
