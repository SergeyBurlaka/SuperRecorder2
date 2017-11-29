package com.yibogame.superrecorder;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.blankj.utilcode.util.ToastUtils;
import com.jakewharton.rxbinding2.view.RxView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author tanyi
 * @date 2017/11/27
 */

public class PCMPlayerActivity extends BaseActivity {

    private Bundle bundle;
    private String base = Environment.getExternalStorageDirectory().getPath();

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

        RxView.clicks(findViewById(R.id.btn_play_voice))
                .subscribe(o -> {
                    PCMPlayer pcmPlayer = new PCMPlayer(bundle.getInt("voiceSampleRateInHz"),
                            bundle.getInt("voiceChannelConfig"),
                            bundle.getInt("voiceAudioFormat"));
                    String tempPath = base + "/temp_mic.pcm";
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pcmPlayer.write(readSDFile(tempPath));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                });
        RxView.clicks(findViewById(R.id.btn_play_bg))
                .subscribe(o -> {
                    PCMPlayer pcmPlayer = new PCMPlayer(bundle.getInt("bgSampleRateInHz"),
                            bundle.getInt("bgChannelConfig"),
                            bundle.getInt("bgAudioFormat"));
                    String tempPath = base + "/temp_bg.pcm";
                    pcmPlayer.write(readSDFile(tempPath));
                });
    }

    /***
     * 读取文件为byte[]
     * @param fileName
     * @return
     * @throws IOException
     */
    public byte[] readSDFile(String fileName) throws IOException {
        byte[] bytesForReturn = null;
        byte[] bytes = new byte[1024];
        ByteArrayOutputStream arrayOutputStream = null;
        try {
            FileInputStream inputStream = new FileInputStream(fileName);

            arrayOutputStream = new ByteArrayOutputStream();
            while (inputStream.read(bytes) != -1) {
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
}
