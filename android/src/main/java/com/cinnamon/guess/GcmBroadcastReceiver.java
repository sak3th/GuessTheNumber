package com.cinnamon.guess;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "GuessGcmRcvr";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, intent.getAction() + "\n Payload: " + intent.getStringExtra(GuessApp.KEY_MSG));
        String msg = intent.getStringExtra(GuessApp.KEY_MSG);
        String matchId = intent.getStringExtra(GuessApp.KEY_MATCH_ID);
        Intent matchIntent = new Intent();
        matchIntent.setClass(context, MainActivity.class);
        matchIntent.putExtra(GuessApp.KEY_MATCH_ID, matchId);
        matchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        matchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (msg != null) {
            if (msg.equals(GuessApp.MSG_NEW_MATCH)) {
                if (matchId != null) {
                    matchIntent.setAction(MainActivity.ACTION_NEW_MATCH);
                    context.startActivity(matchIntent);
                    Log.d(TAG, "sending new match intent for " + matchId);
                }
            } else if (msg.equals(GuessApp.MSG_MATCH_UPDATE)) {
                if (matchId != null) {
                    matchIntent.setAction(MainActivity.ACTION_MATCH_UPDATE);
                    context.startActivity(matchIntent);
                    Log.d(TAG, "sending update match intent for " + matchId);
                }
            }
        }
    }


}
