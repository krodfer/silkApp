package com.example.ufabcirco.ui.custom;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

public class ColoredUnderlineSpan extends ReplacementSpan {
    private final int underlineColor;
    private final float underlineHeight;

    public ColoredUnderlineSpan(int color, float height) {
        this.underlineColor = color;
        this.underlineHeight = height;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end));
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        RectF rect = new RectF(x, bottom - underlineHeight, x + measureText(paint, text, start, end), bottom);
        int originalColor = paint.getColor();
        paint.setColor(underlineColor);
        canvas.drawRect(rect, paint);
        paint.setColor(originalColor);
        canvas.drawText(text, start, end, x-5, y+5, paint);
    }

    private float measureText(Paint paint, CharSequence text, int start, int end) {
        return paint.measureText(text, start, end);
    }
}