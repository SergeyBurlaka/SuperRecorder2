package com.yibogame.superrecorder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.media.AudioManager;
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
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jakewharton.rxbinding2.view.RxView;
import com.kyleduo.switchbutton.SwitchButton;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.yibogame.superrecorder.cmd.Mp32PCMCmd;
import com.yibogame.superrecorder.interfaces.IRecordListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.gavinliu.android.ffmpeg.box.FFmpegBox;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by parcool on 2017/11/25.
 */

public class RecordActivity extends BaseActivity implements IRecordListener {

    private AppCompatSeekBar mACSBMusicVolume;
    private TextView tvBgMusicVolume;
    private TextView tvDuration;
    private CustomTextView ctvRecordPause;
    private int recordStatus = 0;
    private static final int RECORD_STATUS_NONE = 0, RECORD_STATUS_RECORDING = 1, RECORD_STATUS_PAUSE = 2;
    private RxPermissions rxPermissions;
    private ProgressBar pbMic, pbBg;
    private SwitchButton switchButton;
    private TextView tvBGDuration;

    private MediaPlayer myMediaPlayer;
    private AudioManager mAudioManager;
    private int currMusicVolume = -1;
    private int bgLength = 52;
    private int currBgLength = 52;
    private Visualizer visualizer;

    private List<Map<String, Long>> history = new ArrayList<>();

    private boolean isRecordingActivity = false;
    private Thread threadAddBlankVoice;


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
                Map<String, Long> map = new HashMap<>();
                map.put("onSeekBarChanged" + i, System.currentTimeMillis());
                history.add(map);
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
                    Map<String, Long> map = new HashMap<>();
                    map.put("stopRecord", System.currentTimeMillis());
                    history.add(map);

                    Intent intent = new Intent(RecordActivity.this, CutActivity.class);
                    startActivity(intent);
                });
        ctvRecordPause = findViewById(R.id.ctv_record_pause);
        //录制按钮
        RxView.clicks(ctvRecordPause)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    switch (recordStatus) {
                        case RECORD_STATUS_NONE:
                            startVoiceRecord();
                            isRecordingActivity = true;
                            Map<String, Long> map = new HashMap<>();
                            map.put("startRecord", System.currentTimeMillis());
                            history.add(map);
                            break;
                        case RECORD_STATUS_PAUSE:
                            goOnVoiceRecordUI();
                            Map<String, Long> map2 = new HashMap<>();
                            map2.put("goOnRecord", System.currentTimeMillis());
                            history.add(map2);
                            break;
                        case RECORD_STATUS_RECORDING:
                            pauseVoiceRecordUI();
                            Map<String, Long> map3 = new HashMap<>();
                            map3.put("pauseRecord", System.currentTimeMillis());
                            history.add(map3);
                            break;
                        default:
                            break;
                    }

                });
        //重录按钮
        RxView.clicks(findViewById(R.id.ctv_record))
                .subscribe(o -> {
                    stopMp3();
                    stopTimer();
                    stopVoiceRecordUI();
                    Map<String, Long> map = new HashMap<>();
                    map.put("reStartRecord", System.currentTimeMillis());
                    history.add(map);
                });

        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    playMp3();
                    Map<String, Long> map = new HashMap<>();
                    map.put("playMp3", System.currentTimeMillis());
                    history.add(map);
                } else {
                    pauseMp3();
                    Map<String, Long> map = new HashMap<>();
                    map.put("pauseMp3", System.currentTimeMillis());
                    history.add(map);
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

        RxView.clicks(findViewById(R.id.tv_change_bg))
                .subscribe(o -> {
                    Intent intent = new Intent(RecordActivity.this, PCMPlayerActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("voiceSampleRateInHz", RecorderUtil.getInstance().getSampleRateInHz());
                    bundle.putInt("voiceChannelConfig", RecorderUtil.getInstance().getChannelConfig());
                    bundle.putInt("voiceAudioFormat", RecorderUtil.getInstance().getAudioFormat());
                    bundle.putInt("voiceBufferSizeInBytes", RecorderUtil.getInstance().getBufferSizeInBytes());
                    intent.putExtra("bundle", bundle);
                    startActivity(intent);
                });


        Observable.just(true)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showDialog("请稍等...");
                    }
                })
                .subscribeOn(Schedulers.io())
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        Mp32PCMCmd.Builder builder = new Mp32PCMCmd.Builder()
                                .setChannel(1)
                                .setRate(44100)
                                .setInputFile(base + "/bg_music_1.mp3")
                                .setOutputFile(base + "/bg_music_1.pcm");
                        int ret = FFmpegBox.getInstance().execute(builder.build());
                        LogUtils.d("ret=" + ret);
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

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {

                    }
                });
        threadAddBlankVoice = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isInterrupted) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        isInterrupted = true;
                    }
                    if (!isRecordingActivity) {
                        RecorderUtil.getInstance().appendBlankData(0.01f);
                    }
                }
            }
        });

    }

    private boolean isInterrupted = false;

    String base = Environment.getExternalStorageDirectory().getPath();

    private void startVoiceRecord() {
        rxPermissions.request(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> { // will emit 2 Permission objects
                            if (granted) {
                                // `permission.name` is granted !
                                startRecord();
                                if (!threadAddBlankVoice.isAlive()) {
                                    threadAddBlankVoice.start();
                                }
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


    private void pauseVoiceRecordUI() {
        recordStatus = RECORD_STATUS_PAUSE;
        ctvRecordPause.setDrawableTop(R.mipmap.ic_recorde_pause);
        ctvRecordPause.setText("继续");
//        mRecorderVoice.stop();//继续录音，让它录制静音，只为了保证两个长度一样。
        stopVoiceRecordUI();
    }

    private void stopVoiceRecordUI() {
        isRecordingActivity = false;
        recordStatus = RECORD_STATUS_NONE;
        ctvRecordPause.setDrawableTop(R.mipmap.ic_recorder);
        ctvRecordPause.setText("录音");
        stopRecord();
    }

    private void goOnVoiceRecordUI() {
        isRecordingActivity = true;
        recordStatus = RECORD_STATUS_RECORDING;
        ctvRecordPause.setDrawableTop(R.mipmap.ic_recorder);
        ctvRecordPause.setText("暂停");
        startVoiceRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecord();
        stopEmpty();
    }


    private void playMp3() {
        if (mAudioManager == null) {
            return;
        }
        if (currMusicVolume != -1) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currMusicVolume, 0);
        }
        if (myMediaPlayer != null) {
            myMediaPlayer.seekTo(myMediaPlayerCurrentPosition);
            myMediaPlayer.start();
            //start record???
            if (visualizer != null) {
                visualizer.setEnabled(true);
            }
            startTimer();
            return;
        }


        AssetFileDescriptor fileDescriptor;
        try {
            fileDescriptor = RecordActivity.this.getAssets().openFd("bg_music_1.mp3");
            myMediaPlayer = new MediaPlayer();
            myMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            myMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());
            myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    LogUtils.e("MediaPlayer onCompletion!");
                    bgLength = 52;
                    currBgLength = bgLength;
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
            myMediaPlayer.setLooping(true);
            myMediaPlayer.prepare();
            myMediaPlayer.seekTo(myMediaPlayerCurrentPosition);
            myMediaPlayer.start();
            visualizer.setEnabled(true);
            startTimer();
            //will record???
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private int myMediaPlayerCurrentPosition = 0;

    private void pauseMp3() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        myMediaPlayerCurrentPosition = myMediaPlayer.getCurrentPosition();
//        myMediaPlayer.pause();
        visualizer.setEnabled(false);
        stopTimer();
    }


    private void stopMp3() {
        myMediaPlayerCurrentPosition = 0;
        //stop record???
        if (myMediaPlayer != null) {
            myMediaPlayer.stop();
            myMediaPlayer.release();
        }
        if (visualizer != null) {
            visualizer.release();
        }
        myMediaPlayer = null;
        switchButton.setCheckedImmediatelyNoEvent(false);
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
            @Override
            public void onTick(long millisUntilFinished) {
//                LogUtils.d("millisUntilFinished=" + millisUntilFinished);
                currBgLength = (int) (millisUntilFinished / 1000);
                int lefts = (int) (millisUntilFinished / 1000);
                tvBGDuration.setText("00:" + (lefts < 10 ? "0" + lefts : lefts));
            }

            @Override
            public void onFinish() {
                tvBGDuration.setText("00:00");
                bgLength = 52;
                currBgLength = bgLength;
                startTimer();
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
        int height = (model[0] + model[1] + model[2] + model[3] + model[4] + model[5]) / mSpectrumNum;
        pbBg.setProgress(height);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopMp3();
        stopTimer();
        stopVoiceRecordUI();
        stopRecord();
    }

    @Override
    public void initRecorder() {

    }

    @Override
    public void startRecord() {
        RecorderUtil.getInstance().startRecording(MediaRecorder.AudioSource.MIC, base + "/temp_mic.pcm", true, new RecorderUtil.OnVolumeChangeListener() {
            @Override
            public void onVolumeChanged(double volume) {
                pbMic.setProgress((int) volume);
            }
        });
    }

    @Override
    public void pauseRecord() {
        RecorderUtil.getInstance().stopRecording();
    }

    @Override
    public void stopRecord() {
        isRecordingActivity = false;
        RecorderUtil.getInstance().stopRecording();
    }

    private void stopEmpty(){
        if (threadAddBlankVoice.isAlive() && !threadAddBlankVoice.isInterrupted()) {
            threadAddBlankVoice.interrupt();
        }
    }
}

