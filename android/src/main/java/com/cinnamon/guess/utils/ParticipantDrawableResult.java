package com.cinnamon.guess.utils;

import android.graphics.drawable.Drawable;

import com.google.android.gms.games.multiplayer.Participant;

public class ParticipantDrawableResult {
    public Drawable drawable;
    public Participant par;

    public ParticipantDrawableResult(Drawable drawable, Participant par) {
        this.drawable = drawable;
        this.par = par;
    }

    @Override
    public String toString() {
        return "ParticipantDrawableResult{" +
                "par=" + par +
                '}';
    }
}
