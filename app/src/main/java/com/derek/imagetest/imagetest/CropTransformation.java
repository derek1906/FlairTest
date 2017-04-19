package com.derek.imagetest.imagetest;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.squareup.picasso.Transformation;

public class CropTransformation implements Transformation {
    private boolean isPercentage;
    private int width, height, x, y;
    private String id;

    public CropTransformation(Context context, String id, int width, int height, int x, int y) {
        super();
        this.id = id;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    public CropTransformation(Context context, String id, int width, int height, int x, int y, boolean isPercentage) {
        this(context, id, width, height, x, y);
        this.isPercentage = isPercentage;
    }

    @Override
    public Bitmap transform(Bitmap bitmap) {
        int nX, nY;

        if(isPercentage) {
            nX = Math.max(0, Math.min(bitmap.getWidth() - 1, bitmap.getWidth() * x / 100));
            nY = Math.max(0, Math.min(bitmap.getHeight() - 1, bitmap.getHeight() * y / 100));
        }else{
            nX = Math.max(0, Math.min(bitmap.getWidth() - 1, x));
            nY = Math.max(0, Math.min(bitmap.getHeight() - 1, y));
        }

        int
                nWidth = Math.max(1, Math.min(bitmap.getWidth() - nX - 1, width)),
                nHeight = Math.max(1, Math.min(bitmap.getHeight() - nY - 1, height));

        Bitmap b = Bitmap.createBitmap(bitmap, nX, nY, nWidth, nHeight);
        if(bitmap != b) bitmap.recycle();
        return b;
    }

    @Override
    public String key() {
        return "crop-" + id;
    }
}