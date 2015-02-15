package com.cinnamon.guess;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cinnamon.guess.model.Match;
import com.cinnamon.guess.utils.MatchCardView;

import java.util.ArrayList;
import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "Geo" + MatchAdapter.class.getSimpleName();

    private static final int ITEM_NEW_MATCH = 1;
    private static final int ITEM_MESSAGE = 2;
    private static final int ITEM_MATCH = 3;

    private String mEmail;
    private List<Match> mMatches;
    private OnMatchUpdateListener mMatchUpdateCallback;
    private boolean mDeviceRegistered;
    private boolean mConnected;
    private String mMessage;
    private int mStaticItems = 0;

    public MatchAdapter(String email, OnMatchUpdateListener newMatchCallback) {
        mEmail = email;
        mMatches = new ArrayList<>();
        mMatchUpdateCallback = newMatchCallback;
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
        } else if (holder instanceof MatchViewHolder) {
            int pos = position - getStaticItemsCount();
            ((MatchViewHolder) holder).bindMatch(mMatches.get(pos), pos);
        }
    }

    @Override
    public int getItemCount() {
        int size = mMatches == null ? 0 : mMatches.size();
        return size + getStaticItemsCount();
    }

    @Override
    public int getItemViewType(int position) {
        switch (getStaticItemsCount()) {
            case 1:
                if (position == 0) return mStaticItems;
                else return ITEM_MATCH;
            case 2:
                if (position == 0) return ITEM_NEW_MATCH;
                else if (position == 1) return ITEM_MESSAGE;
                else return ITEM_MATCH;
            default:
                return ITEM_MATCH;
        }
    }

    public void setMatches(List<Match> matches) {
        mMatches = matches;
        notifyDataSetChanged();
    }

    public void addMatch(Match match) {
        mMatches.add(match);
        notifyDataSetChanged();
    }

    public Match getMatch(Long id) {
        // FIXME
        return null;
    }

    public void updateMatch() {
        // TODO
        notifyDataSetChanged();
    }

    public void removeMatch(Match match) {
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
                mMatchUpdateCallback.onAutoNewMatchClicked();
            } else if (v.getId() == R.id.textViewFriendMatch) {
                Toast.makeText(v.getContext(), "Not supported yet", Toast.LENGTH_SHORT).show();
            }
        }

        public void bind() {
            mTextViewAutoMatch.setEnabled(mConnected);
        }
    }

    public class MatchViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Match mMatch;
        private MatchCardView mMatchCardView;
        private ImageView mMyPic, mOppoPic;
        private Button mButton1, mButton2, mButton3, mButton4, mButton5;
        private Button[] mButtons;
        private TextView mTurnStatus, mNudge, mGuessNSelect;

        public MatchViewHolder(View v) {
            super(v);
            mMatchCardView = (MatchCardView) v.findViewById(R.id.cardviewMatch);
            mMyPic = (ImageView) v.findViewById(R.id.imageViewMyPic);
            mOppoPic = (ImageView) v.findViewById(R.id.imageViewOppPic);
            /*int imgId = SharedPrefs.getDarkTheme(v.getContext()) ?
                    R.drawable.ic_mood_black_48dp : R.drawable.ic_mood_white_48dp;
            mMyPic.setImageResource(imgId);
            mOppoPic.setImageResource(imgId);*/

            mButton1 = (Button) v.findViewById(R.id.button1);
            mButton2 = (Button) v.findViewById(R.id.button2);
            mButton3 = (Button) v.findViewById(R.id.button3);
            mButton4 = (Button) v.findViewById(R.id.button4);
            mButton5 = (Button) v.findViewById(R.id.button5);
            mButtons = new Button[] {mButton1, mButton2, mButton3, mButton4, mButton5};
            for (Button button : mButtons) {
                button.setOnClickListener(this);
            }

            mTurnStatus = (TextView) v.findViewById(R.id.textViewTurnStatus);
            mNudge = (TextView) v.findViewById(R.id.textViewNudge);
            mGuessNSelect = (TextView) v.findViewById(R.id.textViewGuessNSelect);
            mNudge.setOnClickListener(this);
            mGuessNSelect.setOnClickListener(this);
        }

        public void bindMatch(Match match, int pos) {
            mMatch = match;
            int size = MatchCardView.sDarkBgColors.length;
            if (pos >= size) {
                pos = pos % size;
            } else {
                ;
            }
            int color = mMatchCardView.getResources().getColor(MatchCardView.sDarkBgColors[pos]);
            mMatchCardView.setCardBackgroundColor(color);
            // TODO set match state
            if (!mMatch.getTurnOf().equals(mEmail)) {
                mTurnStatus.setText(R.string.opp_turn);
                for (Button button : mButtons) {
                    button.setEnabled(false);
                    button.setSelected(false);
                }
                if (mMatch.getGuess() != -1) {
                    mButtons[mMatch.getGuess()-1].setSelected(true);
                }
                mNudge.setVisibility(View.VISIBLE);
                mNudge.setEnabled(mConnected);
                mGuessNSelect.setVisibility(View.GONE);
            } else {
                mTurnStatus.setText(R.string.your_turn);
                for (Button button : mButtons) {
                    button.setEnabled(true);
                    button.setSelected(false);
                }
                if (mMatch.isTurnToSelect()) {
                    mGuessNSelect.setText(R.string.select);
                    if (mMatch.getNewGuess() == Match.NOT_SELECTED) {
                        mGuessNSelect.setEnabled(false);
                    } else {
                        mGuessNSelect.setEnabled(true);
                        mButtons[mMatch.getNewGuess() - 1].setSelected(true);
                    }
                } else {
                    mGuessNSelect.setText(R.string.guess);
                    if (mMatch.getNewGuess() == Match.NOT_SELECTED) {
                        mGuessNSelect.setEnabled(false);
                    } else {
                        mGuessNSelect.setEnabled(true);
                        mButtons[mMatch.getNewGuess() - 1].setSelected(true);
                    }
                }
                mNudge.setVisibility(View.GONE);
                mGuessNSelect.setVisibility(View.VISIBLE);
                if (!mConnected) {
                    mGuessNSelect.setEnabled(false);
                }
            }
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.textViewGuessNSelect) {
                // FIXME why getTurnToSelect instead of isTurnToSelect
                if (mMatch.isTurnToSelect()) {
                    mMatchUpdateCallback.onNumSelected(mMatch, mMatch.getNewGuess());
                    mGuessNSelect.setEnabled(false);
                } else {
                    // check if selected num is the right guess and update match
                    if (mMatch.getNewGuess() != mMatch.getGuess()) {
                        Toast.makeText(v.getContext(), "Wrong guess! \n Pick a number for opponent", Toast.LENGTH_LONG).show();
                        updateWrongGuess();
                        mMatchUpdateCallback.onNumGuessed(mMatch, false);
                    } else {
                        updateRightGuess();
                        mMatchUpdateCallback.onNumGuessed(mMatch, true);
                        Toast.makeText(v.getContext(), "Awesome guess!", Toast.LENGTH_LONG).show();
                    }
                }
            } else if (v.getId() == R.id.textViewNudge) {
                Toast.makeText(v.getContext(), "Nudge is not supported yet!", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, v.getId() + " rbutton clicked " );
                for (int i = 0; i < 5; i++) {
                    if (v.getId() == mButtons[i].getId()) {
                        mMatch.setNewGuess(i+1);
                        selectGuess(i);
                        mButtons[i].setSelected(true);
                        mGuessNSelect.setEnabled(true);
                    }
                }
            }
        }

        private void updateWrongGuess() {
            mMatch.setGuess(-1);
            mMatch.setNewGuess(-1);
            for (Button button : mButtons) {
                button.setSelected(false);
            }
            mGuessNSelect.setEnabled(false);
            mGuessNSelect.setText(R.string.select);
        }

        private void updateRightGuess() {
            //mMatch.
        }

        private void selectGuess(int num) {
            for (Button button : mButtons) {
                button.setSelected(false);
            }
            mButtons[num].setSelected(true);
        }
    }

    public interface OnMatchUpdateListener {
        public void onNumGuessed(Match match, boolean guess);
        public void onNumSelected(Match match, int num);
        public void onAutoNewMatchClicked();
        public void onFriendNewMatchClicked();
    }
}
