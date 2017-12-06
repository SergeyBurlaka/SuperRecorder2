package com.yibogame.superrecorder;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jakewharton.rxbinding2.view.RxView;
import com.kyleduo.switchbutton.SwitchButton;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.yibogame.superrecorder.cmd.Mp32PCMCmd;
import com.yibogame.superrecorder.interfaces.IRecordListener;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import cn.gavinliu.android.ffmpeg.box.FFmpegBox;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author parcool
 * @date 2017/11/25
 */

public class RecordActivity extends BaseActivity implements IRecordListener {

    private AppCompatSeekBar mACSBMusicVolume;
    private TextView tvBgMusicVolume;
    private TextView tvDuration;
    private CustomTextView ctvRecordPause;
    RecordStatus recordStatus = RecordStatus.NONE;
    //    private static final int RECORD_STATUS_NONE = 0, RECORD_STATUS_RECORDING = 1, RECORD_STATUS_PAUSE = 2;
    private RxPermissions rxPermissions;
    private ProgressBar pbMic, pbBg;
    private SwitchButton switchButton;
    private TextView tvBGDuration;

    private MediaPlayer myMediaPlayer;
    private AudioManager mAudioManager;
    private float currVolume = 0.6f;
    private int bgLength = 52;
    private int currBgLength = 52;
    private Visualizer visualizer;

    private boolean isPlaying = false;
    private int recordingTime;

    //    private float volumeFactor;
    private CutView cutView;


    private Thread threadAddBlankVoice, threadAddBlankBg;

    private void setPlaying(boolean playing) {
        if (this.isPlaying == playing) {
            return;
        }
        this.isPlaying = playing;
        onDataChanged.onChanged();
    }

    private void setRecordStatus(RecordStatus status) {
        if (status == this.recordStatus) {
            return;
        }
        recordStatus = status;
        onDataChanged.onChanged();
        setVoiceRecorderUI();
    }

    private interface OnDataChanged {
        void onChanged();
    }

    private OnDataChanged onDataChanged;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        onDataChanged = new OnDataChanged() {
            @Override
            public void onChanged() {
                LogUtils.w("recordStatus=" + recordStatus + ",isPlaying=" + isPlaying);
                //停止录音且停止播放音乐
                if (recordStatus == RecordStatus.NONE && !isPlaying) {
                    deleteTempFiles();
                    totalNeedSleep = 0;
                    stopCountDownTimer();
                    stopVoiceRecord();
                    stopMp3();
                    stopWriteEmptyVoiceDataThread();
                    stopWriteEmptyBgDataThread();
                    recordingTime = 0;
                    tvDuration.setText(getFormatedLenght(recordingTime / 1000));
                    return;
                }
                if (isPlaying || recordStatus == RecordStatus.RECORDING) {
                    startWriteEmptyBgmThread();
                    startWriteEmptyVoiceThread();
                }
                //暂停录音且暂停播放背景音乐
                if (recordStatus == RecordStatus.PAUSE && !isPlaying) {
                    stopVoiceRecord();

                    pauseMp3();
                    stopCountDownTimer();
                    stopWriteEmptyVoiceDataThread();
                    stopWriteEmptyBgDataThread();
                    return;
                }
                //继续录音但暂停背景音乐
                if (recordStatus == RecordStatus.RECORDING && !isPlaying) {
                    startVoiceRecord();

                    pauseMp3();
                    stopCountDownTimer();
                    return;
                }
                //暂停录音但播放背景音乐
                if (recordStatus == RecordStatus.PAUSE && isPlaying) {
                    stopVoiceRecord();

                    playMp3();
                    startCountDownTimer();
                    return;
                }
                //继续录音且继续播放背景音乐
                if (recordStatus == RecordStatus.RECORDING && isPlaying) {
                    startVoiceRecord();

                    playMp3();
                    startCountDownTimer();
                    return;
                }

                if (recordStatus == RecordStatus.NONE && isPlaying) {
                    playMp3();
                    startCountDownTimer();
                }

            }
        };


        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        cutView = findViewById(R.id.cutview);
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
        mACSBMusicVolume.setProgress((int) (currVolume * 100));
        mACSBMusicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvBgMusicVolume.setText(String.valueOf(i));
                currVolume = i / 100f;
                try {
                    myMediaPlayer.setVolume(currVolume, currVolume);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //下面几句代码后面可以注释
//                if (mAudioManager != null) {
//                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (i / volumeFactor), 0);
//                }
//                currMusicVolumeOri = (float) i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //下一步
        RxView.clicks(findViewById(R.id.ctv_next))
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecordActivity.this)
                            .setTitle("提示")
                            .setMessage("录音已暂停，你是否确定要去下一步？")
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();

                                }
                            })
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(RecordActivity.this, SettingAudioActivity.class);
                                    startActivity(intent);
                                }
                            });
                    setRecordStatus(RecordStatus.PAUSE);
                    setPlaying(false);
                    builder.show();
                });
        RxView.clicks(findViewById(R.id.ctv_cut))
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    setRecordStatus(RecordStatus.PAUSE);
                    setPlaying(false);
                    Intent intent = new Intent(RecordActivity.this, CutActivity.class);
                    startActivity(intent);
                });
        ctvRecordPause = findViewById(R.id.ctv_record_pause);
        //录制/暂停/继续的那个按钮
        RxView.clicks(ctvRecordPause)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    switch (recordStatus) {
                        case NONE:
                            setRecordStatus(RecordStatus.RECORDING);
                            break;
                        case RECORDING:
                            setRecordStatus(RecordStatus.PAUSE);
                            break;
                        case PAUSE:
                            setRecordStatus(RecordStatus.RECORDING);
                            break;
                        default:
                            break;
                    }

                });
        //重录按钮
        RxView.clicks(findViewById(R.id.ctv_record))
                .subscribe(o -> {
                    deleteTempFiles();
                    setRecordStatus(RecordStatus.NONE);
                    setPlaying(false);
                });
        //打开音乐按钮
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    setPlaying(true);
                } else {
                    setPlaying(false);
                }
            }
        });

//        int mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        if (currMusicVolumeOri == -1) {
//            currMusicVolumeOri = mVolume * volumeFactor;
//        }
//        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        volumeFactor = 100f / maxVolume;
        tvBgMusicVolume.setText(String.valueOf((int) (currVolume * 100)));

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


        Observable.just(!FileUtils.isFileExists(base + Config.bgFileMp3) || !FileUtils.isFileExists(base + Config.bgFilePCM))

                .flatMap(new Func1<Boolean, Observable<String>>() {
                    @Override
                    public Observable<String> call(Boolean aBoolean) {
                        if (!aBoolean) {
                            return Observable.just(null);
                        }
                        MoveAssetsToSDCardUtil.getInstance().move(RecordActivity.this, "bg_music_1.mp3", base + Config.bgFileMp3);
                        return Observable.just(base + Config.bgFileMp3);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showDialog("请稍等...");
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .flatMap(new Func1<String, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(String s) {
                        if (s == null) {
                            return Observable.just(true);
                        }
                        Mp32PCMCmd.Builder builder = new Mp32PCMCmd.Builder()
                                .setChannel(1)
                                .setRate(44100)
                                .setInputFile(s)
                                .setOutputFile(base + Config.bgFilePCM);
                        int ret = FFmpegBox.getInstance().execute(builder.build());
                        return Observable.just(true);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        dismissProgressDialog();
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.showShort(e.getMessage());
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {

                    }
                });
    }


    private void deleteTempFiles() {
        File file = new File(base + Config.tempMicFileName);
        if (file.exists()) {
            boolean b = file.delete();
        }

        File fileBg = new File(base + Config.tempBgFileName);
        if (fileBg.exists()) {
            boolean b = fileBg.delete();
        }

        File fileMix = new File(base + "/mix.pcm");
        if (fileMix.exists()) {
            boolean b = fileMix.delete();
        }
    }

    private void setVoiceRecorderUI() {
        switch (recordStatus) {
            case NONE:
                currVolume = 0.6f;
                mACSBMusicVolume.setProgress((int) (currVolume * 100));
                ctvRecordPause.setDrawableTop(R.mipmap.ic_recorder);
                ctvRecordPause.setText("录音");
                pbMic.post(new Runnable() {
                    @Override
                    public void run() {
                        pbMic.setProgress(0);
                    }
                });
                break;
            case PAUSE:
                currVolume = 0.6f;
                mACSBMusicVolume.setProgress((int) (currVolume * 100));
                ctvRecordPause.setDrawableTop(R.mipmap.ic_recorder);
                ctvRecordPause.setText("继续");
                pbMic.post(new Runnable() {
                    @Override
                    public void run() {
                        pbMic.setProgress(0);
                    }
                });
                break;
            case RECORDING:
                currVolume = 0.2f;
                mACSBMusicVolume.setProgress((int) (currVolume * 100));
                ctvRecordPause.setDrawableTop(R.mipmap.ic_recorde_pause);
                ctvRecordPause.setText("暂停");
                break;
            default:
                break;
        }
    }


    private boolean isVoiceThreadInterrupted = false, isBGMThreadInterrupted;

    String base = Environment.getExternalStorageDirectory().getPath();
    private long lastMills;

    private void requestPermissionToStartVoiceRecord() {
        rxPermissions.request(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> { // will emit 2 Permission objects
                            if (granted) {
                                // `permission.name` is granted !
                                startVoiceRecord();
                            } else {
                                // Denied permission with ask never again
                                // Need to go to the settings
                                ToastUtils.showShort("请允许权限后再试！");
                            }
                        }
                );
    }


    private void playMp3() {
        rxPermissions.request(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (mAudioManager == null) {
                        return;
                    }

//                    if (currMusicVolumeOri != -1) {
//                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (currMusicVolumeOri / volumeFactor), 0);
//                    }
                    //暂停后的开始
                    if (myMediaPlayer != null) {
                        myMediaPlayer.start();
                        //start record???
                        if (visualizer != null) {
                            visualizer.setEnabled(true);
                        }
                        startCountDownTimer();
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
                                lastMills = 0;
                            }
                        });
                        myMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                            @Override
                            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                                LogUtils.d("percent=" + percent);
                            }
                        });


                        visualizer = new Visualizer(myMediaPlayer.getAudioSessionId());
                        visualizer.setCaptureSize(Visualizer.getMaxCaptureRate());
                        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                            @Override
                            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int i) {

                            }

                            @Override
                            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int i) {
                                if (myMediaPlayer == null) {
                                    return;
                                }
                                int mills = myMediaPlayer.getCurrentPosition();
                                if (mills - lastMills <= 0) {
                                    lastMills = 0;
                                    return;
                                }
                                if (recordStatus != RecordStatus.RECORDING) {
                                    RecorderUtil.getInstance().appendBlankData((float) (mills - lastMills) / 1000f, base + Config.tempMicFileName);
//                                    RecorderUtil.getInstance().appendBlankData(new byte[bytes.length], base + Config.tempMicFileName);
                                }
                                RecordBgMusicUtil.getInstance().appendMusic(base + "/bg_music_1.pcm", base + Config.tempBgFileName, lastMills, mills - lastMills, currVolume, true);
                                lastMills = mills;
//                                RecordBgMusicUtil.getInstance().appendMusic(base + "/bg_music_1.pcm", base + Config.tempBgFileName, bytes.length, currMusicVolumeOri / 100f, true);
//                                RecordBgMusicUtil.getInstance().appendMusic(base + Config.tempBgFileName, bytes, currMusicVolumeOri / 100f, true);
//                                LogUtils.d("bytes[10]=" + bytes[10]);
//                                RecordBgMusicUtil.getInstance().appendMusic(base + Config.tempBgFileName, bytes, currMusicVolumeOri / (float) mACSBMusicVolume.getMax(), true);
                                updateVisualizer(bytes);
                            }
                        }, Visualizer.getMaxCaptureRate() / 2, false, true);
                        myMediaPlayer.setLooping(true);
                        myMediaPlayer.prepare();
                        myMediaPlayer.setVolume(currVolume, currVolume);
//                        myMediaPlayer.seekTo(myMediaPlayerCurrentPosition);
                        myMediaPlayer.start();
                        visualizer.setEnabled(true);
                        //will record???
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }


//    private int myMediaPlayerCurrentPosition = 0;

    private void pauseMp3() {
        if (myMediaPlayer != null && myMediaPlayer.isPlaying()) {
            myMediaPlayer.pause();
//            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }
//        myMediaPlayerCurrentPosition = myMediaPlayer.getCurrentPosition();
//        myMediaPlayer.pause();
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
            } catch (Exception e) {
//                e.printStackTrace();
            }

        }
    }


    private void stopMp3() {
//        myMediaPlayerCurrentPosition = 0;
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
    private void startCountDownTimer() {
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
                startCountDownTimer();
            }
        };


        countDownTimer.start();
    }

    /**
     * 结束倒计时
     */
    private void stopCountDownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private int mSpectrumNum = 6;


    public void updateVisualizer(byte[] fft) {
        byte[] model = new byte[fft.length / 2 + 1];
        model[0] = (byte) Math.abs(fft[0]);
        for (int i = 2, j = 1; j < mSpectrumNum; ) {
            model[j] = (byte) Math.hypot(fft[i], fft[i + 1]);
            i += 2;
            j++;
        }
        int height = (model[0] + model[1] + model[2] + model[3] + model[4] + model[5]) / mSpectrumNum;
//        double sum=0;
//        for (int i = 0; i < fft.length/2; i++) {
//            double y = (fft[i*2] | fft[i*2+1] << 8) / 32768.0;
//            sum += y * y;
//        }
//        double rms = Math.sqrt(sum / fft.length/2);
//        int dbAmp = (int) (20.0*Math.log10(rms));
        pbBg.setProgress(height);
    }


    @Override
    public void startVoiceRecord() {
        RecorderUtil.getInstance().startRecording(MediaRecorder.AudioSource.MIC, base + Config.tempMicFileName, true, new RecorderUtil.OnVolumeChangeListener() {
            @Override
            public void onVolumeChanged(int readSize, double volume) {
                pbMic.setProgress((int) volume);
                if (!isPlaying) {
                    byte[] bytes = new byte[readSize];
                    RecordBgMusicUtil.getInstance().appendMusic(base + Config.tempBgFileName, bytes, currVolume, true);
                }
//                else {
//                    RecordBgMusicUtil.getInstance().appendMusic(base + "/bg_music_1.pcm", base + Config.tempBgFileName, readSize, currMusicVolumeOri / 100f, true);
//                }
            }
        });
    }


    @Override
    public void stopVoiceRecord() {
        RecorderUtil.getInstance().stopRecording();
    }

    private long needSleep = 100, needSleepBg = 1000, totalNeedSleep = 0;

    private void startWriteEmptyVoiceThread() {
//        if (threadAddBlankVoice != null && threadAddBlankVoice.isAlive() && !threadAddBlankVoice.isInterrupted()) {
//            return;
//        }
//        threadAddBlankVoice = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (!isVoiceThreadInterrupted) {
//                    try {
//                        long preMills = System.currentTimeMillis();
//                        if (recordStatus != RecordStatus.RECORDING) {
//                            RecorderUtil.getInstance().appendBlankData(0.1f, base + Config.tempMicFileName);
//                            needSleep = 100 - (System.currentTimeMillis() - preMills);
//                            needSleep = needSleep > 0 ? needSleep : 0;
//                        } else {
//                            //这一句不能删
//                            needSleep = 100;
//                        }
//                        Thread.sleep(needSleep);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                        isVoiceThreadInterrupted = true;
//                    }
//                }
//            }
//        });
//        threadAddBlankVoice.start();
    }

    private void stopWriteEmptyVoiceDataThread() {
//        if (threadAddBlankVoice != null && threadAddBlankVoice.isAlive() && !threadAddBlankVoice.isInterrupted()) {
//            isVoiceThreadInterrupted = true;
//            threadAddBlankVoice.interrupt();
//            threadAddBlankVoice = null;
//        }
    }

    Subscription subscription;

    private void startWriteEmptyBgmThread() {
//        if (threadAddBlankBg != null && threadAddBlankBg.isAlive() && !threadAddBlankBg.isInterrupted()) {
//            return;
//        }
//        if (threadAddBlankBg != null || threadAddBlankBg == null) {
//            return;
//        }
//        threadAddBlankBg = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (!isBGMThreadInterrupted) {
//                    try {
//                        long preMills = System.currentTimeMillis();
////                        LogUtils.d("(float) needSleepBg / 1000f=" + ((float) needSleepBg / 1000f));
//                        if (recordStatus != RecordStatus.RECORDING) {
//                            RecorderUtil.getInstance().appendBlankData((float) needSleepBg / 1000f, base + Config.tempMicFileName);
//                        }
//
////                        long preMills = System.currentTimeMillis();
//                        if (!isPlaying) {
//                            long preMills2 = System.currentTimeMillis();
//                            RecorderUtil.getInstance().appendBlankData((float) needSleepBg / 1000f, base + Config.tempBgFileName);
////                            needSleepBg = needSleepBg - (System.currentTimeMillis() - preMills2);
////                            needSleepBg = needSleepBg > 0 ? needSleepBg : 0;
//                            totalNeedSleep += needSleepBg;
//                        } else {
////                            needSleepBg = 1000;
//                            if (totalNeedSleep >= 52000) {
//                                totalNeedSleep = 0;
//                            }
//                            totalNeedSleep += needSleepBg;
//                            long preMills3 = System.currentTimeMillis();
//                            RecordBgMusicUtil.getInstance().appendMusic(base + "/bg_music_1.pcm", base + Config.tempBgFileName, totalNeedSleep - needSleepBg, needSleepBg, (float) currMusicVolumeOri / 100f, true);
////                            needSleepBg = needSleepBg - (System.currentTimeMillis() - preMills3);
////                            needSleepBg = needSleepBg > 0 ? needSleepBg : 0;
//                        }
//                        try {
//                            byte[] bytesBg = RecordBgMusicUtil.getInstance().readSDFile(base + Config.tempBgFileName, (int) ((totalNeedSleep - needSleepBg) / 1000f * 88200), (int) (needSleepBg / 1000f * 88200), -1);
//                            byte[] bytesMic = RecordBgMusicUtil.getInstance().readSDFile(base + Config.tempMicFileName, (int) ((totalNeedSleep - needSleepBg) / 1000f * 88200), (int) (needSleepBg / 1000f * 88200), -1);
//                            byte[][] bytes = new byte[][]{bytesBg, bytesMic};
//                            byte[] result = MixUtil.getInstance().averageMix(bytes);
//                            double volume = CutActivity.calculateVolume(result);
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    long pre = System.currentTimeMillis();
//                                    cutView.addVolume(volume);
//                                }
//                            });
////                            LogUtils.d("bytesBg.length=" + bytesBg.length + ",bytesMic.length=" + bytesMic.length + "," + "result.length=" + result.length);
//                            RecordBgMusicUtil.getInstance().writeAudioDataToFile(base + "/mix.pcm", result, true);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        recordingTime += 1000;
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                tvDuration.setText(getFormatedLenght(recordingTime / 1000));
//                            }
//                        });
//                        Thread.sleep(needSleepBg);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                        isBGMThreadInterrupted = true;
//                    }
//                }
//            }
//        });
//        threadAddBlankBg.start();

    }

    private String getFormatedLenght(int length) {
        int minutes = length / 60;
        int seconds = length % 60;
        return (minutes < 10 ? "0" + minutes : String.valueOf(minutes)) + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }

    private void stopWriteEmptyBgDataThread() {
        if (threadAddBlankBg != null && threadAddBlankBg.isAlive() && !threadAddBlankBg.isInterrupted()) {
            isBGMThreadInterrupted = true;
            threadAddBlankBg.interrupt();
            threadAddBlankBg = null;
        }
//        subscription.unsubscribe();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //先应该弹出对话框让用户确认
        deleteTempFiles();
        setPlaying(false);
        setRecordStatus(RecordStatus.NONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteTempFiles();
        setPlaying(false);
        setRecordStatus(RecordStatus.NONE);
    }

}

