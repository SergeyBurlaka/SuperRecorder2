package com.yibogame.superrecorder;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.EditText;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jakewharton.rxbinding2.view.RxView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.Arrays;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author tanyi
 * @date 2017/11/27
 */

public class PCMPlayerActivity extends BaseActivity {

    private Bundle bundle;
    private String base = Environment.getExternalStorageDirectory().getPath();
    private float mDB = 0.5f;
    private boolean isPlaying = false;
    PCMPlayer pcmPlayer, pcmPlayer2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcm_player);
        if (getIntent() == null) {
            ToastUtils.showShort("请传入bundle！");
            finish();
        }
        bundle = getIntent().getBundleExtra("bundle");
        if (bundle == null) {
            ToastUtils.showShort("请传入参数！");
            finish();
        }
        RxView.clicks(findViewById(R.id.bt_set_db))
                .subscribe(o -> {
                    EditText editText = findViewById(R.id.et_db);
                    mDB = Float.parseFloat(editText.getText().toString());
                });

        RxView.clicks(findViewById(R.id.btn_play_voice))
                .subscribe(o -> {
                    stopPlay();
                    pcmPlayer = new PCMPlayer(0, 0, 0);
                    String tempPath = base + Config.tempMicFileName;
                    isPlaying = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pcmPlayer.write(readSDFile(base + "/mix.pcm", pcmPlayer.getBufferSize(), -1));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                });
        RxView.clicks(findViewById(R.id.btn_play_bg))
                .subscribe(o -> {
                    stopPlay();
                    pcmPlayer2 = new PCMPlayer(0, 0, 0);
                    String tempPath = base + Config.tempBgFileName;
                    isPlaying = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pcmPlayer2.write(readSDFile(tempPath, pcmPlayer2.getBufferSize(), mDB));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                });

        Observable.just(true)
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        File[] files = new File[2];
                        String tempPath = base + Config.tempMicFileName;
                        String tempPath2 = base + Config.tempBgFileName;
                        files[0] = new File(tempPath);
                        files[1] = new File(tempPath2);
                        try {
                            byte[] b1 = readSDFile(tempPath, 0, 1.0f);
                            byte[] b2 = readSDFile(tempPath2, 0, 1.0f);
                            int length = b1.length - b2.length;
                            byte[] blank = new byte[length];
                            writeAudioDataToFile(tempPath2,blank,true);
                            byte[] b3 = readSDFile(tempPath2, 0, 1.0f);
                            byte[][] bytes = new byte[][]{b1, b3};
                            byte[] result = averageMix(bytes);
                            LogUtils.d("b1=" + b1.length + ",b2=" + b2.length+",result="+result.length);
                            writeAudioDataToFile(base + "/mix.pcm", result, true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showDialog("合成中");
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        dismissProgressDialog();
                    }
                });
    }

    /***
     * 读取文件为byte[]
     * @param fileName
     * @return
     * @throws IOException
     */
    public byte[] readSDFile(String fileName, int bufferSizeInBytes, float db) throws IOException {
        byte[] bytesForReturn = null;
        byte[] bytes = new byte[512];
        ByteArrayOutputStream arrayOutputStream = null;
        int byteread = 0;
        try {
            FileInputStream inputStream = new FileInputStream(fileName);
//            while ((byteread = inputStream.read(bytes)) != -1) {
//                System.out.write(bytes, 0, byteread);
//                System.out.flush();
//            }
            arrayOutputStream = new ByteArrayOutputStream();
            while (inputStream.read(bytes) != -1) {
                if (db != -1) {
                    bytes = VolumeUtil.getInstance().resetVolume(bytes, db);
                }
                arrayOutputStream.write(bytes, 0, bytes.length);

            }
            bytesForReturn = arrayOutputStream.toByteArray();
            inputStream.close();
            arrayOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytesForReturn;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPlaying = false;
        stopPlay();
    }

    private void stopPlay() {
        if (pcmPlayer != null) {
            pcmPlayer.destoryPlay();
            pcmPlayer = null;
        }
    }

    public byte[] mixAudios(File[] rawAudioFiles) {
        byte[] mixBytes = new byte[0];
        final int fileSize = rawAudioFiles.length;

        FileInputStream[] audioFileStreams = new FileInputStream[fileSize];
        File audioFile = null;

        FileInputStream inputStream;
        byte[][] allAudioBytes = new byte[fileSize][];
        boolean[] streamDoneArray = new boolean[fileSize];
        byte[] buffer = new byte[512];
        int offset;

        try {

            for (int fileIndex = 0; fileIndex < fileSize; ++fileIndex) {
                audioFile = rawAudioFiles[fileIndex];
                audioFileStreams[fileIndex] = new FileInputStream(audioFile);
            }

            while (true) {

                for (int streamIndex = 0; streamIndex < fileSize; ++streamIndex) {

                    inputStream = audioFileStreams[streamIndex];
                    if (!streamDoneArray[streamIndex] && (offset = inputStream.read(buffer)) != -1) {
                        allAudioBytes[streamIndex] = Arrays.copyOf(buffer, buffer.length);
                    } else {
                        streamDoneArray[streamIndex] = true;
                        allAudioBytes[streamIndex] = new byte[512];
                    }
                }

                mixBytes = averageMix(allAudioBytes);

                //mixBytes 就是混合后的数据

                boolean done = true;
                for (boolean streamEnd : streamDoneArray) {
                    if (!streamEnd) {
                        done = false;
                    }
                }

                if (done) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
//            if(mOnAudioMixListener != null)
//                mOnAudioMixListener.onMixError(1);
        } finally {
            try {
                for (FileInputStream in : audioFileStreams) {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mixBytes;
    }


    /**
     * 每一行是一个音频的数据
     */
    private byte[] averageMix(byte[][] bMulRoadAudioes) {

        if (bMulRoadAudioes == null || bMulRoadAudioes.length == 0) {
            return null;
        }
        byte[] realMixAudio = bMulRoadAudioes[0];

        if (bMulRoadAudioes.length == 1) {
            return realMixAudio;
        }
        for (int rw = 0; rw < bMulRoadAudioes.length; ++rw) {
            if (bMulRoadAudioes[rw].length != realMixAudio.length) {
                Log.e("app", "column of the road of audio + " + rw + " is diffrent.");
//                return null;
                break;
            }
        }

        int row = bMulRoadAudioes.length;
        int coloum = realMixAudio.length / 2;
        short[][] sMulRoadAudioes = new short[row][coloum];
        for (int r = 0; r < row; ++r) {
            for (int c = 0; c < coloum; ++c) {
                sMulRoadAudioes[r][c] = (short) ((bMulRoadAudioes[r][c * 2] & 0xff) | (bMulRoadAudioes[r][c * 2 + 1] & 0xff) << 8);
            }
        }
        short[] sMixAudio = new short[coloum];
        int mixVal;
        int sr = 0;
        for (int sc = 0; sc < coloum; ++sc) {
            mixVal = 0;
            sr = 0;
            for (; sr < row; ++sr) {
                mixVal += sMulRoadAudioes[sr][sc];
            }
            sMixAudio[sc] = (short) (mixVal / row);
        }
        for (sr = 0; sr < coloum; ++sr) {
            realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
            realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
        }
        return realMixAudio;
    }

    private void writeAudioDataToFile(String filePath, byte[] bytes, boolean append) {
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
