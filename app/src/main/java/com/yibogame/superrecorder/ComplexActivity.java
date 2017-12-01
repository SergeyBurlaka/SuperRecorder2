package com.yibogame.superrecorder;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.blankj.utilcode.util.LogUtils;
import com.jakewharton.rxbinding2.view.RxView;
import com.yibogame.superrecorder.cmd.ChangeVolumeCmd;
import com.yibogame.superrecorder.cmd.ConcatCmd;
import com.yibogame.superrecorder.cmd.CutCmd;
import com.yibogame.superrecorder.cmd.MixCmd;
import com.yibogame.superrecorder.cmd.PCM2Mp3Cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.gavinliu.android.ffmpeg.box.FFmpegBox;
import cn.gavinliu.android.ffmpeg.box.commands.Command;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * @author parcool
 * @date 2017/11/25
 */

public class ComplexActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();
    private List<Map<String, String>> listOperationRecord = new ArrayList<>();
    private String base = Environment.getExternalStorageDirectory().getPath();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complex);


        RxView.clicks(findViewById(R.id.btn_split))
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe(o -> {
                    Observable.just(base + "/jmgc.mp3")
                            .subscribeOn(Schedulers.io())
//                            .map(s -> {
//                                LogUtils.w("getStringTime(1)=" + getStringTime(1) + ",getStringTime(1 + 10)=" + getStringTime(1 + 10));
//                                split(s, base + "/split1.mp3", getStringTime(0), getStringTime(10));
//                                return base + "/split1.mp3";
//                            })
//                            .map(s -> {
//                                setVolume(s, base + "/split1_1.mp3", 3.5f);
//                                return base + "/split1_1.mp3";
//                            })
//                            .map(s->{
//                                return base + "/jmgc.mp3";
//                            })
//                            .map(s -> {
//                                LogUtils.w("getStringTime(65)=" + getStringTime(65) + ",getStringTime(65 + 10)=" + getStringTime(65 + 10));
//                                split(s, base + "/split2.mp3", getStringTime(65), getStringTime(10));
//                                return base + "/split2.mp3";
//                            })
//                            .map(s -> {
//                                setVolume(s, base + "/split2_2.mp3", 2.5f);
//                                return base + "/split2_2.mp3";
//                            })
//                            .map(s -> {
//                                List<String> list = new ArrayList<>();
//                                list.add(base+"/split1_1.mp3");
//                                list.add(base+"/split2_2.mp3");
//                                contact(list,base+"/concat.mp3");
//                                return "contact.mp3";
//                            })
//                            .map(s -> {
//                                List<String> list = new ArrayList<>();
//                                list.add(base+Config.tempMicFileName);
//                                list.add(base+"/split2_2.mp3");
//                                mix(list,1,base+"/mix.mp3");
//                                return "success!";
//                            })
                            .map(s -> {
                                cast(base + Config.tempMicFileName, base + "/temp_mic.mp3");
                                return "success!";
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Subscriber<String>() {
                                @Override
                                public void onCompleted() {

                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onNext(String s) {
                                    LogUtils.i("成功了？" + s);
                                }
                            });
                });

    }

    public void split(String inputfile, String outputfile, String start, String endTime) {
        CutCmd.Builder cutCmdBuilder = new CutCmd.Builder();
        Command cutCmd = cutCmdBuilder.setInputFile(inputfile)
                .setOutputFile(outputfile)
                .setStartTime(start)
                .setEndTime(endTime)
                .build();
        int ret = FFmpegBox.getInstance().execute(cutCmd);
        LogUtils.d("split cmd=" + cutCmd.getCommand() + ",ret=" + ret);
    }

    public void setVolume(String inputFile, String outputFile, float times) {
        ChangeVolumeCmd.Builder builder = new ChangeVolumeCmd.Builder();
        Command command = builder.setInputFile(inputFile)
                .setOutputFile(outputFile)
                .setTimes(times)
                .build();
        LogUtils.d("setVolume cmd=" + command.getCommand());
        int ret = FFmpegBox.getInstance().execute(command);
        LogUtils.d("ret=" + ret);
    }

    public void contact(List<String> list, String outputFile) {
        ConcatCmd.Builder builder = new ConcatCmd.Builder();
        for (String s : list) {
            builder.addInputs(s);
        }
        Command command = builder.setOutputFile(outputFile).build();
        LogUtils.d("contact cmd=" + command.getCommand());
        int ret = FFmpegBox.getInstance().execute(command);
    }

    public void mix(List<String> list, int whichOne, String outputFile) {
        MixCmd.Builder builder = new MixCmd.Builder();
        for (String s : list) {
            builder.addInputs(s);
        }
        Command command = builder.setDurationWhichOne(whichOne)
                .setOutputFile(outputFile)
                .build();
        LogUtils.d("mix cmd=" + command.getCommand());
        int ret = FFmpegBox.getInstance().execute(command);
    }

    public void cast(String inputFile, String outputFile) {
        PCM2Mp3Cmd.Builder builder = new PCM2Mp3Cmd.Builder();
        Command command = builder.setChannel(1)
                .setInputFile(inputFile)
                .setOutputFile(outputFile)
                .setRate(44100)
                .build();
        LogUtils.d("cast cmd=" + command.getCommand());
        int ret = FFmpegBox.getInstance().execute(command);
    }

    /**
     * 将秒转成小时分秒 * @param time * @return
     */
    public static String getStringTime(int time) { //小时
        int h = time / 3600;
        int m = (time % 3600) / 60;
        int s = (time % 3600) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
    }

    private void getMockOperationRecord() {
        List<Map<String, String>> listOperationRecord = new ArrayList<>();
        Map<String, String> map1 = new HashMap<>(4);
        map1.put("music", base + "/xjhw.mp3");
        map1.put("start", "0");
        map1.put("end", "10");
        map1.put("volume", "0.5f");
    }
}
