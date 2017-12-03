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
import rx.android.schedulers.AndroidSchedulers;
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



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cut);
        View vStatus = findViewById(R.id.v_status);
        vStatus.getLayoutParams().height = BarUtils.getStatusBarHeight();

        tvDuration = findViewById(R.id.tv_duration);
        ctvCut = findViewById(R.id.ctv_cut);
        cutContainer = findViewById(R.id.ll_cut);
        findViewById(R.id.ctv_next).setOnClickListener(v -> {
            Intent intent = new Intent(CutActivity.this, SettingAudioActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.ctv_next1).setOnClickListener(v -> {
            Intent intent = new Intent(CutActivity.this, SettingAudioActivity.class);
            startActivity(intent);
        });


        CutView cutView = new CutView(this, ConvertUtils.dp2px(120), list);
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
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            dismissProgressDialog();
                            if (aBoolean) {
                                int seconds = (int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200f);
                                tvDuration.setText(getFormatedLenght(seconds));
                                calculateVolume();
                                cutContainer.setCutViewLength(cutView.getMaxLength());
                                cutView.setListVolume(list);
                                cutView.postInvalidate();
                                cutView.setScrollX(0);
                                ToastUtils.showShort("裁剪成功！");
                            }
                        }
                    });
        });

        int seconds = (int) (FileUtils.getFileLength(base + "/mix.pcm") / 88200f);
        tvDuration.setText(getFormatedLenght(seconds));
        calculateVolume();


        hsv = findViewById(R.id.hsv);
        hsv.measure(0, 0);

        //增加整体布局监听
        ViewTreeObserver vto = hsv.getViewTreeObserver();

        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                hsv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                cutView.setHsvWidth(hsv.getMeasuredWidth());
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
//                ToastUtils.showShort("min=" + min + ",max=" + max);
                float[] range = mySeekBar.getCurrentRange();
                cutView.setRange(range);
            }
        });
        mySeekBar.setValue(0, 20);
        hsv.addView(cutView);

//        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) cutContainer.getCut2().getLayoutParams();
//        layoutParams.leftMargin = cutView.getMaxLength() - ConvertUtils.dp2px(14);
//        cutContainer.measure(0, 0);
//        if (layoutParams.leftMargin > cutContainer.getMeasuredWidth()) {
//            layoutParams.leftMargin = cutContainer.getMeasuredWidth() - ConvertUtils.dp2px(14);
//        }
//        if (layoutParams.leftMargin < ConvertUtils.dp2px(14)) {
//            layoutParams.leftMargin = ConvertUtils.dp2px(14);
//        }

//        LogUtils.d("aa" + layoutParams.leftMargin + ",cutView.getMaxLength()=" + cutView.getMaxLength());
//        cutContainer.getCut2().setLayoutParams(layoutParams);
        cutContainer.setCutViewLength(cutView.getMaxLength());

    }

    private String getFormatedLenght(int length) {
        int minutes = length / 60;
        int seconds = length % 60;
        return (minutes < 10 ? "0" + minutes : String.valueOf(minutes)) + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }

    private void calculateVolume() {
        byte[] bytes = readSDFile(base + "/mix.pcm");
        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 100;
        bufferSize = 88200 / 2;
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

    private double calculateVolume(byte[] buffer) {
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
