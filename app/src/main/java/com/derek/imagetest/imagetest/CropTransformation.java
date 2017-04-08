package com.derek.imagetest.imagetest;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.squareup.picasso.Transformation;

public class CropTransformation implements Transformation {
    private int width, height, x, y;

    public CropTransformation(Context context, int width, int height, int x, int y) {
        super();
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    @Override
    public Bitmap transform(Bitmap bitmap) {
        int
                nX = Math.max(0, Math.min(bitmap.getWidth(), x)),
                nY = Math.max(0, Math.min(bitmap.getHeight(), y)),
                nWidth = Math.max(0, Math.min(bitmap.getWidth() - nX, width)),
                nHeight = Math.max(0, Math.min(bitmap.getHeight() - nY, height));

        Bitmap b = Bitmap.createBitmap(bitmap, nX, nY, nWidth, nHeight);
        if(bitmap != b) bitmap.recycle();
        return b;
    }

    @Override
    public String key() {
        return "crop";
    }
}