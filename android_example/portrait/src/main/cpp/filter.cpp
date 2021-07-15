#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <string>

#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, "NATIVE_FILTER",__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"NATIVE_FILTER",__VA_ARGS__)

void filter_forground(unsigned char* out,                ///< out image data
                      unsigned char* img,                ///< img image data
                      unsigned char* mask,               ///< mask image data
                      unsigned int w,                    ///< image width
                      unsigned int h                     ///< image height
)
{
    for(int r = 0; r < h; ++r){
        for(int c = 0; c < w; ++c){
            int idx = r*w*4 + c*4;
            int val = mask[idx];
            if(val == 255){
                out[idx + 0] = img[idx + 0];
                out[idx + 1] = img[idx + 1];
                out[idx + 2] = img[idx + 2];
                out[idx + 3] = img[idx + 3];
            }
            else{
                out[idx + 0] = 0;
                out[idx + 1] = 0;
                out[idx + 2] = 0;
                out[idx + 3] = 0;
            }
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_portrait_BlurModule_filterForeground(JNIEnv *env, jobject self, jobject bitmap_out,
jobject img, jobject mask) {
    // Properties
    AndroidBitmapInfo   infoOut;
    void*               pixelsOut;
    void*               pixelsImg;
    void*               pixelsMask;

    int ret;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, img, &infoOut)) != 0) {
//        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    // Check image
    if (infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
//        LOGE("Bitmap format is not RGBA_8888!");
//        LOGE("==> %d", infoOut.format);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, bitmap_out, &pixelsOut)) != 0) {
//        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, img, &pixelsImg)) != 0) {
//        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, mask, &pixelsMask)) != 0) {
//        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    int h = infoOut.height;
    int w = infoOut.width;

    filter_forground((unsigned char*)pixelsOut, (unsigned char*)pixelsImg, (unsigned char*)pixelsMask, w, h);

    // Unlocks everything
    AndroidBitmap_unlockPixels(env, bitmap_out);
    AndroidBitmap_unlockPixels(env, img);
    AndroidBitmap_unlockPixels(env, mask);
}

void filter_forground_v2(unsigned char* out,                ///< out image data
                      unsigned char* img,                ///< img image data
                      int* mask,               ///< mask image data
                      unsigned int w,                    ///< image width
                      unsigned int h                     ///< image height
)
{
    for(int r = 0; r < h; ++r){
        for(int c = 0; c < w; ++c){
            int idx = r*w*4 + c*4;
            float bg = mask[r*w*2 + c*2 + 0];
            float fg = mask[r*w*2 + c*2 + 1];
            if(bg > fg){
                out[idx + 0] = img[idx + 0];
                out[idx + 1] = img[idx + 1];
                out[idx + 2] = img[idx + 2];
                out[idx + 3] = img[idx + 3];
            }
            else if(fg < 0.5){
                out[idx + 0] = img[idx + 0];
                out[idx + 1] = img[idx + 1];
                out[idx + 2] = img[idx + 2];
                out[idx + 3] = img[idx + 3];
            }
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_portrait_BlurModule_filterForegroundV2(JNIEnv *env, jobject self, jobject bitmap_out,
                                                jobject img, jintArray mask) {
    // Properties
    AndroidBitmapInfo   infoOut;
    void*               pixelsOut;
    void*               pixelsImg;
    jint*               pixelsMask = env->GetIntArrayElements(mask, nullptr);

    int ret;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, img, &infoOut)) != 0) {
//        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    // Check image
    if (infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
//        LOGE("Bitmap format is not RGBA_8888!");
//        LOGE("==> %d", infoOut.format);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, bitmap_out, &pixelsOut)) != 0) {
//        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, img, &pixelsImg)) != 0) {
//        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    int h = infoOut.height;
    int w = infoOut.width;

    filter_forground_v2((unsigned char*)pixelsOut, (unsigned char*)pixelsImg, pixelsMask, w, h);

    // Unlocks everything
    AndroidBitmap_unlockPixels(env, bitmap_out);
    AndroidBitmap_unlockPixels(env, img);
    AndroidBitmap_unlockPixels(env, mask);
}

void merge_foregound(unsigned char* fg,
                     const unsigned char* bg,
                     const unsigned char* mask,
                     unsigned int w,
                     unsigned int h
)
{
    for(int r = 0; r < h; ++r){
        for(int c = 0; c < w; ++c){
            int idx = r*w*4 + c*4;
            int val = mask[idx+1];
            if(val != 0){
                fg[idx+0] = bg[idx+0];
                fg[idx+1] = bg[idx+1];
                fg[idx+2] = bg[idx+2];
                fg[idx+3] = bg[idx+3];
            }
        }
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_portrait_BlurModule_mergeForeground(JNIEnv *env, jobject self, jobject fg, jobject bg,
                                             jobject mask) {
    // Properties
    AndroidBitmapInfo   infoOut;
    void*               pixelsFg;
    void*               pixelsBg;
    void*               pixelsMask;

    int ret;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, fg, &infoOut)) != 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    // Check image
    if (infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888!");
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, fg, &pixelsFg)) != 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, bg, &pixelsBg)) != 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, mask, &pixelsMask)) != 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    int h = infoOut.height;
    int w = infoOut.width;

    merge_foregound((unsigned char*)pixelsFg, (unsigned char*)pixelsBg, (unsigned char*)pixelsMask, w, h);

    // Unlocks everything
    AndroidBitmap_unlockPixels(env, fg);
    AndroidBitmap_unlockPixels(env, bg);
    AndroidBitmap_unlockPixels(env, mask);
}
