package com.yibogame.superrecorder;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.blankj.utilcode.util.LogUtils;

/**
 * @author tanyi
 * @date 2017/12/8
 */

public class ChoiceBgMusicActivity extends BaseActivity {


    String bgFileMp31 = "/bg_music_1.mp3";
    String bgFilePCM1 = "/bg_music_1.pcm";

    String bgFileMp32 = "/bg_music_2.mp3";
    String bgFilePCM2 = "/bg_music_2.pcm";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice_bgm);

        findViewById(R.id.tv1).setOnClickListener(v -> {
            if (!bgFileMp31.equals(Config.bgFileMp3)) {
                Config.bgFileMp3 = bgFileMp31;
                Config.bgFilePCM = bgFilePCM1;
                LogUtils.d("1Config.bgFileMp3="+Config.bgFileMp3+",Config.bgFilePCM="+Config.bgFilePCM);
                setResult(RESULT_OK);
            }
            finish();
        });
        findViewById(R.id.tv2).setOnClickListener(v -> {
            if (!bgFileMp32.equals(Config.bgFileMp3)) {
                Config.bgFileMp3 = bgFileMp32;
                Config.bgFilePCM = bgFilePCM2;
                LogUtils.d("2Config.bgFileMp3="+Config.bgFileMp3+",Config.bgFilePCM="+Config.bgFilePCM);
                setResult(RESULT_OK);
            }
            finish();
        });
    }
}
