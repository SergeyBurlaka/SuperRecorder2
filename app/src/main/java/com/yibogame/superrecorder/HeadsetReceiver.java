package com.yibogame.superrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.blankj.utilcode.util.LogUtils;

/**
 * Created by parcool on 2017/12/10.
 */

public class HeadsetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_HEADSET_PLUG.equalsIgnoreCase(intent.getAction())) {
            int state = intent.getIntExtra("state", 0);
            if (intent.hasExtra("state")) {
                if (intent.getIntExtra("state", 0) == 0) {
//                    Config.isHeadsetIn = false;
                    //Headset is not plugged
                    LogUtils.d("耳机未插入");
                } else if (intent.getIntExtra("state", 0) == 1)//Headphones into
                {
                    LogUtils.d("耳机已插入");
                    Config.isHeadsetIn = true;
                }
            }
        }
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equalsIgnoreCase(intent.getAction())) {
            //
            LogUtils.d("耳机未插入2");
            Config.isHeadsetIn = false;
        }
    }
}
