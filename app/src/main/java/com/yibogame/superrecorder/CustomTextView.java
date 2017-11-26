package com.yibogame.superrecorder;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

/**
 * Created by parcool on 2017/11/26.
 */

public class CustomTextView extends AppCompatTextView {
    public CustomTextView(Context context) {
        super(context);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    void initAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray attributeArray = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.CustomTextView);

            Drawable drawableLeft = null;
            Drawable drawableRight = null;
            Drawable drawableBottom = null;
            Drawable drawableTop = null;
            int size = attributeArray.getDimensionPixelSize(R.styleable.CustomTextView_drawableTopCompatSize, 48);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawableLeft = attributeArray.getDrawable(R.styleable.CustomTextView_drawableLeftCompat);
                drawableRight = attributeArray.getDrawable(R.styleable.CustomTextView_drawableRightCompat);
                drawableBottom = attributeArray.getDrawable(R.styleable.CustomTextView_drawableBottomCompat);
                drawableTop = attributeArray.getDrawable(R.styleable.CustomTextView_drawableTopCompat);
            } else {
                final int drawableLeftId = attributeArray.getResourceId(R.styleable.CustomTextView_drawableLeftCompat, -1);
                final int drawableRightId = attributeArray.getResourceId(R.styleable.CustomTextView_drawableRightCompat, -1);
                final int drawableBottomId = attributeArray.getResourceId(R.styleable.CustomTextView_drawableBottomCompat, -1);
                final int drawableTopId = attributeArray.getResourceId(R.styleable.CustomTextView_drawableTopCompat, -1);


                if (drawableLeftId != -1) {
                    drawableLeft = AppCompatResources.getDrawable(context, drawableLeftId);

                }
                if (drawableRightId != -1) {
                    drawableRight = AppCompatResources.getDrawable(context, drawableRightId);
                }
                if (drawableBottomId != -1) {
                    drawableBottom = AppCompatResources.getDrawable(context, drawableBottomId);
                }
                if (drawableTopId != -1) {
                    drawableTop = AppCompatResources.getDrawable(context, drawableTopId);
                }

            }
            if (drawableLeft != null) {
                drawableLeft.setBounds(0, 0, size, size);
            }
            if (drawableRight != null) {
                drawableRight.setBounds(0, 0, size, size);
            }
            if (drawableBottom != null) {
                drawableBottom.setBounds(0, 0, size, size);
            }
            if (drawableTop != null) {
                drawableTop.setBounds(0, 0, size, size);
            }
            setCompoundDrawables(drawableLeft, drawableTop, drawableRight, drawableBottom);
            attributeArray.recycle();
        }
    }

    public void setDrawableLeft(int resId) {
        Drawable[] drawables = getCompoundDrawables();
        if (resId == -1) {
            drawables[0] = null;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    drawables[0] = getResources().getDrawable(resId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                drawables[0] = AppCompatResources.getDrawable(getContext(), resId);
            }
        }
        setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3]);
    }

    public void setDrawableRight(int resId) {
        Drawable[] drawables = getCompoundDrawables();
        if (resId == -1) {
            drawables[2] = null;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    drawables[2] = getResources().getDrawable(resId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                drawables[2] = AppCompatResources.getDrawable(getContext(), resId);
            }
        }
        setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3]);
    }

    public void setDrawableTop(int resId) {
        Drawable[] drawables = getCompoundDrawables();
        int size = 0;

        if (drawables[1] != null) {
            size = drawables[1].getBounds().right;
        }
        if (resId == -1) {
            drawables[1] = null;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    drawables[1] = getResources().getDrawable(resId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                drawables[1] = AppCompatResources.getDrawable(getContext(), resId);
            }
        }
        if (size != 0 && drawables[1] != null) {
            drawables[1].setBounds(0, 0, size, size);
        }
        setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
    }

    public void setDrawableBottom(int resId) {
        Drawable[] drawables = getCompoundDrawables();
        if (resId == -1) {
            drawables[3] = null;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    drawables[3] = getResources().getDrawable(resId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                drawables[3] = AppCompatResources.getDrawable(getContext(), resId);
            }
        }
        setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3]);
    }

}
