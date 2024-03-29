package com.lakshitasuman.musicstation.radio.service;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;

import androidx.preference.PreferenceManager;

import com.lakshitasuman.musicstation.radio.HistoryManager;
import com.lakshitasuman.musicstation.radio.MainRadioHelper;
import com.lakshitasuman.musicstation.radio.Utils;
import com.lakshitasuman.musicstation.radio.players.selector.PlayerType;
import com.lakshitasuman.musicstation.radio.station.DataRadioStation;

public class HeadsetConnectionReceiver extends BroadcastReceiver {

    private Boolean headsetConnected = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PlayerServiceUtil.getPauseReason() != PauseReason.BECAME_NOISY) {
            return;
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        boolean resumeOnWiredHeadset = sharedPref.getBoolean("auto_resume_on_wired_headset_connection", false);
        boolean resumeOnBluetoothHeadset = sharedPref.getBoolean("auto_resume_on_bluetooth_a2dp_connection", false);

        if (!resumeOnWiredHeadset && !resumeOnBluetoothHeadset) {
            return;
        }

        if (PlayerServiceUtil.isPlaying()) {
            return;
        }

        boolean play = false;

        if (AudioManager.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
            if (resumeOnWiredHeadset) {
                final int state = intent.getIntExtra("state", 0);
                play = state == 1 && headsetConnected == Boolean.FALSE;
                headsetConnected = state == 1;
            }
        } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction()) ||
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
            if (resumeOnBluetoothHeadset) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
                play = state == BluetoothProfile.STATE_CONNECTED && headsetConnected == Boolean.FALSE;
                headsetConnected = state == BluetoothProfile.STATE_CONNECTED;
            }
        }

        if (play) {
            MainRadioHelper radioDroidApp = (MainRadioHelper) context.getApplicationContext();
            HistoryManager historyManager = radioDroidApp.getHistoryManager();
            DataRadioStation lastStation = historyManager.getFirst();

            if (lastStation != null) {
                if (!PlayerServiceUtil.isPlaying() && !radioDroidApp.getMpdClient().isMpdEnabled()) {
                    Utils.playAndWarnIfMetered(radioDroidApp, lastStation, PlayerType.RADIODROID, () -> Utils.play(radioDroidApp, lastStation));
                }
            }
        }
    }
}
