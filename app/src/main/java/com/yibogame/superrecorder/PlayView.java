package com.yibogame.superrecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.blankj.utilcode.util.LogUtils;

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

    private static final int lineWidth = 5;

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
        invalidate();
    }

    public float getPlayPercent() {
        return playPercent;
    }


    public void setPlayPercent(float playPercent) {
        int offset = (int) (playPercent * (widthPerLine + space) * listVolume.size());
        int x = offset / getMeasuredWidth();
        this.playPercent = playPercent;
        if (x >= 1) {
            int left = x * getMeasuredWidth() * -1;
            LogUtils.d("left = " + left);
            ((RelativeLayout.LayoutParams) this.getLayoutParams()).leftMargin = left;
        }
        if (playPercent <= 0) {
            ((RelativeLayout.LayoutParams) this.getLayoutParams()).leftMargin = 0;
        }
        invalidate();
        requestLayout();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(colorDefault);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMySize(200, widthMeasureSpec);
        int height = getMySize(0, heightMeasureSpec);


        setMeasuredDimension(width, height);
    }

    private int getMySize(int defaultSize, int measureSpec) {
        int mySize = defaultSize;

        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        switch (mode) {
            case MeasureSpec.UNSPECIFIED: {//如果没有指定大小，就设置为默认大小
                mySize = defaultSize;
                break;
            }
            case MeasureSpec.AT_MOST: {//如果测量模式是最大取值为size
                //我们将大小取最大值,你也可以取其他值
                mySize = size;
                break;
            }
            case MeasureSpec.EXACTLY: {//如果是固定的大小，那就不要去改变它
                mySize = size;
                break;
            }
        }
        return mySize;
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
            canvas.drawRect(playPercent * (widthPerLine + space) * listVolume.size(), 0, playPercent * (widthPerLine + space) * listVolume.size() + lineWidth, getMeasuredHeight(), mPaint);
            if (offset < playPercent * (widthPerLine + space) * listVolume.size()) {
                mPaint.setColor(colorAccent);
            } else {
                mPaint.setColor(colorDefault);
            }
            //画音频线
            canvas.drawRect(offset, getMeasuredHeight() - height, offset + widthPerLine, getMeasuredHeight(), mPaint);
        }
    }
}
