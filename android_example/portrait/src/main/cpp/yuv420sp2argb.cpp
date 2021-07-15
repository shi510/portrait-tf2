#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <string>

#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, "NATIVE_FILTER",__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"NATIVE_FILTER",__VA_ARGS__)

void yuv420sp_to_argb888(unsigned char *argb, unsigned char *yuv420sp, int width, int height)
{

}
//
//extern "C"
//JNIEXPORT void JNICALL
//Java_com_portrait_ImageUtils_native_1yuv420sp_1to_1argb888(JNIEnv *env, jclass clazz,
//        jbyteArray input, jint width,
//        jint height, jintArray output) {
//    // Properties
//    AndroidBitmapInfo   infoOut;
//    jbyte*             pixelsOut = env->GetByteArrayElements(yuvOut, nullptr);
//    void*               pixelsIn;
//    int ret;
//
//    // Get image info
//    if ((ret = AndroidBitmap_getInfo(env, img, &infoOut)) != 0) {
////        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
//        return;
//    }
//
//    // Check image
//    if (infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
//        return;
//    }
//
//    // Lock all images
//    if ((ret = AndroidBitmap_lockPixels(env, img, &pixelsIn)) != 0) {
//        return;
//    }
//
//    int h = infoOut.height;
//    int w = infoOut.width;
//
//    argb888_to_yuv420p((unsigned char*)pixelsOut, (unsigned char *)pixelsIn, w, h);
//
//    // Unlocks everything
//    AndroidBitmap_unlockPixels(env, img);
//    env->ReleaseByteArrayElements(yuvOut, pixelsOut, 0);
//}