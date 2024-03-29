package com.lakshitasuman.musicstation.radio.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;


import com.lakshitasuman.musicstation.BuildConfig;
import com.lakshitasuman.musicstation.R;
import com.lakshitasuman.musicstation.radio.IPlayerService;
import com.lakshitasuman.musicstation.radio.players.PlayState;
import com.lakshitasuman.musicstation.radio.players.selector.PlayerType;
import com.lakshitasuman.musicstation.radio.station.DataRadioStation;
import com.lakshitasuman.musicstation.radio.station.live.ShoutcastInfo;
import com.lakshitasuman.musicstation.radio.station.live.StreamLiveInfo;

public class PlayerServiceUtil {

    private static Context mainContext = null;
    private static boolean mBound;
    private static ServiceConnection serviceConnection;

    public static void startService(Context context) {
        if (mBound) return;

        Intent anIntent = new Intent(context, PlayerService.class);
        anIntent.putExtra(PlayerService.PLAYER_SERVICE_NO_NOTIFICATION_EXTRA, true);
        mainContext = context;
        serviceConnection = getServiceConnection();
        context.bindService(anIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
    }

    public static void bindService(Context context) {
        if (mBound) return;

        mainContext = context;
        serviceConnection = getServiceConnection();
        Intent anIntent = new Intent(context, PlayerService.class);
        context.bindService(anIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
    }

    private static void unBind(Context context) {
        try {
            context.unbindService(serviceConnection);
        } catch (Exception exception) {
        }
        serviceConnection = null;
        mBound = false;
    }

    public static void shutdownService() {
        if (mainContext != null) {
            try {
                if (BuildConfig.DEBUG) {
                    Log.d("PlayerServiceUtil", "PlayerServiceUtil: shutdownService");
                }

                Intent anIntent = new Intent(mainContext, PlayerService.class);
                unBind(mainContext);
                mainContext.stopService(anIntent);
                itsPlayerService = null;
                serviceConnection = null;
            } catch (Exception exception) {
                if (BuildConfig.DEBUG) {
                    Log.d("PlayerServiceUtil", "PlayerServiceUtil: shutdownService E001:" + exception.getMessage());
                }
            }
        }
    }

    private static IPlayerService itsPlayerService;

    private static ServiceConnection getServiceConnection() {
        return new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder binder) {
                if (BuildConfig.DEBUG) {
                    Log.d("PLAYER", "Service came online");
                }
                itsPlayerService = IPlayerService.Stub.asInterface(binder);

                Intent local = new Intent();
                local.setAction(PlayerService.PLAYER_SERVICE_BOUND);
                LocalBroadcastManager.getInstance(mainContext).sendBroadcast(local);
            }

            public void onServiceDisconnected(ComponentName className) {
                if (BuildConfig.DEBUG) {
                    Log.d("PLAYER", "Service offline");
                }
                unBind(mainContext);
                itsPlayerService = null;
            }
        };
    }

    public static boolean isServiceBound() {
        return itsPlayerService != null;
    }

    public static boolean isPlaying() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.isPlaying();
            } catch (RemoteException exception) {
            }
        }
        return false;
    }

    public static PlayState getPlayerState() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getPlayerState();
            } catch (RemoteException exception) {
            }
        }
        return PlayState.Idle;
    }

    public static void stop() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.Stop();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void play(DataRadioStation station) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.SetStation(station);
                itsPlayerService.Play(false);
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void setStation(DataRadioStation station) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.SetStation(station);
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void skipToNext() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.SkipToNext();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void skipToPrevious() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.SkipToPrevious();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void pause(PauseReason pauseReason) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.Pause(pauseReason);
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void resume() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.Resume();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void clearTimer() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.clearTimer();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void addTimer(int secondsAdd) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.addTimer(secondsAdd);
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static long getTimerSeconds() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getTimerSeconds();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return 0;
    }

    public static @NonNull
    StreamLiveInfo getMetadataLive() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getMetadataLive();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return new StreamLiveInfo(null);
    }

    public static String getStationId() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getCurrentStationID();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return null;
    }

    public static DataRadioStation getCurrentStation() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getCurrentStation();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return null;
    }

    public static void getStationIcon(final ImageView holder, final String fromUrl) {
        if (fromUrl == null) {
            return;
        }

        if (fromUrl.trim().equals("")) return;
        Resources r = mainContext.getResources();
        final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, r.getDisplayMetrics());

        Callback imageLoadCallback = new Callback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Exception exception) {
                Picasso.get()
                        .load(fromUrl)
                        .placeholder(ContextCompat.getDrawable(mainContext, R.drawable.ic_photo_24dp))
                        .resize((int) px, 0)
                        .networkPolicy(NetworkPolicy.NO_CACHE)
                        .into(holder);
            }
        };

        Picasso.get()
                .load(fromUrl)
                .placeholder(ContextCompat.getDrawable(mainContext, R.drawable.ic_photo_24dp))
                .resize((int) px, 0)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(holder, imageLoadCallback);
    }

    public static ShoutcastInfo getShoutcastInfo() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getShoutcastInfo();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return null;
    }

    public static void startRecording() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.startRecording();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void stopRecording() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.stopRecording();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static boolean isRecording() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.isRecording();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return false;
    }

    public static String getCurrentRecordFileName() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getCurrentRecordFileName();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return null;
    }

    public static boolean getIsHls() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getIsHls();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return false;
    }

    public static long getTransferredBytes() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getTransferredBytes();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return 0;
    }

    public static long getBufferedSeconds() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getBufferedSeconds();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return 0;
    }

    public static long getLastPlayStartTime() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getLastPlayStartTime();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return 0;
    }

    public static PauseReason getPauseReason() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getPauseReason();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return PauseReason.NONE;
    }

    public static void enableMPD(String hostname, int port) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.enableMPD(hostname, port);
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public void disableMPD() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.disableMPD();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static void warnAboutMeteredConnection(PlayerType playerType) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.warnAboutMeteredConnection(playerType);
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
    }

    public static boolean isNotificationActive() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.isNotificationActive();
            } catch (RemoteException exception) {
                Log.e("", "" + exception);
            }
        }
        return false;
    }
}
