package com.cinnamon.guess.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;

import com.cinnamon.guess.GuessApp;
import com.cinnamon.guess.SharedPrefs;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.Participant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class ImageLoader {

    private static ImageLoader sSelf;

    private Context mContext;
    private Set<String> mImageSet;

    private ImageLoader() {
        mContext = GuessApp.getInstance().getApplicationContext();
        sSelf = this;
        mImageSet = SharedPrefs.getImages(mContext);
        if (mImageSet == null) {
            //Log.d("GIL", "loading image set from prefs");
            mImageSet = new HashSet<>();
        }
    }

    private static ImageLoader self() {
        if (sSelf == null) {
            sSelf = new ImageLoader();
        }
        return sSelf;
    }

    public static Set<String> getImages() {
        //Log.d("GIL", "storing image set in prefs");
        return self().getImageSet();
    }

    private Set<String> getImageSet() {
        return mImageSet;
    }

    public static void load(Participant par, ImageLoaderListener iml, AsyncTaskListener asl) {
        self().loadImage(par, iml, asl);
    }

    public static void load(Player p, ImageLoaderListener iml, AsyncTaskListener asl) {
        self().loadImage(p, iml, asl);
    }

    private void loadImage(Participant par, ImageLoaderListener iml, AsyncTaskListener asl) {
        Player p = par.getPlayer();
        Log.d("GIL", "loadImage: " + par + "\n" + p);
        if (p == null || p.getPlayerId() == null || !p.hasIconImage()) {
            //Log.d("GIL", "player does not have image");
            Log.d("GIL", "loadImage:  returning");
            iml.onImageLoaded(null);
            return;
        }
        String id = p.getPlayerId();
        if (mImageSet.contains(id)) {
            //Log.d("GIL", "player has image in cache");
            String path = mContext.getFilesDir().toString() + "/" + id + ".drbl";
            iml.onImageLoaded(new ParticipantDrawableResult(
                    readDrawable(mContext, new File(path).toString()), par));
        } else {
            //Log.d("GIL", "player does not have image in cache");
            self().new ParticipantImageLoaderTask(iml, asl).execute(par);
        }
    }

    private void loadImage(Player p, ImageLoaderListener iml, AsyncTaskListener asl) {
        if (p == null || p.getPlayerId() == null || !p.hasIconImage()) {
            //Log.d("GIL", "player does not have image");
            iml.onImageLoaded(null);
            return;
        }
        String id = p.getPlayerId();
        if (mImageSet.contains(id)) {
            //Log.d("GIL", "player has image in cache");
            String path = mContext.getFilesDir().toString() + "/" + id + ".drbl";
            iml.onImageLoaded(new PlayerDrawableResult(
                    readDrawable(mContext, new File(path).toString()),
                    p));
        } else {
            //Log.d("GIL", "player does not have image in cache");
            self().new PlayerImageLoaderTask(iml, asl).execute(p);
        }
    }

    private static void writeBitmap(String path, Bitmap bm) throws IOException {
        File file = new File(path);
        FileOutputStream outStream = new FileOutputStream(file);
        bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        outStream.flush();
        outStream.close();
    }

    private static Drawable readDrawable(Context ctxt, String path) {
        RoundedBitmapDrawable d = RoundedBitmapDrawableFactory.create(ctxt.getResources(), path);
        d.setAntiAlias(true);
        d.setCornerRadius(Math.max(d.getMinimumWidth(), d.getMinimumHeight()) / 2.0f);
        return d;
    }

    private class ParticipantImageLoaderTask extends AsyncTask<Participant, Void, ParticipantDrawableResult> {
        ImageLoaderListener mIml;
        AsyncTaskListener mAsl;
        public ParticipantImageLoaderTask(ImageLoaderListener iml, AsyncTaskListener asl) {
            mIml = iml;
            mAsl = asl;
        }
        @Override
        protected void onPreExecute() {
            if (!isCancelled()) {
                mAsl.onTaskCreated(this);
            }
        }
        @Override
        protected ParticipantDrawableResult doInBackground(Participant... params) {
            RoundedBitmapDrawable d = null;
            Player p = params[0].getPlayer();
            String url = params[0].getIconImageUrl();
            try {
                d = RoundedBitmapDrawableFactory.create(mContext.getResources(),
                        new URL(url).openConnection().getInputStream());
                d.setAntiAlias(true);
                d.setCornerRadius(Math.max(d.getMinimumWidth(), d.getMinimumHeight()) / 2.0f);
                String path = mContext.getFilesDir().toString() + "/" + p.getPlayerId() + ".drbl";
                writeBitmap(path, d.getBitmap());
                mImageSet.add(p.getPlayerId());
                //Log.d("GIL", "storing player in imageset");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                return new ParticipantDrawableResult(d, params[0]);
            }
        }
        @Override
        protected void onPostExecute(ParticipantDrawableResult pdr) {
            super.onPostExecute(pdr);
            mIml.onImageLoaded(pdr);
            mAsl.onTaskDestroyed(this);
        }
    }

    private class PlayerImageLoaderTask extends AsyncTask<Player, Void, PlayerDrawableResult> {
        ImageLoaderListener mIml;
        AsyncTaskListener mAsl;
        public PlayerImageLoaderTask(ImageLoaderListener iml, AsyncTaskListener asl) {
            mIml = iml;
            mAsl = asl;
        }
        @Override
        protected void onPreExecute() {
            if (!isCancelled()) {
                mAsl.onTaskCreated(this);
            }
        }
        @Override
        protected PlayerDrawableResult doInBackground(Player... params) {
            RoundedBitmapDrawable d = null;
            Player p = params[0];
            String url = params[0].getIconImageUrl();
            try {
                d = RoundedBitmapDrawableFactory.create(mContext.getResources(),
                        new URL(url).openConnection().getInputStream());
                d.setAntiAlias(true);
                d.setCornerRadius(Math.max(d.getMinimumWidth(), d.getMinimumHeight()) / 2.0f);
                String path = mContext.getFilesDir().toString() + "/" + p.getPlayerId() + ".drbl";
                writeBitmap(path, d.getBitmap());
                mImageSet.add(p.getPlayerId());
                //Log.d("GIL", "storing player in imageset");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                return new PlayerDrawableResult(d, p);
            }
        }
        @Override
        protected void onPostExecute(PlayerDrawableResult pdr) {
            super.onPostExecute(pdr);
            mIml.onImageLoaded(pdr);
            mAsl.onTaskDestroyed(this);
        }
    }

    public interface ImageLoaderListener {
        public void onImageLoaded(Object result);
    }
}
