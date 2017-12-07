package com.yibogame.superrecorder;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cut);
        View vStatus = findViewById(R.id.v_status);
        vStatus.getLayoutParams().height = BarUtils.getStatusBarHeight();

        tvDuration = findViewById(R.id.tv_duration);
        ctvCut = findViewById(R.id.ctv_cut);
        cutContainer = findViewById(R.id.ll_cut);
        tvStart = findViewById(R.id.tv_start);
        tvEnd = findViewById(R.id.tv_end);
        findViewById(R.id.ctv_next).setOnClickListener(v -> {
            finish();
        });
        findViewById(R.id.ctv_next1).setOnClickListener(v -> {
            Intent intent = new Intent(CutActivity.this, SettingAudioActivity.class);
            startActivity(intent);
        });


        cutView = new CutView(this, ConvertUtils.dp2px(120), list);
        ctvCut.setOnClickListener(v -> {
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
                            byte[] bytesStart = new byte[(int) (bytes.length * floats[0])];
                            byte[] bytesEnd = new byte[(int) (bytes.length - bytes.length * floats[1])];
                            System.arraycopy(bytes, 0, bytesStart, 0, bytesStart.length);
                            System.arraycopy(bytes, (int) (bytes.length * floats[1]), bytesEnd, 0, bytesEnd.length);

                            FileUtils.deleteFile(new File(base + "/mix.pcm"));
                            writeAudioDataToFile(base + "/mix.pcm", bytesStart, true);
                            writeAudioDataToFile(base + "/mix.pcm", bytesEnd, true);
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
//                                int seconds = (int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200f);
//                                tvDuration.setText(getFormatedLenght(seconds));
//                                tvEnd.setText(getFormatedLenght(seconds));
////                                cutContainer.setCutViewLength(cutView.getMaxLength());
////                                cutView.setListVolume(list);
////                                cutView.postInvalidate();
////                                cutView.setScrollX(0);
//                                ToastUtils.showShort("裁剪成功！");
//
//                                list.clear();
//                                init();
                            } else {
                                ToastUtils.showShort("裁剪失败！");
                            }
                        }
                    });
        });

        int seconds = (int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200f);
        tvDuration.setText(getFormatedLenght(seconds));
        tvEnd.setText(getFormatedLenght(seconds));

        init();


    }

    private void init() {
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
                        hsv = findViewById(R.id.hsv);
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

                        mySeekBar = findViewById(R.id.seekbar1);
                        mySeekBar.setOnRangeChangedListener(new MySeekBar.OnRangeChangedListener() {
                            @Override
                            public void onRangeChanged(MySeekBar view, float min, float max, boolean isFromUser) {
                                float[] floats = cutView.getFromAndToPercent();


                                float[] range = mySeekBar.getCurrentRange();
                                if (range[1] / 100f * hsv.getMeasuredWidth() > cutView.getMaxLength()) {
                                    mySeekBar.setValue(range[0], (float) cutView.getMaxLength() / hsv.getMeasuredWidth() * 100);
                                    return;
                                }
                                range = mySeekBar.getCurrentRange();
//                                LogUtils.d("min="+min+",max="+max+",range[0]="+range[0]+",range[1]="+range[1]);
                                cutView.setRange(range);
                                if (floats[1] == 0) {
                                    return;
                                }
                                byte[] bytes = readSDFile(base + "/mix.pcm");
                                tvStart.setText(getFormatedLenght((int) ((bytes.length * floats[0]) / 88200)));
                                tvEnd.setText(getFormatedLenght((int) (bytes.length * floats[1] / 88200)));
//                                LogUtils.d("(int) (bytes.length * floats[1] / 88200))="+(int) (bytes.length * floats[1] / 88200));
                            }
                        });
                        hsv.removeAllViews();
                        hsv.addView(cutView);
                        cutContainer.setCutViewLength(cutView.getMaxLength());
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
    }


    private String getFormatedLenght(int length) {
        int minutes = length / 60;
        int seconds = length % 60;
        return (minutes < 10 ? "0" + minutes : String.valueOf(minutes)) + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }

    private void calculateVolume() {
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
        try {
            int fileLength = (int) getFileLength(fileName);
            if (fileLength == -1) {
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

    public long getFileLength(String fileName) throws IOException {
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

}
