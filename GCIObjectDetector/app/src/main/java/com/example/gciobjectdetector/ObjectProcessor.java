package com.example.gciobjectdetector;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;

import java.util.List;

public class ObjectProcessor {

    private Context context;
    private ImageView mRecognitionView;

    private float minimumConfidence = 0.5f;
    private Size previewSize;

    private int[] colors;

    private Paint boxPaint;
    private Paint textBoxPaint;
    private Paint textPaint;

    public ObjectProcessor(Context context, Size previewSize) {
        this.context = context;
        this.previewSize = previewSize;

        String[] colorNames = context.getResources().getStringArray(R.array.colorNames);
        TypedArray typedArray = context.getResources().obtainTypedArray(R.array.colors);
        colors = new int[colorNames.length];
        for (int i = 0; i < colors.length; i ++) {
            colors[i] = typedArray.getColor(i, 0);
        }

        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.RED);
        boxPaint.setStrokeWidth(5);

        textBoxPaint = new Paint();
        textBoxPaint.setColor(Color.RED);
        textBoxPaint.setAlpha(128);
        textBoxPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeWidth(1.5f);
        textPaint.setTextSize(50);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setRecognitionView(ImageView recognitionView) {
        this.mRecognitionView = recognitionView;
    }

    public void process(List<Recognition> recognitions, Matrix cropToFrameTransform) {

        Bitmap boxes = Bitmap.createBitmap(previewSize.getWidth(), previewSize.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(boxes);

        for (Recognition rec: recognitions) {
            if (rec.getConfidence() >= minimumConfidence) {

                String information = rec.getTitle() +
                        String.format(" %.1f", rec.getConfidence() * 100) + "%";

                RectF location = new RectF(rec.getLocation());
                cropToFrameTransform.mapRect(location);

                float boxWidth = location.right - location.left;
                float boxHeight = location.bottom - location.top;
                float r = boxHeight * 0.1f;
//                boxPaint.setColor(colors[rec.getId()]);
                canvas.drawRoundRect(location, r, r, boxPaint);

                float textWidth = 10f;
                float[] widths = new float[information.length()];
                textPaint.getTextWidths(information, widths);
                for (float w: widths) {
                    textWidth += w;
                }
                float textHeight = textPaint.getTextSize();

//                textBoxPaint.setColor(colors[rec.getId()]);
                canvas.drawRect(
                        location.centerX() - textWidth / 2,
                        location.top,
                        location.centerX() + textWidth / 2,
                        location.top + textHeight,
                        textBoxPaint);

                canvas.drawText(information, location.centerX(), location.top + textHeight, textPaint);
            }
        }
        mRecognitionView.setImageBitmap(boxes);

    }

}
