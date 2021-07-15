package com.vcall.custom_view;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class BlurryView extends View {
    Bitmap originImg;

    public BlurryView(Context context) {
        super(context);
    }

    public BlurryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BlurryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BlurryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void set(Bitmap img){
        originImg = img;
    }

    @Override
    public void onDraw(Canvas canvas) {
//        }
        if(originImg != null){
            int targetHeight = originImg.getHeight() * this.getWidth() / originImg.getWidth();
            Bitmap toDraw = Bitmap.createScaledBitmap(originImg, this.getWidth(), targetHeight, true);
            canvas.drawBitmap(toDraw, 0, 0, null);
        }
    }
}
