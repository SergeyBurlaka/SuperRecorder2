package com.yibogame.superrecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * Created by tanyi on 2017/12/8.
 */

public class PlayView extends View {

    private List<Double> listVolume = null;
    private int max = 40;
    private int widthPerLine = 5, space = 2;
    private Paint mPaint;
    private float playPercent;

    private static final int lineWidth = 4;

    private int colorAccent = Color.parseColor("#c10e41");
    private int colorDefault = Color.parseColor("#b7bbc6");
    private int colorCursorLine = Color.parseColor("#c7db22");

    public PlayView(Context context) {
        super(context);
        init();
    }

    public PlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public List<Double> getListVolume() {
        return listVolume;
    }

    public void setListVolume(List<Double> listVolume) {
        this.listVolume = listVolume;
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(colorDefault);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (listVolume == null || listVolume.size() <= 0) {
            return;
        }
        for (int i = 0; i < listVolume.size(); i++) {
            int height = (int) (listVolume.get(i) / max * getMeasuredHeight());
            height = height > getMeasuredHeight() ? getMeasuredHeight() : height;
            int offset = i * (widthPerLine + space);
            //画时间线
            mPaint.setColor(colorCursorLine);
            canvas.drawRect(playPercent * getMeasuredWidth(), 0, offset + lineWidth, getMeasuredHeight(), mPaint);
            if (offset < playPercent * getMeasuredWidth()) {
                mPaint.setColor(colorDefault);
            } else {
                mPaint.setColor(colorAccent);
            }
            //画音频线
            canvas.drawRect(offset, getMeasuredHeight() - height, offset + widthPerLine, getMeasuredHeight(), mPaint);
        }
    }
}
