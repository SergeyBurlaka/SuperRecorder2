package com.yibogame.superrecorder;

import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 *
 * @author tanyi
 * @date 2017/12/8
 */

public class ChoiceBgMusicActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice_bgm);

        findViewById(R.id.tv1).setOnClickListener(v -> {

        });
        findViewById(R.id.tv2).setOnClickListener(v -> {

        });
    }
}
