package com.yibogame.superrecorder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.text.TextUtils;
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
import com.yibogame.superrecorder.interfaces.IBufferDataChangeInterface;
import com.yibogame.superrecorder.interfaces.IOnRecordingListener;
import com.yibogame.superrecorder.interfaces.IRecordListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private boolean isVoiceRecording = false, isBgRecording = false;

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
                        default:
                            break;
                    }

                });
        //重录按钮
        RxView.clicks(findViewById(R.id.ctv_record))
                .subscribe(o -> {
                    stopMp3();
                    stopTimer();
                    stopVoiceRecord();
                    stopRecordBg();
                });

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

        RxView.clicks(findViewById(R.id.tv_change_bg))
                .subscribe(o -> {
                    Intent intent = new Intent(RecordActivity.this, PCMPlayerActivity.class);
                    Bundle bundle = new Bundle();
                    if (mRecorderVoice != null) {
                        bundle.putInt("voiceSampleRateInHz", mRecorderVoice.getSampleRateInHz());
                        bundle.putInt("voiceChannelConfig", mRecorderVoice.getChannelConfig());
                        bundle.putInt("voiceAudioFormat", mRecorderVoice.getAudioFormat());
                        bundle.putInt("voiceBufferSizeInBytes", mRecorderVoice.getBufferSizeInBytes());
                    }
                    if (mRecorderBg != null) {
                        bundle.putInt("bgSampleRateInHz", mRecorderBg.getSampleRateInHz());
                        bundle.putInt("bgChannelConfig", mRecorderBg.getChannelConfig());
                        bundle.putInt("bgAudioFormat", mRecorderBg.getAudioFormat());
                        bundle.putInt("bgBufferSizeInBytes", mRecorderBg.getBufferSizeInBytes());
                    }
                    intent.putExtra("bundle", bundle);
                    startActivity(intent);
                });
    }

    String base = Environment.getExternalStorageDirectory().getPath();

    private void startVoiceRecord() {
//        String tempPath = base + File.separator + "temp_mic.pcm";
//        File file = new File(tempPath);
//        if (file.exists()) {
//            boolean isDeleteSuccess = file.delete();
//            LogUtils.d("成功删除之前的temp文件！");
//        }
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        audio.setMicrophoneMute(false);
        rxPermissions.request(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> { // will emit 2 Permission objects
                            if (granted) {
                                // `permission.name` is granted !
                                startRecord(IRecordListener.TYPE_VOICE);
                                isVoiceRecording = true;
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
//        mRecorderVoice.stop();//继续录音，让它录制静音，只为了保证两个长度一样。
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        audio.setMicrophoneMute(true);
    }

    private void stopVoiceRecord() {
        isVoiceRecording = false;
        recordStatus = RECORD_STATUS_NONE;
        ctvRecordPause.setDrawableTop(R.mipmap.ic_recorder);
        ctvRecordPause.setText("录音");
        if (mRecorderVoice != null) {
            mRecorderVoice.stop();
        }
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        audio.setMicrophoneMute(false);
    }

    private void goOnVoiceRecord() {
        recordStatus = RECORD_STATUS_RECORDING;
        ctvRecordPause.setDrawableTop(R.mipmap.ic_recorder);
        ctvRecordPause.setText("暂停");
//        startVoiceRecord();
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        audio.setMicrophoneMute(false);
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
        if (currMusicVolume != -1) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currMusicVolume, 0);
        }
        if (myMediaPlayer != null) {
            myMediaPlayer.seekTo(myMediaPlayerCurrentPosition);
            myMediaPlayer.start();
            isBgRecording = true;
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
//                    stopMp3();
                    bgLength = 52;
                    currBgLength = bgLength;
//                    if (visualizer != null) {
//                        try {
//                            visualizer.setEnabled(false);
//                        } catch (Exception e) {
//                            visualizer.release();
//                        }
//                    }
//                    stopTimer();
//                    playMp3();
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
            isBgRecording = true;
            visualizer.setEnabled(true);
            startTimer();
            startRecord(IRecordListener.TYPE_BG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void stopRecordBg() {
        if (mRecorderBg != null) {
            mRecorderBg.stop();
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
        isBgRecording = false;
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

    /**
     * 此方法为android程序写入sd文件文件，用到了android-annotation的支持库@
     *
     * @param buffer   写入文件的内容
     * @param folder   保存文件的文件夹名称,如log；可为null，默认保存在sd卡根目录
     * @param fileName 文件名称，默认app_log.txt
     * @param append   是否追加写入，true为追加写入，false为重写文件
     * @param autoLine 针对追加模式，true为增加时换行，false为增加时不换行
     */
    public synchronized static void writeFileToSDCard(@NonNull final byte[] buffer, @Nullable final String folder,
                                                      @Nullable final String fileName, final boolean append, final boolean autoLine) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        String folderPath = "";
        if (sdCardExist) {
            //TextUtils为android自带的帮助类
            if (TextUtils.isEmpty(folder)) {
                //如果folder为空，则直接保存在sd卡的根目录
                folderPath = Environment.getExternalStorageDirectory()
                        + File.separator;
            } else {
//                folderPath = Environment.getExternalStorageDirectory()
//                        + File.separator + folder + File.separator;
                folderPath = folder + File.separator;
            }
        } else {
            return;
        }

        File fileDir = new File(folderPath);
        if (!fileDir.exists()) {
            if (!fileDir.mkdirs()) {
                return;
            }
        }
        File file;
        //判断文件名是否为空
        if (TextUtils.isEmpty(fileName)) {
            file = new File(folderPath + "app_log.txt");
        } else {
            file = new File(folderPath + fileName);
//            ToastUtils.showLong("file.path=" + file.getAbsolutePath());
        }
        RandomAccessFile raf = null;
        FileOutputStream out = null;
        try {
            if (append) {
                //如果为追加则在原来的基础上继续写文件
                raf = new RandomAccessFile(file, "rw");
                raf.seek(file.length());
                raf.write(buffer);
                if (autoLine) {
                    raf.write("\n".getBytes());
                }
            } else {
                //重写文件，覆盖掉原来的数据
                out = new FileOutputStream(file);
                out.write(buffer);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//            }
//        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopMp3();
        stopTimer();
        stopVoiceRecord();
        stopRecordBg();
    }

    @Override
    public void initRecorder(int type) {

    }

    @Override
    public void startRecord(int type) {
        if (type == IRecordListener.TYPE_BG) {
            mRecorderBg = new Recorder(
                    MediaRecorder.AudioSource.DEFAULT,
                    512,
                    interfaceVoice);
            mRecorderBg.setOnRecordingListener(new IOnRecordingListener() {
                @Override
                public void onDataReceived(short[] mPCMBuffer, int readSize, double volume) {
                    writeFileToSDCard(toByteArray(mPCMBuffer), base, "temp_bg.pcm", true, false);
                }
            });
            mRecorderBg.startRecording();
            isBgRecording = true;
        } else if (type == IRecordListener.TYPE_VOICE) {
            mRecorderVoice = new Recorder(
                    MediaRecorder.AudioSource.MIC,
                    512,
                    interfaceVoice);
            mRecorderVoice.setOnPeriodInFramesChangeListener(new Recorder.OnPeriodInFramesChangeListener() {
                @Override
                public void onFrames(AudioRecord record) {
                    LogUtils.d("getNotificationMarkerPosition=" + record.getNotificationMarkerPosition() + ",getPositionNotificationPeriod=" + record.getPositionNotificationPeriod());
                }
            });
            mRecorderVoice.setOnRecordingListener(new IOnRecordingListener() {
                @Override
                public void onDataReceived(short[] mPCMBuffer, int readSize, double volume) {
                    LogUtils.d("onDataReceived of mic!");
                    writeFileToSDCard(toByteArray(mPCMBuffer), base, "temp_mic.pcm", true, false);
                }
            });
            mRecorderVoice.startRecording();
            isVoiceRecording = true;
        }
    }

    @Override
    public void pauseRecord(int type) {

    }

    @Override
    public void stopRecord(int type) {

    }

}

