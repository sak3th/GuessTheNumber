package com.cinnamon.guess.utils;

import android.graphics.drawable.Drawable;

import com.google.android.gms.games.Player;

public class PlayerDrawableResult {
    public Drawable drawable;
    public Player player;

    public PlayerDrawableResult(Drawable drawable, Player p) {
        this.drawable = drawable;
        this.player = p;
    }
}
