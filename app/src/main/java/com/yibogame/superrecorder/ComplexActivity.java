package com.yibogame.superrecorder;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import cn.gavinliu.android.ffmpeg.box.FFmpegBox;
import cn.gavinliu.android.ffmpeg.box.commands.BaseCommand;

/**
 * Created by parcool on 2017/11/25.
 */

public class ComplexActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String base = Environment.getExternalStorageDirectory().getPath();
        String a = String.format("ffmpeg -i " + base + "/jmgc.mp3 -i " + base + "/rwlznsb.mp3 -filter_complex amix=inputs=2:duration=first:dropout_transition=2 -f mp3 " + base+"/output.mp3");
        BaseCommand baseCommand = new BaseCommand(a) {
            @Override
            public String getCommand() {
                return super.getCommand();
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = FFmpegBox.getInstance().execute(baseCommand);
                Log.d(TAG, "the ret=" + ret+"!!!");
            }
        }).start();
    }
}
