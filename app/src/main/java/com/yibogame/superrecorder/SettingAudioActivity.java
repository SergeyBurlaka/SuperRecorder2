package com.yibogame.superrecorder;

import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.yibogame.superrecorder.cmd.MixCmd;
import com.yibogame.superrecorder.cmd.PCM2Mp3Cmd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    private PCMPlayer pcmPlayer;

    private TextView tvLength, tvCurr;
    private int currPosition;
    private ProgressBar pbDuration;
    private Thread threadPlay;
    private int mPlayOffset, mPrimePlaySize;
    private byte[] data = null;
    private int fileLenght;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_audio);
        View vStatus = findViewById(R.id.v_status);
        vStatus.getLayoutParams().height = BarUtils.getStatusBarHeight();

        ivPlay = findViewById(R.id.play);
        tvLength = findViewById(R.id.tv_length);
        tvCurr = findViewById(R.id.tv_curr);
        pbDuration = findViewById(R.id.pb_duration);

        data = readSDFile(base + "/mix.pcm");
        try {
            fileLenght = (int) getFileLength(base + "/mix.pcm");
            LogUtils.d("fileLenght=" + fileLenght);
        } catch (IOException e) {
            e.printStackTrace();
        }


        pcmPlayer = new PCMPlayer(0, 0, 0);
        mPrimePlaySize = pcmPlayer.getBufferSize() * 2;

        findViewById(R.id.play).setOnClickListener(v -> {
            if (!isPlaying) {
                isPlaying = true;
                ivPlay.setImageResource(R.mipmap.ic_pause_status);
                threadPlay = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isPlaying) {
                            pcmPlayer.write(data, mPlayOffset, mPrimePlaySize);
                            mPlayOffset += mPrimePlaySize;
                            LogUtils.d("mPlayOffset=" + mPlayOffset + ",data.length =" + data.length + ",mPlayOffset / data.length=" + (mPlayOffset / data.length));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvCurr.setText(getFormatedLenght((int) (mPlayOffset / (float) data.length * getLength())));
                                    pbDuration.setProgress((int) (mPlayOffset / (float) data.length * getLength()));
                                    if (mPlayOffset >= data.length) {
                                        isPlaying = false;
                                        ivPlay.setImageResource(R.mipmap.ic_play_status);
                                        mPlayOffset = 0;
                                    }
                                }
                            });
                        }
                    }
                });
                threadPlay.start();
            } else {
                isPlaying = false;
                ivPlay.setImageResource(R.mipmap.ic_play_status);
//                threadPlay.interrupt();
            }
        });

        findViewById(R.id.ctv_save).setOnClickListener(v -> {
//            if (myMediaPlayer != null) {
//                ToastUtils.showShort("已保存到：" + base + "/mix" + time + ".mp3");
//            } else {
//                ToastUtils.showShort("文件已被清理，请返回录制！");
//            }
        });

        tvLength.setText(getFormatedLenght(getLength()));
        pbDuration.setMax(getLength());
    }

    private void test() {
        currPosition++;
        if (currPosition == getLength()) {
            currPosition = 0;
            isPlaying = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ivPlay.setImageResource(R.mipmap.ic_play_status);
                }
            });
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvCurr.setText(getFormatedLenght(currPosition));
                pbDuration.setProgress(currPosition);
            }
        });
    }

    private int getLength() {
        int length = 0;
        length = (int) (fileLenght / 88200);
        return length;
    }

    private String getFormatedLenght(int length) {
        int minutes = length / 60;
        int seconds = length % 60;
        return (minutes < 10 ? "0" + minutes : String.valueOf(minutes)) + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }

    public byte[] readSDFile(String fileName) {
        byte[] bytes = new byte[0];
        try {
            int fileLength = (int) getFileLength(fileName);
            if (fileLength == -1){
                finish();
                ToastUtils.showShort("暂无录音文件！");
                return new byte[0];
            }
            bytes = new byte[fileLength];
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileInputStream inputStream = new FileInputStream(fileName);
            try {
                int read = inputStream.read(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    /***
     * 读取文件为byte[]
     * @param fileName
     * @return
     * @throws IOException
     */
    public byte[] readSDFile(String fileName, float db, int offset, int length) {
        byte[] bytes = new byte[length];
        try {
            FileInputStream inputStream = new FileInputStream(fileName);
            inputStream.skip(offset);
            inputStream.read(bytes, 0, length);
            if (db != -1) {
                bytes = VolumeUtil.getInstance().resetVolume(bytes, db);
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public long getFileLength(String fileName) throws IOException {
        return FileUtils.getFileLength(fileName);
    }


    private int caculateLength(int seconds) {
        return seconds * 88200;
    }


}
