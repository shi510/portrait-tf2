package com.portrait;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlurModule {
    static {
        System.loadLibrary("mask_processing-lib");
    }
    final int EXECUTOR_THREADS = Runtime.getRuntime().availableProcessors() / 2;
    final ExecutorService EXECUTOR = Executors.newFixedThreadPool(EXECUTOR_THREADS);
    Bitmap filtered = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    private static native void nativeBlur(Bitmap bitmap_out, int radius, int thread_count, int thread_index, int round);

    public native void filterForeground(Bitmap bitmap_out, Bitmap img, Bitmap mask);

    public native void filterForegroundV2(Bitmap bitmap_out, Bitmap img, int[] mask);

    public native void mergeForeground(Bitmap fg, Bitmap bg, Bitmap mask);

    public Bitmap blur(Bitmap original, int radius) {
        Bitmap bitmapOut = original.copy(Bitmap.Config.ARGB_8888, true);
        nativeBlur(bitmapOut, radius, 1, 0, 1);
        nativeBlur(bitmapOut, radius, 1, 0, 2);
        return bitmapOut;
    }

    // thread race issue
    public Bitmap blur_with_thread(Bitmap original, int radius) {
        Bitmap bitmapOut = original.copy(Bitmap.Config.ARGB_8888, true);
        int cores = EXECUTOR_THREADS;

        ArrayList<NativeTask> horizontal = new ArrayList<NativeTask>(cores);
        ArrayList<NativeTask> vertical = new ArrayList<NativeTask>(cores);
        for (int i = 0; i < cores; i++) {
            horizontal.add(new NativeTask(bitmapOut, (int) radius, cores, i, 1));
            vertical.add(new NativeTask(bitmapOut, (int) radius, cores, i, 2));
        }

        try {
            EXECUTOR.invokeAll(horizontal);
            EXECUTOR.invokeAll(vertical);
        } catch (InterruptedException e) {
            Log.e("BlurModule", e.getMessage());
            return bitmapOut;
        }
        return bitmapOut;
    }

    public Bitmap maskedBlur(Bitmap img, Bitmap mask){
        Bitmap bitmapOut = img.copy(Bitmap.Config.ARGB_8888, true);
        if(filtered.getHeight() != mask.getHeight() ||
                filtered.getWidth() != mask.getWidth())
        {
            filtered = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
        }
        Bitmap scaledImg = Bitmap.createScaledBitmap(bitmapOut, mask.getWidth(), mask.getHeight(), true);
        filterForeground(filtered, scaledImg, mask);
//        filterForegroundV2(filtered, scaledImg, mask);
        Bitmap blurred = filtered;
//        Bitmap blurred = Bitmap.createScaledBitmap(filtered, filtered.getWidth(), filtered.getHeight(), true);
        blurred = blur(blurred, 3);
//        blurred = blur_with_thread(blurred, 2);
        mask = Bitmap.createScaledBitmap(mask, img.getWidth(), img.getHeight(), true);
        blurred = Bitmap.createScaledBitmap(blurred, img.getWidth(), img.getHeight(), true);
        mergeForeground(bitmapOut, blurred, mask);
        return bitmapOut;
    }

    private static class NativeTask implements Callable<Void> {
        private final Bitmap _bitmapOut;
        private final int _radius;
        private final int _totalCores;
        private final int _coreIndex;
        private final int _round;

        public NativeTask(Bitmap bitmapOut, int radius, int totalCores, int coreIndex, int round) {
            _bitmapOut = bitmapOut;
            _radius = radius;
            _totalCores = totalCores;
            _coreIndex = coreIndex;
            _round = round;
        }

        @Override public Void call() throws Exception {
            nativeBlur(_bitmapOut, _radius, _totalCores, _coreIndex, _round);
            return null;
        }

    }
}

