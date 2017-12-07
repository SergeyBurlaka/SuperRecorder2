package com.yibogame.superrecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parcool on 2017/12/3.
 */

public class CutView extends View {

    private List<Double> listVolume = null;
    private int max = 70;
    private int widthPerLine = 5, space = 2;
    private Paint mPaint;
    private int mHeight;
    private float[] range;
    private int scrollX;

    private int start, end, hsvWidth;

    public void setListVolume(List<Double> listVolume) {
        this.listVolume = listVolume;
    }

    public void addVolume(double volume) {
        this.listVolume.add(volume);
        requestLayout();
    }

    public void setRange(float[] range) {
        start = (int) ((range[0] / 100f * hsvWidth) + scrollX);
        end = (int) (range[1] / 100f * hsvWidth + scrollX);
        postInvalidate();
        this.range = range;
    }

    public float[] getFromAndToPercent() {
        float[] floats = new float[2];
        floats[0] = (float) start / getMeasuredWidth();
        floats[1] = (float) end / getMeasuredWidth();
        return floats;
    }

    @Override
    public void setScrollX(int scrollX) {
        if (range != null) {
            start = (int) ((range[0] / 100f * hsvWidth) + scrollX);
            end = (int) (range[1] / 100f * hsvWidth + scrollX);
            postInvalidate();
        }
        this.scrollX = scrollX;
    }

    public void setHsvWidth(int hsvWidth) {
        LogUtils.d("hsvWidth=" + hsvWidth);
        this.hsvWidth = hsvWidth;
    }

    private int colorAccent = Color.parseColor("#c10e41");
    private int colorDefault = Color.parseColor("#b7bbc6");

    public int getMaxLength() {
        return listVolume.size() * (widthPerLine + space);
    }

    public CutView(Context context, int height, List<Double> listVolume) {
        super(context);
        this.mHeight = height;
        this.listVolume = listVolume;

        initPaint();
    }

    public CutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mHeight = ConvertUtils.dp2px(46);
        this.listVolume = new ArrayList<>();
        initPaint();
    }

    public CutView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mHeight = ConvertUtils.dp2px(46);
        this.listVolume = new ArrayList<>();
        initPaint();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setColor(colorDefault);
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int width = getMySize(100, widthMeasureSpec);
//        int height = getMySize(100, heightMeasureSpec);

//        if (width < height) {
//            height = width;
//        } else {
//            width = height;
//        }
        int width = listVolume.size() * (widthPerLine + space);
        setMeasuredDimension(width, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < listVolume.size(); i++) {
            int height = (int) (listVolume.get(i) / max * getMeasuredHeight());
            height = height > getMeasuredHeight() ? getMeasuredHeight() : height;
            int offset = i * (widthPerLine + space);
//            if (start != 0) {
                if (offset < start || offset > end) {
                    mPaint.setColor(colorDefault);
                } else {
                    mPaint.setColor(colorAccent);
                }
//            }
            canvas.drawRect(offset, getMeasuredHeight() - height, offset + widthPerLine, getMeasuredHeight(), mPaint);
        }
    }


}
