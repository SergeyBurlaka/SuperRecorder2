package com.yibogame.superrecorder;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.jakewharton.rxbinding2.view.RxView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        setContentView(R.layout.activity_complex);


        String base = Environment.getExternalStorageDirectory().getPath();
//        String a = String.format("ffmpeg -i " + base + "/jmgc.mp3 -i " + base + "/rwlznsb.mp3 -filter_complex amix=inputs=2:duration=first:dropout_transition=2 -f mp3 " + base + "/output.mp3");
//        BaseCommand baseCommand = new BaseCommand(a) {
//            @Override
//            public String getCommand() {
//                return super.getCommand();
//            }
//        };
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int ret = FFmpegBox.getInstance().execute(baseCommand);
//                Log.d(TAG, "the ret=" + ret+"!!!");
//            }
//        }).start();

//        String path = "file:///android_asset/bg_music_1.mp3";
        File file = new File(base + "/rwlznsb.mp3");
        if (file.exists()) {
            LogUtils.d("文件是存在的啊~");
        }
        RxView.clicks(findViewById(R.id.btn_split))
                .subscribe(o -> {
                    split(base + "/output.mp3", base + "/split1.mp3", 0, 10);
                });

    }

    public static void split(String inputfile, String outputfile, int start, int duration) {

        try {
//            List<String> cmd = new ArrayList<String>();
            StringBuilder cmd = new StringBuilder();
            cmd.append("ffmpeg -y ");
            cmd.append("-i");
            cmd.append(" ");
            cmd.append(inputfile);
            cmd.append(" ");
            cmd.append("-ss");
            cmd.append(" ");
//            cmd.append(getStringTime(start));
            cmd.append("00:00:00");
            cmd.append(" ");
            cmd.append("-t");
            cmd.append(" ");
            cmd.append("00:00:12");
            cmd.append(" ");
            cmd.append("-acodec copy");
            cmd.append(" ");
//            cmd.append(String.valueOf(duration));
            cmd.append(outputfile);
            BaseCommand baseCommand = new BaseCommand(cmd.toString()) {
                @Override
                public String getCommand() {
                    return super.getCommand();
                }
            };
            int ret = FFmpegBox.getInstance().execute(baseCommand);
            LogUtils.d("cmd=" + baseCommand.getCommand() + ",ret=" + ret);
//            exec(cmd);
        } catch (Exception e) {
//            logger.error("音频切片错误:", e);
            LogUtils.e("音频切片错误:" + e.getMessage());
        }
    }

    /**
     * 将秒转成小时分秒 * @param time * @return
     */
    public static String getStringTime(int time) { //小时
        int h = time / 3600;
        int m = (time % 3600) / 60;
        int s = (time % 3600) % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

}
