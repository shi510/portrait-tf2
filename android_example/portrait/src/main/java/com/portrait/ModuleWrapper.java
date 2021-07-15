package com.portrait;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ModuleWrapper {
    static {
        System.loadLibrary("mask_processing-lib");
    }
    private PortraitModule portraitNet;
    private BlurModule blurModule = new BlurModule();
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private byte[] yuvOutput;
    private int width;
    private int height;
    Bitmap rgbFrame;

    public native void argb2yuv420p(byte[] bitmap_out, Bitmap img);

    public ModuleWrapper(Activity activity) throws IOException {
        portraitNet = new PortraitModule(activity, PortraitModule.Model.FloatModel, PortraitModule.Device.CPU, 1);
    }

    public ModuleWrapper(Activity activity,  PortraitModule.Model model, PortraitModule.Device device, int num_thread) throws IOException {
        portraitNet = new PortraitModule(activity,  model, device, num_thread);
    }

    public byte[] backgroundBlur(Image image, int rotation){
        final Image.Plane[] planes = image.getPlanes();
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();
        fillBytes(planes, yuvBytes);
        if(width != image.getWidth() || height != image.getHeight()){
            width = image.getWidth();
            height = image.getHeight();
            rgbBytes = new int[width * height];
            yuvOutput = new byte[width * height * 3 / 2];
            rgbFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        ImageUtils.convertYUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                height,
                width,
                yRowStride,
                uvRowStride,
                uvPixelStride,
                rgbBytes);
        rgbFrame.setPixels(rgbBytes, 0, width, 0, 0, width, height);
        Bitmap rotated = rotateBitmap(rgbFrame, rotation);
        Bitmap masked = portraitNet.recognizeImage(rotated, 0);
        Bitmap output = blurModule.maskedBlur(rotated, masked);
        argb2yuv420p(yuvOutput, output);
        return yuvOutput;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotation){
        if(rotation == 0){
            return bitmap;
        }
        else{
            int sx = 1;
            int sy = 1;
            if(rotation == 270){
                sx = -1;
            }
            else if(rotation == 90){
                sx=1;
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            matrix.postScale(sx, sy);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        }
    }

    private void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }
}
