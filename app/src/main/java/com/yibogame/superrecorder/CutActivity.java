package com.yibogame.superrecorder;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;

import com.blankj.utilcode.util.BarUtils;

/**
 * Created by parcool on 2017/11/26.
 */

public class CutActivity extends BaseActivity {

    private String time;
    String base = Environment.getExternalStorageDirectory().getPath();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cut);
        View vStatus = findViewById(R.id.v_status);
        vStatus.getLayoutParams().height = BarUtils.getStatusBarHeight();
    }




}
