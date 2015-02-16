package com.cinnamon.guess.utils;


import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

import com.cinnamon.guess.R;
import com.cinnamon.guess.SharedPrefs;

import java.util.Random;

public class MatchCardView extends CardView {

    public static int[] sDarkBgColors = new int[] {
            R.color.red_400,
            R.color.pink_400,
            //R.color.purple_400, // bad
            R.color.deep_purple_400,
            R.color.indigo_400,
            //R.color.blue_400, // bad
            R.color.light_blue_500,
            R.color.cyan_500,
            R.color.teal_400,
            R.color.green_400,
            //R.color.light_green_400, // bad
            //R.color.lime_500,
            //R.color.yellow_500, // bad
            R.color.amber_400,
            R.color.orange_400,
            R.color.deep_orange_400,
            //R.color.brown_400,
            //R.color.grey_400, // bad
            //R.color.blue_grey_400,
    };

    public MatchCardView(Context context) {
        super(context);
    }

    public MatchCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MatchCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        /*int i = new Random().nextInt(sDarkBgColors.length);
        int id = SharedPrefs.getDarkTheme(getContext()) ?
                sDarkBgColors[i] : R.color.cardview_light_background;
        setCardBackgroundColor(getResources().getColor(id));*/
    }

    @Override
    protected void onDraw(Canvas canvas) {
        /*int i = new Random().nextInt(sDarkBgColors.length);
        int id = SharedPrefs.getDarkTheme(getContext()) ?
                sDarkBgColors[i] : R.color.cardview_light_background;
        setCardBackgroundColor(getResources().getColor(id));*/
        super.onDraw(canvas);
    }
}
