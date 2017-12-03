package com.yibogame.superrecorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.blankj.utilcode.util.LogUtils;
import com.jaygoo.widget.RangeSeekBar;

/**
 * Created by parcool on 2017/12/3.
 */

public class CutContainer extends RelativeLayout {

//    private RangeSeekBar cut1, cut2;
//
//    public SeekBar getCut1() {
//        if (cut1 == null) {
//            init();
//        }
//        return cut1;
//    }
//
//    public SeekBar getCut2() {
//        if (cut2 == null) {
//            init();
//        }
//        return cut2;
//    }

    private int cutViewLength = 0;

    public void setCutViewLength(int cutViewLength) {
        this.cutViewLength = cutViewLength;
    }


    public CutContainer(Context context) {
        super(context);
    }

    public CutContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CutContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    @SuppressLint("ClickableViewAccessibility")
//    private void init() {
//        cut1 = findViewById(R.id.iv_cut1);
//        cut2 = findViewById(R.id.iv_cut2);
//    }


}
