package com.yibogame.superrecorder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jakewharton.rxbinding2.view.RxView;
import com.kyleduo.switchbutton.SwitchButton;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.yibogame.superrecorder.interfaces.IBufferDataChangeInterface;
import com.yibogame.superrecorder.interfaces.IOnRecordingListener;
import com.yibogame.superrecorder.interfaces.IVolumeChangeListener;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.util.concurrent.TimeUnit;

/**
 * Created by parcool on 2017/11/25.
 */

public class RecordActivity extends BaseActivity {

    private AppCompatSeekBar mACSBMusicVolume;
    private TextView tvBgMusicVolume;
    private TextView tvDuration;
    private CustomTextView ctvRecordPause;
    private int recordStatus = 0;
    private static final int RECORD_STATUS_NONE = 0, RECORD_STATUS_RECORDING = 1, RECORD_STATUS_PAUSE = 2;
    private RxPermissions rxPermissions;
    private Recorder mRecorderVoice, mRecorderBg;
    private IBufferDataChangeInterface interfaceVoice = new IBufferDataChangeInterface() {
        @Override
        public void onDataChange(int position, Buffer buffer) {
            LogUtils.d("position=" + position);
        }
    };
    private ProgressBar pbMic, pbBg;
    private SwitchButton switchButton;
    private TextView tvBGDuration;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        rxPermissions = new RxPermissions(this);
        tvBGDuration = findViewById(R.id.tv_duration_of_bg);
        switchButton = findViewById(R.id.switch_button);
        pbMic = findViewById(R.id.pb_mic);
        pbBg = findViewById(R.id.pb_bg);
        View vStatus = findViewById(R.id.v_status);
        vStatus.getLayoutParams().height = BarUtils.getStatusBarHeight();

        tvDuration = findViewById(R.id.tv_duration);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "font.TTF");
        tvDuration.setTypeface(typeface);
        tvBgMusicVolume = findViewById(R.id.tv_bg_music_volume);
        mACSBMusicVolume = findViewById(R.id.bg_music_volume);
        mACSBMusicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvBgMusicVolume.setText(String.valueOf(i));
                if (mAudioManager != null) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
                }
                currMusicVolume = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tvBgMusicVolume.setText(String.valueOf(mACSBMusicVolume.getProgress()));
        RxView.clicks(findViewById(R.id.ctv_next))
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    Intent intent = new Intent(RecordActivity.this, SettingAudioActivity.class);
                    startActivity(intent);
                });
        RxView.clicks(findViewById(R.id.ctv_cut))
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    Intent intent = new Intent(RecordActivity.this, CutActivity.class);
                    startActivity(intent);
                });
        ctvRecordPause = findViewById(R.id.ctv_record_pause);
        //录制按钮
        RxView.clicks(ctvRecordPause)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    LogUtils.d("click record!");
                    switch (recordStatus) {
                        case RECORD_STATUS_NONE:
                            startVoiceRecord();
                            break;
                        case RECORD_STATUS_PAUSE:
                            goOnVoiceRecord();
                            break;
                        case RECORD_STATUS_RECORDING:
                            pauseVoiceRecord();
                            break;
                    }

                });
        //重录按钮
        RxView.clicks(findViewById(R.id.ctv_record))
                .subscribe(o -> stopVoiceRecord());

        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    playMp3();
                } else {
                    pauseMp3();
                }
            }
        });

        int mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (currMusicVolume == -1) {
            currMusicVolume = mVolume;
            tvBgMusicVolume.setText(String.valueOf(mVolume));
        } else {
            tvBgMusicVolume.setText(String.valueOf(currMusicVolume));
        }
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mACSBMusicVolume.setMax(maxVolume);
        mACSBMusicVolume.setProgress(currMusicVolume);
    }

    private void startVoiceRecord() {
        rxPermissions.request(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> { // will emit 2 Permission objects
                            if (granted) {
                                // `permission.name` is granted !
                                mRecorderVoice = new Recorder(
                                        MediaRecorder.AudioSource.MIC/*AudioSource*/,
                                        512/*每次多少个采样*/,
                                        interfaceVoice/*接受数据的监听，如果不需要可以填null*/);
                                mRecorderVoice.setOnPeriodInFramesChangeListener(new Recorder.OnPeriodInFramesChangeListener() {
                                    @Override
                                    public void onFrames(AudioRecord record) {
                                        LogUtils.d("getNotificationMarkerPosition=" + record.getNotificationMarkerPosition() + ",getPositionNotificationPeriod=" + record.getPositionNotificationPeriod());
                                    }
                                });
                                mRecorderVoice.setOnRecordingListener(new IOnRecordingListener() {
                                    @Override
                                    public void onDataReceived(short[] mPCMBuffer, int readSize, double volume) {
                                        String base = Environment.getExternalStorageDirectory().getPath();
                                        File file = new File(base + "/temp_mic.pcm");
                                        FileIOUtils.writeFileFromBytesByChannel(file, toByteArray(mPCMBuffer), true);
                                        pbMic.setProgress((int) volume);

                                    }
                                });
                                mRecorderVoice.startRecording();


                                recordStatus = RECORD_STATUS_RECORDING;
                                ctvRecordPause.setDrawableTop(R.mipmap.ic_recorder);
                                ctvRecordPause.setText("暂停");
                            } else {
                                // Denied permission with ask never again
                                // Need to go to the settings
                                ToastUtils.showShort("请允许权限后再试！");
                            }
                        }
                );


    }

    private void pauseVoiceRecord() {
        recordStatus = RECORD_STATUS_PAUSE;
        ctvRecordPause.setDrawableTop(R.mipmap.ic_recorde_pause);
        ctvRecordPause.setText("继续");
        mRecorderVoice.stop();
    }

    private void stopVoiceRecord() {
        recordStatus = RECORD_STATUS_NONE;
        ctvRecordPause.setDrawableTop(R.mipmap.ic_recorder);
        ctvRecordPause.setText("录音");
    }

    private void goOnVoiceRecord() {
        recordStatus = RECORD_STATUS_RECORDING;
        ctvRecordPause.setDrawableTop(R.mipmap.ic_recorder);
        ctvRecordPause.setText("暂停");
        startVoiceRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecorderVoice.stop();
        mRecorderVoice.release();
    }

    public byte[] toByteArray(short[] src) {
        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2] = (byte) (src[i]);
            dest[i * 2 + 1] = (byte) (src[i] >> 8);
        }
        return dest;
    }


    private MediaPlayer myMediaPlayer;
    private AudioManager mAudioManager;
    private int currMusicVolume = -1;
    private int bgLength = 52;
    private int currBgLength = 52;
    private Visualizer visualizer;

    private void playMp3() {
        if (mAudioManager == null) {
            return;
        }
        if (myMediaPlayer != null) {
            myMediaPlayer.start();
            if (visualizer != null) {
                visualizer.setEnabled(true);
            }
            startTimer();
            return;
        }
        if (currMusicVolume != -1) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currMusicVolume, 0);
        }

        AssetFileDescriptor fileDescriptor;
        try {
            fileDescriptor = RecordActivity.this.getAssets().openFd("rwlznsb.mp3");
            myMediaPlayer = new MediaPlayer();
            myMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            myMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());
            myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopMp3();
                    bgLength = 52;
                    if (visualizer != null) {
                        try {
                            visualizer.setEnabled(false);
                        } catch (Exception e) {
                            visualizer.release();
                        }

                    }
                    stopTimer();
                    playMp3();

                }
            });
            visualizer = new Visualizer(myMediaPlayer.getAudioSessionId());
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int i) {

                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int i) {
                    updateVisualizer(bytes);
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true);

            myMediaPlayer.prepare();
            myMediaPlayer.start();
            visualizer.setEnabled(true);
            startTimer();
            mRecorderBg = new Recorder(
                    MediaRecorder.AudioSource.DEFAULT/*AudioSource*/,
                    512/*每次多少个采样*/,
                    interfaceVoice/*接受数据的监听，如果不需要可以填null*/);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pauseMp3() {
        myMediaPlayer.pause();
        visualizer.setEnabled(false);
        stopTimer();
    }

    private void stopMp3() {
        myMediaPlayer.stop();
        myMediaPlayer.release();
        visualizer.release();
        myMediaPlayer = null;
    }

    private CountDownTimer countDownTimer;

    /**
     * 开启倒计时
     */
    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(currBgLength * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                LogUtils.d("millisUntilFinished=" + millisUntilFinished);
                currBgLength = (int) (millisUntilFinished / 1000);
                int lefts = (int) (millisUntilFinished / 1000);
                tvBGDuration.setText("00:" + (lefts < 10 ? "0" + lefts : lefts));
            }

            public void onFinish() {
                tvBGDuration.setText("00:00");
                bgLength = 52;
                currBgLength = bgLength;
            }
        };


        countDownTimer.start();
    }

    /**
     * 结束倒计时
     */
    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private int mSpectrumNum = 6;

    /***
     * 因为频谱可分1024个，但是这里需求的是只有一个频谱展示的地方，那就只能做几个（随便取6个）采样，然后取一个平均值了.很显然，这个值肯定不能代表全部所以会看上去不准确
     * @param fft
     */
    public void updateVisualizer(byte[] fft) {
        byte[] model = new byte[fft.length / 2 + 1];
        model[0] = (byte) Math.abs(fft[0]);
        for (int i = 2, j = 1; j < mSpectrumNum; ) {
            model[j] = (byte) Math.hypot(fft[i], fft[i + 1]);
            i += 2;
            j++;
        }
        int height = (model[0]+model[1]+model[2]+model[3]+model[4]+model[5])/mSpectrumNum;
        pbBg.setProgress(height);
    }
}

