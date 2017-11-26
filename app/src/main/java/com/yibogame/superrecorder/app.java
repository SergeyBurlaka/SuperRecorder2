package com.yibogame.superrecorder;

import android.app.Application;

import com.blankj.utilcode.util.Utils;

/**
 * Created by parcool on 2017/11/25.
 */

public class app extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
    }
}
