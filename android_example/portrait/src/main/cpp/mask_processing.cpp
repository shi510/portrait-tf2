#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <cmath>

#define LOG_TAG "libbitmaputils"

void mask_processing(unsigned char* out,               ///< out image data
                  float* mask,                        ///< mask image data
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
                out[idx] = 255;
                out[idx+1] = 255;
                out[idx+2] = 255;
                out[idx+3] = 255;
            }
            else if(fg < 0.50){
                out[idx] = 255;
                out[idx+1] = 255;
                out[idx+2] = 255;
                out[idx+3] = 255;
            }
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_portrait_PortraitModule_maskProcessing(JNIEnv *env, jobject self, jobject bitmap_out,
                                                jfloatArray mask_prob) {
    // Properties
    AndroidBitmapInfo   infoOut;
    void*               pixelsOut;
    jfloat*             pixelsMask = env->GetFloatArrayElements(mask_prob, nullptr);
    int ret;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, bitmap_out, &infoOut)) != 0) {
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

    int h = infoOut.height;
    int w = infoOut.width;


    mask_processing((unsigned char*)pixelsOut, (float *)pixelsMask, w, h);

    // Unlocks everything
    AndroidBitmap_unlockPixels(env, bitmap_out);
    env->ReleaseFloatArrayElements(mask_prob, pixelsMask, 0);
}
