package com.yibogame.superrecorder;

import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.yibogame.superrecorder.cmd.MixCmd;
import com.yibogame.superrecorder.cmd.PCM2Mp3Cmd;

import java.io.File;
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

    private String time;
    private ImageView ivPlay;
    private boolean isPlaying = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_audio);
        View vStatus = findViewById(R.id.v_status);
        vStatus.getLayoutParams().height = BarUtils.getStatusBarHeight();

        ivPlay = findViewById(R.id.play);

        Observable.just(FileUtils.isFileExists(base + Config.tempMicFileName) && FileUtils.isFileExists(base + Config.tempBgFileName))
                .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Boolean aBoolean) {
                        if (aBoolean) {
                            mix();
                            return Observable.just(true);
                        }
                        return Observable.just(false);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showDialog("合成中。。。");
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
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
                        if (aBoolean) {
                            deletePCM();
                            myMediaPlayer = new MediaPlayer();
                            myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    isPlaying = false;
                                    ivPlay.setImageResource(R.mipmap.ic_play_status);
                                }
                            });
                            try {
                                myMediaPlayer.setDataSource(base + "/mix" + time + ".mp3");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                myMediaPlayer.prepare();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });


        findViewById(R.id.play).setOnClickListener(v -> {
            if (myMediaPlayer != null) {
                if (!isPlaying) {
                    myMediaPlayer.start();
                    ivPlay.setImageResource(R.mipmap.ic_pause_status);
                } else {
                    myMediaPlayer.pause();
                    ivPlay.setImageResource(R.mipmap.ic_play_status);
                }
                isPlaying = !isPlaying;
            } else {
                ToastUtils.showShort("文件已被清理，请返回录制！");
            }
        });

        findViewById(R.id.ctv_save).setOnClickListener(v -> {
            if (myMediaPlayer != null) {
                ToastUtils.showShort("已保存到：" + base + "/mix" + time + ".mp3");
            } else {
                ToastUtils.showShort("文件已被清理，请返回录制！");
            }
        });


    }

    private void mix() {
        time = String.valueOf(System.currentTimeMillis());
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

        MixCmd.Builder builder3 = new MixCmd.Builder().setDurationWhichOne(2).addInputs(base + "/a1.mp3").addInputs(base + "/a2.mp3").setOutputFile(base + "/mix" + time + ".mp3");
        int ret3 = FFmpegBox.getInstance().execute(builder3.build());
    }

    private void deletePCM() {
        File file = new File(base + Config.tempBgFileName);
        if (file.exists()) {
            boolean b = file.delete();
            LogUtils.d("[voice]删除" + (b ? "成功" : "失败！"));
        } else {
            LogUtils.d("[voice]文件不存在！");
        }

        File fileBg = new File(base + Config.tempMicFileName);
        if (fileBg.exists()) {
            boolean b = fileBg.delete();
            LogUtils.d("[bg]删除" + (b ? "成功" : "失败！"));
        } else {
            LogUtils.d("[bg]文件不存在！");
        }
    }
}
