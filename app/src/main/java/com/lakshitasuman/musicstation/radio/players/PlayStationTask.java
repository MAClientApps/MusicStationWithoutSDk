package com.lakshitasuman.musicstation.radio.players;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.lakshitasuman.musicstation.radio.ActivityMain;
import com.lakshitasuman.musicstation.radio.CastHandler;
import com.lakshitasuman.musicstation.radio.FavouriteManager;
import com.lakshitasuman.musicstation.radio.HistoryManager;
import com.lakshitasuman.musicstation.R;
import com.lakshitasuman.musicstation.radio.MainRadioHelper;
import com.lakshitasuman.musicstation.radio.Utils;
import com.lakshitasuman.musicstation.radio.players.mpd.MPDClient;
import com.lakshitasuman.musicstation.radio.players.mpd.MPDServerData;
import com.lakshitasuman.musicstation.radio.players.mpd.tasks.MPDPlayTask;
import com.lakshitasuman.musicstation.radio.station.DataRadioStation;

import java.lang.ref.WeakReference;

public class PlayStationTask extends AsyncTask<Void, Void, String> {
    public interface PlayFunc {
        void play(String url);
    }

    public enum ExecutionResult {
        FAILURE,
        SUCCESS,
    }

    public interface PostExecuteTask {
        void onPostExecute(ExecutionResult executionResult);
    }

    private PlayFunc playFunc;
    private PostExecuteTask postExecuteTask;
    private DataRadioStation stationToPlay;
    private WeakReference<Context> contextWeakReference;

    public PlayStationTask(@NonNull DataRadioStation stationToPlay, @NonNull Context ctx,
                           @NonNull PlayFunc playFunc, @Nullable PostExecuteTask postExecuteTask) {
        this.stationToPlay = stationToPlay;
        this.contextWeakReference = new WeakReference<>(ctx);
        this.playFunc = playFunc;
        this.postExecuteTask = postExecuteTask;
    }

    public static PlayStationTask playMPD(MPDClient mpdClient, MPDServerData mpdServerData, DataRadioStation stationToPlay, Context ctx) {
        return new PlayStationTask(stationToPlay, ctx, url -> mpdClient.enqueueTask(mpdServerData, new MPDPlayTask(url, null)), null);
    }

    public static PlayStationTask playExternal(DataRadioStation stationToPlay, Context ctx) {
        return new PlayStationTask(stationToPlay, ctx, url -> {
            Intent share = new Intent(Intent.ACTION_VIEW);
            share.setDataAndType(Uri.parse(url), "audio/*");
            ctx.startActivity(share);
        }, null);
    }

    public static PlayStationTask playCAST(DataRadioStation stationToPlay, Context ctx) {
        MainRadioHelper radioDroidApp = (MainRadioHelper) ctx.getApplicationContext();
        CastHandler castHandler = radioDroidApp.getCastHandler();
        return new PlayStationTask(stationToPlay, ctx, url -> castHandler.playRemote(stationToPlay.Name, url, stationToPlay.IconUrl), null);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Context ctx = contextWeakReference.get();
        if (ctx == null) {
            return;
        }

        LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));

        MainRadioHelper radioDroidApp = (MainRadioHelper) ctx.getApplicationContext();

        HistoryManager historyManager = radioDroidApp.getHistoryManager();
        historyManager.add(stationToPlay);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean autoFavorite = sharedPref.getBoolean("auto_favorite", false);

        if (autoFavorite) {
            FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();
            if (!favouriteManager.has(stationToPlay.StationUuid)) {
                favouriteManager.add(stationToPlay);
                Toast toast = Toast.makeText(ctx, ctx.getString(R.string.notify_autostarred), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        Context ctx = contextWeakReference.get();
        if (ctx != null) {
            MainRadioHelper radioDroidApp = (MainRadioHelper) ctx.getApplicationContext();

            if (!stationToPlay.hasValidUuid()) {
                if (!stationToPlay.refresh(radioDroidApp.getHttpClient(), ctx)) {
                    return null;
                }
            }

            if (isCancelled()) {
                return null;
            }

            return Utils.getRealStationLink(radioDroidApp.getHttpClient(), ctx.getApplicationContext(), stationToPlay.StationUuid);
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Context ctx = contextWeakReference.get();
        if (ctx == null) {
            return;
        }

        LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));

        if (result != null) {
            stationToPlay.playableUrl = result;
            playFunc.play(result);
        } else {
            Toast toast = Toast.makeText(ctx.getApplicationContext(),
                    ctx.getResources()
                            .getText(R.string.error_station_load), Toast.LENGTH_SHORT);
            toast.show();
        }

        if (postExecuteTask != null) {
            postExecuteTask.onPostExecute(result != null ? ExecutionResult.SUCCESS : ExecutionResult.FAILURE);
        }

        super.onPostExecute(result);
    }
}