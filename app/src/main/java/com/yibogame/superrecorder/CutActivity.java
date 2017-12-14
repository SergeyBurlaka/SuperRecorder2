package com.yibogame.superrecorder;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jakewharton.rxbinding2.view.RxView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by parcool on 2017/11/26.
 */

public class CutActivity extends BaseActivity {

    private String time;
    String base = Environment.getExternalStorageDirectory().getPath();
    private List<Double> list = new ArrayList<>();

    private CutContainer cutContainer = null;
    private MySeekBar mySeekBar;
    private MyHorizontalScrollView hsv;
    private TextView tvDuration;
    private CustomTextView ctvCut;
    private TextView tvStart, tvEnd;
    private CutView cutView;

    private boolean isCut = false;

    private PCMPlayer pcmPlayer;
    private int mPlayOffset, mPrimePlaySize;
    private boolean isPlaying = false;
    private Thread threadPlay;
    private byte[] data = null;
    private int fileLength;

    private TextView tvTip;
    private CustomTextView ctvFirst;
    private TextView tvTopTitle;
    private CustomTextView ctvPlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isCut = getIntent().getBooleanExtra("isCut", false);

        setContentView(R.layout.activity_cut);
        View vStatus = findViewById(R.id.v_status);
        vStatus.getLayoutParams().height = BarUtils.getStatusBarHeight();

        hsv = findViewById(R.id.hsv);
        tvDuration = findViewById(R.id.tv_duration);
        ctvCut = findViewById(R.id.ctv_cut);
        cutContainer = findViewById(R.id.ll_cut);
        tvStart = findViewById(R.id.tv_start);
        tvEnd = findViewById(R.id.tv_end);
        tvTip = findViewById(R.id.tv_tip);
        ctvFirst = findViewById(R.id.ctv_next);
        tvTopTitle = findViewById(R.id.tv_top_title);
        mySeekBar = findViewById(R.id.seekbar1);
        ctvPlay = findViewById(R.id.ctv_play);

        cutView = new CutView(this, ConvertUtils.dp2px(120), list);


        int seconds = (int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200f);
//        tvDuration.setText(getFormatedLenght(seconds));
        tvEnd.setText(getFormatedLenght(seconds));
        setUI();

        findViewById(R.id.iv_back).setOnClickListener(v -> {
            onBackPressed();
        });

        pcmPlayer = new PCMPlayer(0, 0, 0);
        mPrimePlaySize = pcmPlayer.getBufferSize() * 2;
    }

    private void setUI() {
        if (isCut) {
            initCut();
        } else {
            initListen();
        }
    }

    private float[] floatsForPlayPercent;

    private void initCut() {
        tvTopTitle.setText("裁剪");
        ctvFirst.setText("取消");
        ctvFirst.setDrawableTop(R.mipmap.ic_save);
        ctvFirst.setOnClickListener(v -> {
            stop();
            finish();
        });
        ctvCut.setText("裁剪");
        ctvCut.setDrawableTop(R.mipmap.ic_cut);
        ctvCut.setOnClickListener(v -> {
            stop();
            showDialog("裁剪中……");
            Observable.just(true)
                    .map(new Func1<Boolean, Boolean>() {
                        @Override
                        public Boolean call(Boolean aBoolean) {
                            float[] floats = cutView.getFromAndToPercent();
                            LogUtils.d("floats[0]=" + floats[0] + ",floats[1]=" + floats[1]);

                            byte[] bytes = readSDFile(base + "/mix.pcm");
                            if (bytes.length * floats[1] > bytes.length) {
                                ToastUtils.showShort("长度超出，请调整裁剪滑块的位置。");
                                return false;
                            }
                            short[] shorts = ConvertUtil.getInstance().toShortArray(bytes);
                            short[] shortStart = new short[(int) (shorts.length * floats[0])];
                            short[] shortEnd = new short[(int) (shorts.length - shorts.length * floats[1])];
                            System.arraycopy(shorts, 0, shortStart, 0, shortStart.length);
                            System.arraycopy(shorts, (int) (shorts.length * floats[1]), shortEnd, 0, shortEnd.length);

                            FileUtils.deleteFile(new File(base + "/mix.pcm"));
                            writeAudioDataToFile(base + "/mix.pcm", ConvertUtil.getInstance().toByteArray(shortStart), true);
                            writeAudioDataToFile(base + "/mix.pcm", ConvertUtil.getInstance().toByteArray(shortEnd), true);
                            return true;
                        }
                    })
                    .doOnNext(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            calculateVolume();
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            dismissProgressDialog();
                            if (aBoolean) {
                                ToastUtils.showShort("裁剪成功！");
                                finish();
                            } else {
                                ToastUtils.showShort("裁剪失败！");
                            }
                        }
                    });
        });
        tvTip.setVisibility(View.VISIBLE);
        Observable.just(true)
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        calculateVolume();
                        return null;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showDialog("请稍等...");
                    }
                })
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        cutView.setListVolume(list);
                        cutView.postInvalidate();
                        cutView.setScrollX(0);


                        hsv.measure(0, 0);

                        //增加整体布局监听
                        ViewTreeObserver vto = hsv.getViewTreeObserver();
                        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                hsv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                cutView.setHsvWidth(hsv.getMeasuredWidth());
                                float maxRange1 = (float) cutView.getMaxLength() / hsv.getMeasuredWidth() * 100f;
                                mySeekBar.setValue(0, maxRange1 > 100 ? 100 : maxRange1);
                                float[] range = mySeekBar.getCurrentRange();
                                cutView.setRange(range);
                                tvStart.setText(getFormatedLenght(0));
                                tvEnd.setText(getFormatedLenght((int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200)));
                                int seconds = (int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200f);
                                tvDuration.setText(getFormatedLenght(seconds));

                                //
                                mySeekBar.setOnRangeChangedListener(new MySeekBar.OnRangeChangedListener() {
                                    @Override
                                    public void onRangeChanged(MySeekBar view, float min, float max, boolean isFromUser) {

                                        float[] floats = cutView.getFromAndToPercent();
                                        float[] range = mySeekBar.getCurrentRange();
                                        LogUtils.d("onChanged!!!!!!!!floats="+floats[0]+","+floats[1]+",range="+range[0]+","+range[1]);
                                        byte[] bytes = readSDFile(base + "/mix.pcm");
                                        if (cutView.getMaxLength() == 0 || range[1] == 0 || hsv.getMeasuredWidth() == 0) {
                                            return;
                                        }
                                        if (range[1] / 100f * hsv.getMeasuredWidth() > cutView.getMaxLength()) {
                                            mySeekBar.setValue(range[0], (float) cutView.getMaxLength() / hsv.getMeasuredWidth() * 100);
                                            return;
                                        }
                                        range = mySeekBar.getCurrentRange();
                                        cutView.setRange(range);
                                        if (floats[1] == 0) {
                                            return;
                                        }
                                        tvStart.setText(getFormatedLenght((int) ((bytes.length * floats[0]) / 88200)));
                                        tvEnd.setText(getFormatedLenght((int) (bytes.length * floats[1] / 88200)));
                                    }
                                });
                            }
                        });

                        hsv.setScrollViewListener(new MyHorizontalScrollView.ScrollViewListener() {
                            @Override
                            public void onScrollChanged(MyHorizontalScrollView scrollView, int x, int y, int oldx, int oldy) {
                                float[] range = mySeekBar.getCurrentRange();
                                cutView.setRange(range);
                                cutView.setScrollX(x);
                                LogUtils.d("x=" + x + ",oldx=" + oldx);
                            }
                        });


                        hsv.removeAllViews();
                        hsv.addView(cutView);
                        cutContainer.setCutViewLength(cutView.getMaxLength());
                        for (int i = 0; i < cutContainer.getChildCount(); i++) {
                            if (cutContainer.getChildAt(i) instanceof PlayView) {
                                cutContainer.getChildAt(i).setVisibility(View.GONE);
                            } else {
                                cutContainer.getChildAt(i).setVisibility(View.VISIBLE);
                            }
                        }
                        dismissProgressDialog();

                        //裁剪页面的试听按钮点击事件
                        RxView.clicks(ctvPlay)
                                .map(o -> ctvPlay)
                                .throttleFirst(300, TimeUnit.MILLISECONDS)
                                .subscribe(o -> {
//                                    isCut = false;
//                                    setUI();
                                    floatsForPlayPercent = cutView.getFromAndToPercent();
//                                    LogUtils.d("floats[0]=" + floats[0] + ",floats[1]=" + floats[1]);

                                    byte[] bytes = readSDFile(base + "/mix.pcm");
                                    short[] shorts = ConvertUtil.getInstance().toShortArray(bytes);
                                    if (bytes.length * floatsForPlayPercent[1] > bytes.length) {
                                        ToastUtils.showShort("长度超出，请调整裁剪滑块的位置。");
                                        return;
                                    }

                                    mPlayOffset = (int) (shorts.length * floatsForPlayPercent[0])*2;
                                    if (!isPlaying) {
                                        isPlaying = true;
                                        o.setText("暂停");
                                        o.setDrawableTop(R.mipmap.ic_cut_pause);
                                        threadPlay = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                data = readSDFile(base + "/mix.pcm");
                                                while (isPlaying) {
                                                    floatsForPlayPercent = cutView.getFromAndToPercent();
                                                    pcmPlayer.write(data, mPlayOffset, mPrimePlaySize);
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            playPercent = mPlayOffset / (float) data.length;
                                                            if (mPlayOffset >= (int) (shorts.length * floatsForPlayPercent[1])*2) {
                                                                playPercent = 0;
                                                                isPlaying = false;
                                                                o.setText("试听");
                                                                o.setDrawableTop(R.mipmap.ic_cut_play);
                                                                mPlayOffset = 0;
                                                            }
                                                        }
                                                    });
                                                    mPlayOffset += mPrimePlaySize;
                                                }
                                            }
                                        });
                                        threadPlay.start();
                                    } else {
                                        isPlaying = false;
                                        o.setText("试听");
                                        o.setDrawableTop(R.mipmap.ic_cut_play);
                                    }


                                });
                    }

                    @Override
                    public void onError(Throwable e) {
                        dismissProgressDialog();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {

                    }
                });
    }

    private float playPercent;
    PlayView playView;

    private void initListen() {

        Observable.just(true)
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        tvTopTitle.setText("试听");
                        ctvFirst.setText("裁剪");
                        ctvFirst.setDrawableTop(R.mipmap.ic_cut);
                        ctvFirst.setOnClickListener(v -> {
                            stop();
                            CutActivity.this.isCut = true;
                            setUI();
                        });
                        ctvCut.setText("下一步");
                        ctvCut.setDrawableTop(R.mipmap.ic_save);
                        ctvCut.setOnClickListener(v -> {
                            stop();
                            ctvPlay.setText("试听");
                            ctvPlay.setDrawableTop(R.mipmap.ic_cut_play);
                            Intent intent = new Intent(CutActivity.this, SettingAudioActivity.class);
                            startActivityForResult(intent, 3);
                        });
                        tvTip.setVisibility(View.INVISIBLE);
                        findViewById(R.id.seekbar1).setVisibility(View.GONE);
                        playView = new PlayView(CutActivity.this);
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        playView.setLayoutParams(layoutParams);
//        cutContainer.removeAllViews();
                        for (int i = 0; i < cutContainer.getChildCount(); i++) {
                            cutContainer.getChildAt(i).setVisibility(View.GONE);
                        }
                        playView.setVisibility(View.VISIBLE);
                        cutContainer.addView(playView);
                        return aBoolean;
                    }
                })
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showDialog("请稍等");
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .map(new Func1<Boolean, Boolean>() {

                    @Override
                    public Boolean call(Boolean aBoolean) {
                        calculateVolume();
                        return aBoolean;
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


                        playView.setListVolume(list);
                        //
                        fileLength = getLength(base + "/mix.pcm");


                        tvStart.setText(getFormatedLenght(0));
                        tvEnd.setText(getFormatedLenght((int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200f)));
                        int seconds = (int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200f);
                        playPercent = 0;
                        mPlayOffset = 0;
                        tvDuration.setText(getFormatedLenght((int) (seconds * playPercent)));
                        RxView.clicks(ctvPlay)
                                .map(o -> ctvPlay)
                                .throttleFirst(300, TimeUnit.MILLISECONDS)
                                .subscribe(o -> {
                                    if (!isPlaying) {
                                        isPlaying = true;
                                        o.setText("暂停");
                                        o.setDrawableTop(R.mipmap.ic_cut_pause);
                                        threadPlay = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                data = readSDFile(base + "/mix.pcm");
                                                while (isPlaying) {
                                                    pcmPlayer.write(data, mPlayOffset, mPrimePlaySize);
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            playPercent = mPlayOffset / (float) data.length;
                                                            playView.setPlayPercent(playPercent);
                                                            tvDuration.setText(getFormatedLenght((int) (seconds * playView.getPlayPercent())));
                                                            if (mPlayOffset >= data.length) {
                                                                playPercent = 0;
                                                                playView.setPlayPercent(playPercent);
                                                                isPlaying = false;
                                                                o.setText("试听");
                                                                o.setDrawableTop(R.mipmap.ic_cut_play);
                                                                mPlayOffset = 0;
                                                            }
                                                        }
                                                    });
                                                    mPlayOffset += mPrimePlaySize;
                                                }
                                            }
                                        });
                                        threadPlay.start();
                                    } else {
                                        isPlaying = false;
                                        o.setText("试听");
                                        o.setDrawableTop(R.mipmap.ic_cut_play);
                                    }
                                });
                    }
                });

    }


    private String getFormatedLenght(int length) {
        int minutes = length / 60;
        int seconds = length % 60;
        return (minutes < 10 ? "0" + minutes : String.valueOf(minutes)) + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }

    private void calculateVolume() {
        list.clear();
        byte[] bytes = readSDFile(base + "/mix.pcm");
        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 100;
        bufferSize = 88200 / 4;
        int offset = 0;
        long v = 0;
        byte[] bytesTemp = new byte[bufferSize];

        while (offset + bufferSize < bytes.length) {
            System.arraycopy(bytes, offset, bytesTemp, 0, bufferSize);
//            LogUtils.d("length=" + bytesTemp.length);
//            v = 0;
//            for (int j = 0; j < bytesTemp.length; j++) {
//                v += bytesTemp[j] * bytesTemp[j];
//            }
//            double mean = v / (double) bufferSize;
//            double volume = 10 * Math.log10(mean);
            list.add(calculateVolume(bytesTemp));
            offset += bufferSize;
        }
    }

    public static double calculateVolume(byte[] buffer) {
        double sumVolume = 0.0;
        double avgVolume = 0.0;
        double volume = 0.0;
        for (int i = 0; i < buffer.length; i += 2) {
            int v1 = buffer[i] & 0xFF;
            int v2 = buffer[i + 1] & 0xFF;
            int temp = v1 + (v2 << 8);// 小端
            if (temp >= 0x8000) {
                temp = 0xffff - temp;
            }
            sumVolume += Math.abs(temp);
        }
        avgVolume = sumVolume / buffer.length / 2;
        volume = Math.log10(1 + avgVolume) * 10;
        return volume;
    }


    private double calcDecibelLevel(short[] buffer) {
        double sum = 0;
        for (short rawSample : buffer) {
            double sample = rawSample / 32768.0;
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / buffer.length);
        return 20 * Math.log10(rms);
    }

    /**
     * Computes the RMS volume of a group of signal sizes ranging from -1 to 1.
     */
    public static double volumeRMS(double[] raw) {
        double sum = 0d;
        if (raw.length == 0) {
            return sum;
        } else {
            for (int ii = 0; ii < raw.length; ii++) {
                sum += raw[ii];
            }
        }
        double average = sum / raw.length;

        double sumMeanSquare = 0d;
        for (int ii = 0; ii < raw.length; ii++) {
            sumMeanSquare += Math.pow(raw[ii] - average, 2d);
        }
        double averageMeanSquare = sumMeanSquare / raw.length;
        double rootMeanSquare = Math.sqrt(averageMeanSquare);

        return rootMeanSquare;
    }

    public byte[] readSDFile(String fileName) {
        byte[] bytes = new byte[0];
        int fileLength = (int) getFileLength(fileName);
        if (fileLength == -1) {
            finish();
            ToastUtils.showShort("暂无录音文件！");
            return new byte[0];
        }
        bytes = new byte[fileLength];
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

    public long getFileLength(String fileName) {
        return FileUtils.getFileLength(fileName);
    }

    public void writeAudioDataToFile(String filePath, byte[] bytes, boolean append) {
        if (bytes == null) {
            return;
        }
        // Write the output audio in byte
//        short sData[] = new short[bytes.length / 2];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath, append);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (os == null) {
            return;
        }
//        byte bData[] = short2byte(sData);
        try {
//            os.write(bData, bytes);
            os.write(bytes, 0, bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getLength(String fileName) {
        int length = 0;
        length = (int) (FileUtils.getFileLength(fileName) / 88200);
        return length;
    }

    private void stop() {
        if (threadPlay != null && threadPlay.isAlive() && !threadPlay.isInterrupted()) {
            isPlaying = false;
            threadPlay.interrupt();
            ctvPlay.setText("试听");
            ctvPlay.setDrawableTop(R.mipmap.ic_cut_play);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3 && resultCode == 3) {
            setResult(2);
            finish();
        }
    }
}
