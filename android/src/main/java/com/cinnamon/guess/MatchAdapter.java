package com.cinnamon.guess;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cinnamon.guess.matchApi.model.Match;
import com.cinnamon.guess.model.MatchData;
import com.cinnamon.guess.utils.AsyncTaskListener;
import com.cinnamon.guess.utils.ImageLoader;
import com.cinnamon.guess.utils.MatchCardView;
import com.cinnamon.guess.utils.ParticipantDrawableResult;
import com.cinnamon.guess.utils.PlayerDrawableResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchEntity;

import java.util.ArrayList;
import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "Guess" + MatchAdapter.class.getSimpleName();

    private static final int NO_SELECTION = -1;

    private static final int ITEM_NEW_MATCH = 1;
    private static final int ITEM_MESSAGE = 2;
    private static final int ITEM_INVITATION = 3;
    private static final int ITEM_MATCH = 4;

    private GoogleApiClient mGoogleApiClient;
    private AsyncTaskListener mAsl;
    private MatchEventsListener mMatchEventsCallback;
    private ArrayList<TurnBasedMatchEntity> mMatches;
    private ArrayList<Invitation> mInvitations;
    private boolean mDeviceRegistered;
    private boolean mConnected;
    private String mMessage;
    private int mStaticItems = 0;

    public MatchAdapter(MainActivity activity) {
        mAsl = activity;
        mMatchEventsCallback = activity;
        mMatches = new ArrayList<>();
        mInvitations = new ArrayList<>();
        mDeviceRegistered = false;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "viewType: " + viewType);
        RecyclerView.ViewHolder vh = null;
        switch (viewType) {
            case ITEM_NEW_MATCH:
                vh = new NewMatchViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cardview_new_game, parent, false));
                break;
            case ITEM_MESSAGE:
                vh = new MessageViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cardview_message, parent, false), mMessage);
                break;
            case ITEM_INVITATION:
                vh = new InviteViewHolder((LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cardview_invitation, parent, false)));
                break;
            default:
                vh = new MatchViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cardview_match, parent, false));
                break;
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NewMatchViewHolder) {
            ((NewMatchViewHolder) holder).bind();
        } else if (holder instanceof MessageViewHolder) {
            ((MessageViewHolder) holder).bind(mMessage);
        } else if (holder instanceof InviteViewHolder) {
            int pos = position - getStaticItemsCount();
            ((InviteViewHolder) holder).bind(mInvitations.get(pos));
        }else if (holder instanceof MatchViewHolder) {
            int pos = position - (getStaticItemsCount() + mInvitations.size());
            ((MatchViewHolder) holder).bindMatch(mMatches.get(pos), pos);
        }
    }

    @Override
    public int getItemCount() {
        return mMatches.size() +  mInvitations.size() + getStaticItemsCount();
    }

    @Override
    public int getItemViewType(int position) {
        switch (getStaticItemsCount()) {
            case 1:
                if (position == 0) return mStaticItems;
                else return getItemInviteMatch(position-1);
            case 2:
                if (position == 0) return ITEM_NEW_MATCH;
                else if (position == 1) return ITEM_MESSAGE;
                else return getItemInviteMatch(position-2);
            default:
                return ITEM_MATCH;
        }
    }

    private int getItemInviteMatch(int position) {
        if (mInvitations.size() > position) {
            return ITEM_INVITATION;
        }
        return ITEM_MATCH;
    }

    public ArrayList<TurnBasedMatchEntity> getItems() {
        return mMatches;
    }

    public void setGoogleApiClient(GoogleApiClient gac) {
        mGoogleApiClient = gac;
        notifyDataSetChanged();
    }

    public void addInvitation(Invitation invite) {
        if (mInvitations.contains(invite)) return;
        mInvitations.add(invite);
        notifyDataSetChanged();
    }

    public void removeInvitation(Invitation invite) {
        mInvitations.remove(invite);
        notifyDataSetChanged();
    }

    public void removeInvitation(String id) {
        for (int i = 0; i < mInvitations.size(); i++) {
            if (mInvitations.get(i).getInvitationId() == id) {
                mInvitations.remove(i);
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void setMatches(ArrayList<TurnBasedMatchEntity> matches) {
        mMatches = matches;
        notifyDataSetChanged();
    }

    public void addMatch(TurnBasedMatchEntity match) {
        mMatches.add(match);
        notifyDataSetChanged();
    }

    public void clear() {
        mMatches.clear();
        notifyDataSetChanged();
    }

    public Match getMatch(Long id) {
        // FIXME
        return null;
    }

    public void updateMatch(TurnBasedMatchEntity match) {
        boolean exists = false;
        for (int i = 0; i < mMatches.size(); i++) {
            Log.d(TAG, "i value : " + i );
            if (tbmEquals(mMatches.get(i), match)) {
                Log.d(TAG, "match found " + mMatches.get(i) );
                exists = true;
                if (!tbmStateEquals(mMatches.get(i), match)) {
                    Log.d(TAG, "updating match at " + i );
                    mMatches.set(i, match);
                    notifyDataSetChanged();
                }
                break;
            }
        }
        if (!exists) {
            Log.d(TAG, "adding match: " + match );
            addMatch(match);
        }
    }

    public void removeMatch(TurnBasedMatchEntity match) {
        mMatches.remove(match);
        notifyDataSetChanged();
    }

    public void setDeviceRegistered(boolean registered) {
        if (mDeviceRegistered != registered) {
            mDeviceRegistered = registered;
            if (registered) addStaticItem(ITEM_NEW_MATCH);
            else removeStaticItem(ITEM_NEW_MATCH);
            notifyDataSetChanged();
        }
    }

    public void setConnected(boolean connected) {
        mConnected = connected;
        mConnected = true;
    }

    public void setMessage(String msg) {
        if (!msg.equals(mMessage)) {
            mMessage = msg;
            Log.d(TAG, "setting messge to " + msg);
            addStaticItem(ITEM_MESSAGE);
            notifyDataSetChanged();
        }
    }

    public void removeMessage() {
        if (mMessage != null) {
            mMessage = null;
            Log.d(TAG, "removing messge");
            removeStaticItem(ITEM_MESSAGE);
            notifyDataSetChanged();
        }
    }

    private void addStaticItem(int item) {
        mStaticItems += item;
    }

    private void removeStaticItem(int item) {
        mStaticItems -= item;
    }

    private int getStaticItemsCount() {
        switch (mStaticItems) {
            case ITEM_NEW_MATCH: return 1;
            case ITEM_MESSAGE: return 1;
            case (ITEM_NEW_MATCH+ITEM_MESSAGE): return 2;
            default: return 0;
        }
    }

    private Participant getNextParticipant(TurnBasedMatchEntity match) {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) return null;
        String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
        String myParticipantId = match.getParticipantId(playerId);
        ArrayList<Participant> participants = match.getParticipants();

        int desiredIndex = -1;
        for (int i = 0; i < participants.size(); i++) {
            if (participants.get(i).getParticipantId().equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }
        if (desiredIndex < participants.size()) {
            return participants.get(desiredIndex);
        }
        if (match.getAvailableAutoMatchSlots() <= 0) {
            // You've run out of automatch slots, so we start over.
            return participants.get(0);
        } else {
            // You have not yet fully automatched, so null will find a new
            // person to play against.
            return null;
        }
    }

    private class MessageViewHolder extends RecyclerView.ViewHolder {
        private String mMessage;
        private TextView mTextViewMessage;
        public MessageViewHolder(View view, String msg) {
            super(view);
            mTextViewMessage = (TextView) view.findViewById(R.id.textViewMessage);
            bind(msg);
        }
        public void bind(String msg) {
            mMessage = msg;
            mTextViewMessage.setText(msg);
        }
    }

    private class NewMatchViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mTextViewAutoMatch, mTextViewFriendMatch;
        public NewMatchViewHolder(View view) {
            super(view);
            mTextViewAutoMatch = (TextView) view.findViewById(R.id.textViewAutoMatch);
            mTextViewAutoMatch.setOnClickListener(this);
            bind();
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.textViewAutoMatch) {
                mMatchEventsCallback.onAutoNewMatchClicked();
            } else if (v.getId() == R.id.textViewFriendMatch) {
                Toast.makeText(v.getContext(), "Not supported yet", Toast.LENGTH_SHORT).show();
            }
        }

        public void bind() {
            mTextViewAutoMatch.setEnabled(mConnected);
        }
    }

    private class InviteViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Invitation mInvitation;
        private TextView mInviteMsg, mAccept, mDecline;
        public InviteViewHolder(View view) {
            super(view);
            mInviteMsg = (TextView) view.findViewById(R.id.textViewInviteMsg);
            mAccept = (TextView) view.findViewById(R.id.textViewAccept);
            mDecline = (TextView) view.findViewById(R.id.textViewDecline);
            mAccept.setOnClickListener(this);
            mDecline.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.textViewAccept) {
                mMatchEventsCallback.onInvitationAccepted(mInvitation);
            } else if (v.getId() == R.id.textViewDecline) {
                mMatchEventsCallback.onInvitationDeclined(mInvitation);
            }
        }

        public void bind(Invitation invitation) {
            mInvitation = invitation;
            mInviteMsg.setText("You have an invitation from " + invitation.getInviter().getDisplayName());
            mAccept.setEnabled(mConnected);
            mDecline.setEnabled(mConnected);
        }
    }

    public class MatchViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //private Match mMatch;
        private TurnBasedMatchEntity mMatch;
        private MatchCardView mMatchCardView;
        private ImageView mMyPic, mOppoPic;
        private Button mButton1, mButton2, mButton3, mButton4, mButton5;
        private Button[] mButtons;
        private TextView mMatchMsg, mDismiss, mMatchTitle, mNudge, mGuessNSelect;
        private View mMatchActionsLayout, mGameNumbersLayout, mGameActionsLayout;

        public MatchViewHolder(View v) {
            super(v);
            mMatchCardView = (MatchCardView) v.findViewById(R.id.cardviewMatch);
            mMyPic = (ImageView) v.findViewById(R.id.imageViewMyPic);
            mOppoPic = (ImageView) v.findViewById(R.id.imageViewOppPic);

            mMatchMsg = (TextView) v.findViewById(R.id.textViewMatchStatus);
            mMatchTitle = (TextView) v.findViewById(R.id.textViewTurnStatus);
            mMatchActionsLayout = v.findViewById(R.id.matchActionsLayout);
            mGameNumbersLayout = v.findViewById(R.id.gameNumsLayout);
            mGameActionsLayout = v.findViewById(R.id.gameActionsLayout);

            mButton1 = (Button) v.findViewById(R.id.button1);
            mButton2 = (Button) v.findViewById(R.id.button2);
            mButton3 = (Button) v.findViewById(R.id.button3);
            mButton4 = (Button) v.findViewById(R.id.button4);
            mButton5 = (Button) v.findViewById(R.id.button5);
            mButtons = new Button[] {mButton1, mButton2, mButton3, mButton4, mButton5};
            for (Button button : mButtons) {
                button.setOnClickListener(this);
            }

            mDismiss = (TextView) v.findViewById(R.id.textViewDismiss);
            mNudge = (TextView) v.findViewById(R.id.textViewNudge);
            mGuessNSelect = (TextView) v.findViewById(R.id.textViewGuessNSelect);
            mDismiss.setOnClickListener(this);
            mNudge.setOnClickListener(this);
            mGuessNSelect.setOnClickListener(this);
        }

        public void bindMatch(final TurnBasedMatchEntity match, int pos) {
            mMatch = match;
            // TODO load img from cache and if unavailable from network
            final Participant par = getNextParticipant(match);
            if (par != null) {
                ImageLoader.load(par,
                        new ImageLoader.ImageLoaderListener() {
                            @Override
                            public void onImageLoaded(Object res) {
                                Log.d(TAG, "onImageLoaded par:  " + res);
                                if (res != null && res instanceof ParticipantDrawableResult) {
                                    ParticipantDrawableResult pdr = (ParticipantDrawableResult) res;
                                    Drawable d = pdr.drawable;
                                    if (d != null && par.getParticipantId().equals(pdr.par.getParticipantId())) {
                                        mOppoPic.setImageDrawable(d);
                                    }
                                }
                            }
                        },
                        mAsl);
            }

            int size = MatchCardView.sDarkBgColors.length;
            if (pos >= size) {
                pos = pos % size;
            }
            int color = mMatchCardView.getResources().getColor(MatchCardView.sDarkBgColors[pos]);
            mMatchCardView.setCardBackgroundColor(color);

            int status = match.getStatus();
            int turnStatus = match.getTurnStatus();
            mGameActionsLayout.setVisibility(View.GONE);
            mGameNumbersLayout.setVisibility(View.GONE);
            mMatchMsg.setVisibility(View.VISIBLE);
            mMatchActionsLayout.setVisibility(View.VISIBLE);
            switch (status) {
                case TurnBasedMatch.MATCH_STATUS_CANCELED:
                    mMatchTitle.setText(R.string.match_title_cancelled);
                    mMatchMsg.setText(R.string.match_msg_cancelled);
                    return;
                case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                    mMatchTitle.setText(R.string.match_title_expired);
                    mMatchMsg.setText(R.string.match_msg_expired);
                    return;
                case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                    mMatchTitle.setText(R.string.match_title_auto_match);
                    mMatchMsg.setText(R.string.match_msg_auto_match);
                    return;
                case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                    mMatchTitle.setText(R.string.match_title_completed);
                    // FIXME show win loss status
                    if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                        mMatchMsg.setText(R.string.match_msg_won_game);
                    } else {
                        // Note that in this state, you must still call "Finish" yourself,
                        // so we allow this to continue.
                        mMatchMsg.setText(R.string.match_msg_lost_game);
                    }
                    return;
            }

            mGameActionsLayout.setVisibility(View.VISIBLE);
            mGameNumbersLayout.setVisibility(View.VISIBLE);
            mMatchMsg.setVisibility(View.GONE);
            mMatchActionsLayout.setVisibility(View.GONE);
            MatchData matchData = MatchData.unpersist(mMatch.getData());
            if (mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN) {
                mMatchTitle.setText(R.string.match_title_opp_turn);
                for (Button button : mButtons) {
                    button.setEnabled(false);
                    button.setSelected(false);
                }
                if (matchData.guessNum != MatchData.NO_SELECTION) {
                    mButtons[matchData.guessNum - 1].setSelected(true);
                }
                mNudge.setVisibility(View.VISIBLE);
                mNudge.setEnabled(mConnected);
                mGuessNSelect.setVisibility(View.GONE);
            } else if (mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                mMatchTitle.setText(R.string.match_title_your_turn);
                for (Button button : mButtons) {
                    button.setEnabled(true);
                    button.setSelected(false);
                }
                if (matchData.isTurnToSelect()) {
                    mGuessNSelect.setText(R.string.select);
                    if (matchData.selectNum == NO_SELECTION) {
                        mGuessNSelect.setEnabled(false);
                    } else {
                        mGuessNSelect.setEnabled(true);
                        mButtons[matchData.selectNum - 1].setSelected(true);
                    }
                } else {
                    mGuessNSelect.setText(R.string.guess);
                    if (matchData.selectNum == NO_SELECTION) {
                        mGuessNSelect.setEnabled(false);
                    } else {
                        mGuessNSelect.setEnabled(true);
                        mButtons[matchData.selectNum - 1].setSelected(true);
                    }
                }
                mNudge.setVisibility(View.GONE);
                mGuessNSelect.setVisibility(View.VISIBLE);
                if (!mConnected) {
                    mGuessNSelect.setEnabled(false);
                }
            } else if (mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                Log.e(TAG, "Should not reach here : turn status complete ");
            } else if (mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_INVITED) {
                Log.e(TAG, "Should not reach here : invited");
            }

        }

        @Override
        public void onClick(View v) {
            int selectedNum = -1;
            for (int i = 0; i < mButtons.length; i++) {
                if(mButtons[i].isSelected() == true) {
                    selectedNum = i+1;
                    break;
                }
            }
            MatchData matchData = MatchData.unpersist(mMatch.getData());
            if (v.getId() == R.id.textViewDismiss) {
                mMatchEventsCallback.onDismissed(mMatch);
            } else if (v.getId() == R.id.textViewGuessNSelect) {
                if (matchData.isTurnToSelect()) {
                    mMatchEventsCallback.onNumSelected(mMatch, selectedNum);
                    mGuessNSelect.setEnabled(false);
                } else {
                    // check if selected num is the right guessNum and update match
                    if (selectedNum != matchData.guessNum) {
                        mMatchMsg.setText(R.string.match_msg_wrong_guess);
                        mMatchMsg.setVisibility(View.VISIBLE);
                        mGameNumbersLayout.setVisibility(View.GONE);
                        updateWrongGuess();
                        mMatchEventsCallback.onNumGuessed(mMatch, false);
                    } else {
                        updateRightGuess();
                        mMatchMsg.setText(R.string.match_msg_right_gues);
                        mMatchMsg.setVisibility(View.VISIBLE);
                        mGameNumbersLayout.setVisibility(View.GONE);
                        mMatchEventsCallback.onNumGuessed(mMatch, true);
                    }
                }
            } else if (v.getId() == R.id.textViewNudge) {
                Toast.makeText(v.getContext(), "Nudge is not supported yet!", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, v.getId() + " button clicked " );
                for (int i = 0; i < 5; i++) {
                    if (v.getId() == mButtons[i].getId()) {
                        matchData.selectNum = i+1; // FIXME this is wrong
                        updateGuess(i);
                        mButtons[i].setSelected(true);
                        mGuessNSelect.setEnabled(true);
                    }
                }
            }
        }

        private void updateWrongGuess() {
            MatchData matchData = MatchData.unpersist(mMatch.getData());
            matchData.guessNum = MatchData.NO_SELECTION;
            matchData.selectNum = MatchData.NO_SELECTION;
            for (Button button : mButtons) {
                button.setSelected(false);
            }
            mGuessNSelect.setEnabled(false);
            mGuessNSelect.setText(R.string.select);
        }

        private void updateRightGuess() {
            mGuessNSelect.setEnabled(false);
            // TODO you can play an animation here
        }

        private void updateGuess(int num) {
            for (Button button : mButtons) {
                button.setSelected(false);
            }
            mButtons[num].setSelected(true);
        }

        private void updateSelectNum(int num) {

        }
    }

    private static boolean tbmEquals(TurnBasedMatch left, TurnBasedMatch right) {
        if (left == null || right == null) return false;
        return (left.getMatchId().equals(right.getMatchId()));
    }

    private static boolean tbmStateEquals(TurnBasedMatch left, TurnBasedMatch right) {
        if (left == null || right == null) return false;
        MatchData leftData = MatchData.unpersist(left.getData());
        MatchData rightData = MatchData.unpersist(right.getData());
        if (leftData.guessNum != rightData.guessNum) return false;
        if (leftData.selectNum != rightData.selectNum) return false;
        if (left.getStatus() != right.getStatus()) return false;
        return (left.getTurnStatus() == right.getTurnStatus());
    }

    public interface MatchEventsListener {
        public void onAutoNewMatchClicked();
        public void onFriendNewMatchClicked();
        public void onInvitationAccepted(Invitation invitation);
        public void onInvitationDeclined(Invitation invitation);
        public void onDismissed(TurnBasedMatchEntity match);
        public void onNumGuessed(TurnBasedMatchEntity match, boolean guess);
        public void onNumSelected(TurnBasedMatchEntity match, int num);
    }
}
