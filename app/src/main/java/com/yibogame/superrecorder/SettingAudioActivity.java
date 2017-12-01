package com.yibogame.superrecorder;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;

import com.blankj.utilcode.util.BarUtils;
import com.yibogame.superrecorder.cmd.MixCmd;
import com.yibogame.superrecorder.cmd.PCM2Mp3Cmd;

import java.io.IOException;

import cn.gavinliu.android.ffmpeg.box.FFmpegBox;
import cn.gavinliu.android.ffmpeg.box.commands.Command;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by parcool on 2017/11/26.
 */

public class SettingAudioActivity extends BaseActivity {

    String base = Environment.getExternalStorageDirectory().getPath();
    private MediaPlayer myMediaPlayer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_audio);
        View vStatus = findViewById(R.id.v_status);
        vStatus.getLayoutParams().height = BarUtils.getStatusBarHeight();


        Observable.just(true)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showDialog("合成中。。。");
                    }
                })
                .subscribeOn(Schedulers.io())
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        mix();
                        return true;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        dismissProgressDialog();
                    }

                    @Override
                    public void onError(Throwable e) {
                        dismissProgressDialog();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {

                    }
                });


        myMediaPlayer = new MediaPlayer();
        try {
            myMediaPlayer.setDataSource(base + "/test.mp3");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            myMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.play).setOnClickListener(v -> {
            myMediaPlayer.start();
        });


    }

    private void mix() {
        PCM2Mp3Cmd.Builder builder = new PCM2Mp3Cmd.Builder();
        Command command = builder.setChannel(1)
                .setInputFile(base + Config.tempMicFileName)
                .setOutputFile(base + "/a1.mp3")
                .setRate(44100)
                .build();
        int ret = FFmpegBox.getInstance().execute(command);

        PCM2Mp3Cmd.Builder builder2 = new PCM2Mp3Cmd.Builder();
        Command command2 = builder2.setChannel(1)
                .setInputFile(base + Config.tempBgFileName)
                .setOutputFile(base + "/a2.mp3")
                .setRate(44100)
                .build();
        int ret2 = FFmpegBox.getInstance().execute(command2);


//        String input1 =

        MixCmd.Builder builder3 = new MixCmd.Builder().setDurationWhichOne(2).addInputs(base + "/a1.mp3").addInputs(base + "/a2.mp3").setOutputFile(base + "/test.mp3");
        int ret3 = FFmpegBox.getInstance().execute(builder3.build());
    }
}
