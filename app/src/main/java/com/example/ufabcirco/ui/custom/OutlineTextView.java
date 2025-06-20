package com.example.ufabcirco.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class OutlineTextView extends AppCompatTextView {

    private int outlineColor = Color.TRANSPARENT;
    private float outlineWidth = 0;

    public OutlineTextView(@NonNull Context context) {
        super(context);
    }

    public OutlineTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OutlineTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOutlineColor(int color) {
        this.outlineColor = color;
        invalidate();
    }

    public void setOutlineWidth(float width) {
        this.outlineWidth = width;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (outlineWidth > 0) {
            Paint paint = getPaint();
            int currentTextColor = getCurrentTextColor();

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(outlineWidth);
            setTextColor(outlineColor);
            super.onDraw(canvas);

            paint.setStyle(Paint.Style.FILL);
            setTextColor(currentTextColor);
        }
        super.onDraw(canvas);
    }
}