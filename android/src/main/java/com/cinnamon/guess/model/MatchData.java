package com.cinnamon.guess.model;


import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class MatchData {
    private static final String TAG = "MatchData";

    public static final String GUESS_NUM_KEY = "guessNum";
    public static final String SELECT_NUM_KEY = "selectNum";
    public static final String TURN_COUNTER_KEY = "turnCounter";

    public static final int NO_SELECTION = -1;
    public static final int INVALID = 0;
    public int guessNum, selectNum, turnCounter;

    public MatchData() {
        guessNum = NO_SELECTION;
        selectNum = NO_SELECTION;
        turnCounter = 0;
    }

    public boolean isTurnToSelect() {
        return  guessNum == NO_SELECTION;
    }

    // This is the byte array we will write out to the TBMP API.
    public byte[] persist() {
        JSONObject retVal = new JSONObject();
        try {
            retVal.put(GUESS_NUM_KEY, guessNum);
            retVal.put(SELECT_NUM_KEY, selectNum);
            retVal.put(TURN_COUNTER_KEY, turnCounter);
        } catch (JSONException e) {
            Log.e(TAG, "persist error: " + e.getStackTrace());
        }
        String st = retVal.toString();
        Log.d(TAG, "==== PERSISTING\n" + st);
        return st.getBytes(Charset.forName("UTF-8"));
    }

    // Creates a new instance of SkeletonTurn.
    static public MatchData unpersist(byte[] byteArray) {
        if (byteArray == null) {
            Log.d(TAG, "Empty array---possible bug.");
            return new MatchData();
        }
        String st = null;
        try {
            st = new String(byteArray, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }
        Log.d(TAG, "====UNPERSISTING \n" + st);
        MatchData retVal = new MatchData();
        try {
            JSONObject obj = new JSONObject(st);
            if (obj.has(GUESS_NUM_KEY)) {
                retVal.guessNum = obj.getInt("guessNum");
            }
            if (obj.has(SELECT_NUM_KEY)) {
                retVal.selectNum = obj.getInt("selectNum");
            }
            if (obj.has(TURN_COUNTER_KEY)) {
                retVal.turnCounter = obj.getInt("turnCounter");
            }
        } catch (JSONException e) {
            Log.e(TAG, "persist error: " + e.getStackTrace());
        }
        return retVal;
    }
}
