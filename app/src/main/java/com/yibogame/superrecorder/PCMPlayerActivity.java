package com.yibogame.superrecorder;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.widget.EditText;

import com.blankj.utilcode.util.ToastUtils;
import com.jakewharton.rxbinding2.view.RxView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ShortBuffer;

/**
 * @author tanyi
 * @date 2017/11/27
 */

public class PCMPlayerActivity extends BaseActivity {

    private Bundle bundle;
    private String base = Environment.getExternalStorageDirectory().getPath();
    private float mDB = 0.5f;
    private boolean isPlaying = false;
    PCMPlayer pcmPlayer;

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
                    pcmPlayer = new PCMPlayer(0,0,0);
                    String tempPath = base + Config.tempMicFileName;
                    isPlaying = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pcmPlayer.write(readSDFile(tempPath, pcmPlayer.getBufferSize(), -1));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                });
        RxView.clicks(findViewById(R.id.btn_play_bg))
                .subscribe(o -> {
                    stopPlay();
                    pcmPlayer = new PCMPlayer(0,0,0);
                    String tempPath = base + Config.tempBgFileName;
                    isPlaying = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pcmPlayer.write(readSDFile(tempPath, pcmPlayer.getBufferSize(), mDB));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
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
            while (isPlaying && inputStream.read(bytes) != -1) {
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

    private void stopPlay(){
        if (pcmPlayer != null) {
            pcmPlayer.destoryPlay();
            pcmPlayer = null;
        }
    }
}
