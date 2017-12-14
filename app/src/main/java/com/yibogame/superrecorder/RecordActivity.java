package com.yibogame.superrecorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
    private RxPermissions rxPermissions;
    private ProgressBar pbMic, pbBg;
    private SwitchButton switchButton;
    private TextView tvBGDuration;

    private MediaPlayer myMediaPlayer;
    private int mediaPlayerStatus = 0;//0:停止；1：播放中；2：暂停
    private float currVolume = 0.6f;

    private int currBgLength = 0;
    private Visualizer visualizer;

    private boolean isPlaying = false;

    private CutView cutView;

    private long playedStartMills;

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
    private HeadsetReceiver mHeadsetPlugUnplugBroadcastReceiver;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHeadsetPlugUnplugBroadcastReceiver = new HeadsetReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mHeadsetPlugUnplugBroadcastReceiver, intentFilter);
        setContentView(R.layout.activity_record);

        onDataChanged = new OnDataChanged() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onChanged() {
                if (recordStatus == RecordStatus.RECORDING) {
                    RecorderUtil.getInstance().setReallyRecord(true);
                } else {
                    RecorderUtil.getInstance().setReallyRecord(false);
                }

                if (isPlaying) {
                    playMp3();
                    startCountDownTimer();
                } else {
                    pauseMp3();
                    stopCountDownTimer();
                }

                if (recordStatus == RecordStatus.RECORDING || isPlaying) {
                    requestPermissionToStartVoiceRecord();
                } else if ((recordStatus == RecordStatus.NONE || recordStatus == RecordStatus.PAUSE) && !isPlaying) {
                    currBgLength = (int) (FileUtils.getFileLength(base + Config.bgFilePCM) / 88200);
                    tvBGDuration.setText(getFormatedLenght(currBgLength));
                    stopMp3();
                    stopVoiceRecord();
                }


//                deleteTempFiles();
            }
        };


//        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
        //音量滑块滑动
        mACSBMusicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvBgMusicVolume.setText(String.valueOf(i));
                currVolume = i / 100f;
                if (myMediaPlayer != null) {
                    myMediaPlayer.setVolume(currVolume, currVolume);
                }
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
                                    startActivityForResult(intent, 3);
                                }
                            });
                    setRecordStatus(RecordStatus.PAUSE);
                    setPlaying(false);
                    builder.show();
                });
        //试听
        RxView.clicks(findViewById(R.id.ctv_listen))
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    setRecordStatus(RecordStatus.PAUSE);
                    setPlaying(false);
                    Intent intent = new Intent(RecordActivity.this, CutActivity.class);
                    intent.putExtra("isCut", false);
                    startActivityForResult(intent, 2);
                });
        //裁剪
        RxView.clicks(findViewById(R.id.ctv_cut))
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    setRecordStatus(RecordStatus.PAUSE);
                    setPlaying(false);
                    Intent intent = new Intent(RecordActivity.this, CutActivity.class);
                    intent.putExtra("isCut", true);
                    startActivityForResult(intent, 2);
                });
        ctvRecordPause = findViewById(R.id.ctv_record_pause);
        //录制/暂停/继续的那个按钮
        RxView.clicks(ctvRecordPause)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe(o -> {
                    switch (recordStatus) {
                        case NONE:
                            RecorderUtil.getInstance().setReallyRecord(true);
                            setRecordStatus(RecordStatus.RECORDING);
                            break;
                        case RECORDING:
                            RecorderUtil.getInstance().setReallyRecord(false);
                            setRecordStatus(RecordStatus.PAUSE);
                            break;
                        case PAUSE:
                            RecorderUtil.getInstance().setReallyRecord(true);
                            setRecordStatus(RecordStatus.RECORDING);
                            break;
                        default:
                            break;
                    }

                });
        //重录按钮
        RxView.clicks(findViewById(R.id.ctv_record))
                .subscribe(o -> {
                    rerecord();
                });
        //打开音乐开关按钮
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setPlaying(b);
            }
        });
        tvBgMusicVolume.setText(String.valueOf((int) (currVolume * 100)));
        //换背景音乐
        RxView.clicks(findViewById(R.id.tv_change_bg))
                .subscribe(o -> {
//                    Intent intent = new Intent(RecordActivity.this, PCMPlayerActivity.class);
//                    Bundle bundle = new Bundle();
//                    bundle.putInt("voiceSampleRateInHz", RecorderUtil.getInstance().getSampleRateInHz());
//                    bundle.putInt("voiceChannelConfig", RecorderUtil.getInstance().getChannelConfig());
//                    bundle.putInt("voiceAudioFormat", RecorderUtil.getInstance().getAudioFormat());
//                    bundle.putInt("voiceBufferSizeInBytes", RecorderUtil.getInstance().getBufferSizeInBytes());
//                    intent.putExtra("bundle", bundle);
//                    startActivity(intent);
                    Intent intent = new Intent(RecordActivity.this, ChoiceBgMusicActivity.class);
                    setPlaying(false);
                    setRecordStatus(RecordStatus.NONE);
                    startActivityForResult(intent, 1);
                });
        //返回按钮
        findViewById(R.id.iv_back).setOnClickListener(v -> {
            onBackPressed();
        });
        //初始化背景音乐
        initBgMusic();
    }

    private void initBgMusic() {
        Observable.just(FileUtils.isFileExists(base + Config.bgFileMp3) && FileUtils.isFileExists(base + Config.bgFilePCM))
                .flatMap(new Func1<Boolean, Observable<String>>() {
                    @Override
                    public Observable<String> call(Boolean aBoolean) {
                        if (aBoolean) {
                            currBgLength = (int) (FileUtils.getFileLength(base + Config.bgFilePCM) / 88200);
                            return Observable.just(null);
                        }
                        MoveAssetsToSDCardUtil.getInstance().move(RecordActivity.this, Config.bgFileMp3.substring(1), base + Config.bgFileMp3);
                        currBgLength = (int) (FileUtils.getFileLength(base + Config.bgFilePCM) / 88200);
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


    private void rerecord() {
        pbBg.setProgress(0);
        if (cutView != null) {
            cutView.clear();
        }
        if (tvDuration != null) {
            tvDuration.setText(getFormatedLenght(0));
        }
        deleteTempFiles();
        setRecordStatus(RecordStatus.NONE);
        setPlaying(false);
    }

    private void deleteTempFiles() {
        File file = new File(base + Config.tempMicFileName);
        if (file.exists()) {
            boolean b = file.delete();
        }
        deleteTempBgFile();

        File fileMix = new File(base + "/mix.pcm");
        if (fileMix.exists()) {
            boolean b = fileMix.delete();
        }
    }

    private void deleteTempBgFile() {
        File fileBg = new File(base + Config.tempBgFileName);
        if (fileBg.exists()) {
            boolean b = fileBg.delete();
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

    String base = Environment.getExternalStorageDirectory().getPath();

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
                    if (granted) {
                        switch (mediaPlayerStatus) {
                            case 0:
                                //已经停止了，准备播放吧
                                AssetFileDescriptor fileDescriptor;
                                try {
                                    fileDescriptor = RecordActivity.this.getAssets().openFd(Config.bgFileMp3.substring(1));
                                    myMediaPlayer = new MediaPlayer();
                                    myMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                    myMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
                                            fileDescriptor.getStartOffset(),
                                            fileDescriptor.getLength());
                                    myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mediaPlayer) {
                                            LogUtils.e("MediaPlayer onCompletion!");
                                            currBgLength = (int) (FileUtils.getFileLength(base + Config.bgFilePCM) / 88200);
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
                                            updateVisualizer(bytes);
                                        }
                                    }, Visualizer.getMaxCaptureRate() / 2, false, true);
                                    myMediaPlayer.setLooping(true);
                                    myMediaPlayer.prepare();
                                    myMediaPlayer.start();
                                    playedStartMills = System.currentTimeMillis();
                                    myMediaPlayer.setVolume(currVolume, currVolume);
                                    mediaPlayerStatus = 1;
                                    visualizer.setEnabled(true);
                                    switchButton.setCheckedImmediatelyNoEvent(true);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 1:
                                //播放中，直接return吧
                                //do nothing
                                break;
                            case 2:
                                //暂停中，可直接播放
                                if (myMediaPlayer != null) {
                                    myMediaPlayer.start();
                                    if (visualizer != null) {
                                        visualizer.setEnabled(true);
                                    }
                                } else {
                                    mediaPlayerStatus = 0;
                                    playMp3();
                                }
                                break;
                            default:
                                break;
                        }
                    } else {
                        ToastUtils.showShort("请授权！");
                    }
                });
    }

    private void pauseMp3() {
        pbBg.setProgress(0);
        if (myMediaPlayer != null && myMediaPlayer.isPlaying()) {
            myMediaPlayer.pause();
            mediaPlayerStatus = 2;
        }
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
            } catch (Exception e) {
//                e.printStackTrace();
                LogUtils.w("无法停止visualizer！");
            }
        }
        switchButton.setCheckedImmediatelyNoEvent(false);
    }


    private void stopMp3() {
        pbBg.setProgress(0);
        if (myMediaPlayer != null) {
            if (myMediaPlayer.isPlaying()) {
                myMediaPlayer.stop();
            }
            myMediaPlayer.reset();
            myMediaPlayer.release();
            mediaPlayerStatus = 0;
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
                currBgLength = (int) (millisUntilFinished / 1000);
                int lefts = (int) (millisUntilFinished / 1000);
                tvBGDuration.setText(getFormatedLenght(lefts));
            }

            @Override
            public void onFinish() {
                tvBGDuration.setText(getFormatedLenght(0));
                currBgLength = (int) (FileUtils.getFileLength(base + Config.bgFilePCM) / 88200);
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

    private int factor = 5, currFactor = 0;
    private long playedOffsetMills;

    @Override
    public void startVoiceRecord() {
        RecorderUtil.getInstance().startRecording(MediaRecorder.AudioSource.MIC, base + Config.tempMicFileName, true, new RecorderUtil.OnVolumeChangeListener() {
            @Override
            public void onVolumeChanged(int readSize, double volume) {
//                if (playedOffsetMills == 0) {
//                    playedOffsetMills = System.currentTimeMillis() - playedStartMills;
////                    LogUtils.e("playedOffsetMills="+playedOffsetMills);
//                    if (myMediaPlayer != null && myMediaPlayer.isPlaying()) {
////                        long playedOffsetMills = myMediaPlayer.getCurrentPosition();
//                        byte[] bytes = new byte[(int) ((playedOffsetMills / 1000f) * 88200) - readSize];
//                        RecordBgMusicUtil.getInstance().appendMusic(base + Config.tempMicFileName, new byte[ConvertUtil.getInstance().toShortArray(bytes).length * 2], -1, true);
//                    }
//                }
//                LogUtils.d("RecorderUtil.getInstance().isReallyRecord()=" + RecorderUtil.getInstance().isReallyRecord());
                if (RecorderUtil.getInstance().isReallyRecord()) {
                    pbMic.setProgress((int) volume);
                } else {
                    pbMic.setProgress(0);
                }
                if (!isPlaying) {
                    byte[] bytes = new byte[readSize];
                    RecordBgMusicUtil.getInstance().appendMusic(base + Config.tempBgFileName, bytes, currVolume, true);
                } else {
//                    LogUtils.d("Config.bgFilePCM=" + Config.bgFilePCM);
                    RecordBgMusicUtil.getInstance().appendMusic(base + Config.bgFilePCM, base + Config.tempBgFileName, FileUtils.getFileLength(base + Config.tempBgFileName), readSize, currVolume, true);
                }
                try {
                    int bgVolume = -1;
                    if (!Config.isHeadsetIn && RecorderUtil.getInstance().isReallyRecord()) {
                        bgVolume = 0;
                    }
                    byte[] bytesBg = RecordBgMusicUtil.getInstance().readSDFile(base + Config.tempBgFileName, FileUtils.getFileLength(base + Config.tempBgFileName) - readSize, readSize, bgVolume);
                    byte[] bytesMic = RecordBgMusicUtil.getInstance().readSDFile(base + Config.tempMicFileName, FileUtils.getFileLength(base + Config.tempMicFileName) - readSize, readSize, -1);
                    byte[][] bytes = new byte[][]{bytesBg, bytesMic};


                    byte[] result = MixUtil.getInstance().averageMix(bytes);
                    double volumeMixed = CutActivity.calculateVolume(result);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            currFactor = currFactor % factor;
                            if (currFactor == 0) {
                                tvDuration.setText(getFormatedLenght((int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200)));
                                cutView.addVolume(volumeMixed);
                            }
                            currFactor++;
                        }
                    });
                    RecordBgMusicUtil.getInstance().writeAudioDataToFile(base + "/mix.pcm", result, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                recordingTime += 1000;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        tvDuration.setText(getFormatedLenght(recordingTime / 1000));
//                    }
//                });
//                Thread.sleep(needSleepBg);
            }
        });
    }


    @Override
    public void stopVoiceRecord() {
        RecorderUtil.getInstance().stopRecording();
    }


    private String getFormatedLenght(int length) {
        int minutes = length / 60;
        int seconds = length % 60;
        return (minutes < 10 ? "0" + minutes : String.valueOf(minutes)) + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
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
    protected void onResume() {
        super.onResume();
        if (tvDuration != null) {
            tvDuration.setText(getFormatedLenght((int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200)));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteTempFiles();
        setPlaying(false);
        setRecordStatus(RecordStatus.NONE);
        unregisterReceiver(mHeadsetPlugUnplugBroadcastReceiver);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3 && resultCode == 3) {
            rerecord();
        } else if (requestCode == 2 && resultCode == 2) {
            rerecord();
        } else if (requestCode == 1 && resultCode == RESULT_OK) {
            initBgMusic();
        }
    }

}

