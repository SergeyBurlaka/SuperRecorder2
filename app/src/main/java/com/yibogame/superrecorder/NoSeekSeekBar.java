package com.yibogame.superrecorder;

import android.content.Context;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by parcool on 2017/12/3.
 */

public class NoSeekSeekBar extends AppCompatSeekBar {
    public NoSeekSeekBar(Context context) {
        super(context);
    }

    public NoSeekSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoSeekSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        return false ;
    }

}
