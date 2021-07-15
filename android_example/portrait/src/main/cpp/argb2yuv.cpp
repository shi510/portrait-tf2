#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <string>

#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, "NATIVE_FILTER",__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"NATIVE_FILTER",__VA_ARGS__)

void argb888_to_yuv420p(unsigned char *destination, unsigned char *argb, int width, int height)
{
    const int chann = 4;
    int image_size = width * height;
    int upos = image_size;
    int vpos = upos + upos / 4;
    int i = 0;

    for( int line = 0; line < height; ++line )
    {
        if( !(line % 2) )
        {
            for( int x = 0; x < width; x += 2 )
            {
                unsigned char r = argb[chann * i + 1];
                unsigned char g = argb[chann * i + 2];
                unsigned char b = argb[chann * i + 3];

                destination[i++] = ((66*r + 129*g + 25*b) >> 8) + 16;
                destination[upos++] = ((-38*r + -74*g + 112*b) >> 8) + 128;
                destination[vpos++] = ((112*r + -94*g + -18*b) >> 8) + 128;

                r = argb[chann * i + 1];
                g = argb[chann * i + 2];
                b = argb[chann * i + 3];

                destination[i++] = ((66*r + 129*g + 25*b) >> 8) + 16;
            }
        }
        else
        {
            for(int x = 0; x < width; x += 1 )
            {
                unsigned char r = argb[chann * i + 1];
                unsigned char g = argb[chann * i + 2];
                unsigned char b = argb[chann * i + 3];
                destination[i++] = ((66*r + 129*g + 25*b) >> 8) + 16;
            }
        }
    }
}

void argb888_to_yuv420sp(unsigned char *destination, unsigned char *argb, int width, int height)
{
    const int chann = 4;
    int image_size = width * height;
    int uvpos = image_size;
    int i = 0;

    for( int line = 0; line < height; ++line )
    {
        if( !(line % 2) )
        {
            for( int x = 0; x < width; x += 2 )
            {
                unsigned char r = argb[chann * i + 1];
                unsigned char g = argb[chann * i + 2];
                unsigned char b = argb[chann * i + 3];

                destination[i++] = ((66*r + 129*g + 25*b) >> 8) + 16;
                destination[uvpos++] = ((-38*r + -74*g + 112*b) >> 8) + 128;
                destination[uvpos++] = ((112*r + -94*g + -18*b) >> 8) + 128;

                r = argb[chann * i + 1];
                g = argb[chann * i + 2];
                b = argb[chann * i + 3];

                destination[i++] = ((66*r + 129*g + 25*b) >> 8) + 16;
            }
        }
        else
        {
            for(int x = 0; x < width; x += 1 )
            {
                unsigned char r = argb[chann * i + 1];
                unsigned char g = argb[chann * i + 2];
                unsigned char b = argb[chann * i + 3];
                destination[i++] = ((66*r + 129*g + 25*b) >> 8) + 16;
            }
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_portrait_ModuleWrapper_argb2yuv420p(JNIEnv *env, jobject thiz, jbyteArray yuvOut,
                                             jobject img) {
    // Properties
    AndroidBitmapInfo   infoOut;
    jbyte*             pixelsOut = env->GetByteArrayElements(yuvOut, nullptr);
    void*               pixelsIn;
    int ret;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, img, &infoOut)) != 0) {
//        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    // Check image
    if (infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, img, &pixelsIn)) != 0) {
        return;
    }

    int h = infoOut.height;
    int w = infoOut.width;

    argb888_to_yuv420p((unsigned char*)pixelsOut, (unsigned char *)pixelsIn, w, h);

    // Unlocks everything
    AndroidBitmap_unlockPixels(env, img);
    env->ReleaseByteArrayElements(yuvOut, pixelsOut, 0);
}
