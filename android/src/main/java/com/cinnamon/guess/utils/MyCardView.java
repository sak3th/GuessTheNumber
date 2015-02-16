package com.cinnamon.guess.utils;


import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

import com.cinnamon.guess.R;
import com.cinnamon.guess.SharedPrefs;

import java.util.Random;

public class MyCardView extends CardView {

    private static int[] sDarkBgColors = new int[] {
            R.color.red_300,
            //R.color.pink_300,
            R.color.purple_300,
            //R.color.deep_purple_300,
            R.color.indigo_300,
            //R.color.blue_300,
            R.color.light_blue_300,
            //R.color.cyan_300,
            R.color.teal_300,
            //R.color.green_300,
            R.color.light_green_300,
            //R.color.lime_300,
            R.color.yellow_300,
            //R.color.amber_300,
            R.color.orange_300,
            //R.color.deep_orange_300,
            R.color.brown_300,
            //R.color.grey_300,
            R.color.blue_grey_300,
    };

    public MyCardView(Context context) {
        super(context);
    }

    public MyCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int id = SharedPrefs.getDarkTheme(getContext()) ?
                R.color.cardview_dark_background : R.color.cardview_light_background;
        setCardBackgroundColor(getResources().getColor(id));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        /*int id = SharedPrefs.getDarkTheme(getContext()) ?
                R.color.cardview_dark_background : R.color.cardview_light_background;
        setCardBackgroundColor(getResources().getColor(id));*/
        super.onDraw(canvas);
    }


}
