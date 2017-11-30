#include <jni.h>
#include <string>
#include "../../../../FFmpegBox/src/main/cpp/compat/avisynth/avs/types.h"
#include "../../../../FFmpegBox/src/main/cpp/compat/avisynth/windowsPorts/windows2linux.h"
#include <stdint.h>

extern "C"
//JNIEXPORT jstring
//
//JNICALL
//Java_com_yibogame_superrecorder_MainActivity_stringFromJNI(
//        JNIEnv *env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}

JNIEXPORT jint
JNICALL
JAVA_com_yibogame_superrecorder_VolumeUtil_resetVolume(JNIEnv *env,jbyte pData, jint nLen, jint nBitsPerSample, jfloat multiple)
{
    int nCur = 0;
    if (16 == nBitsPerSample)
    {
        while (nCur < nLen)
        {
            short* volum = (short*)(pData + nCur);
            *volum = (*volum) * multiple;
            if (*volum < -0x8000)
            {
                *volum = -0x8000;
            }
            if (*volum > SHRT_MAX)//爆音的处理
            {
                *volum = SHRT_MAX;
            }
            *(short*)(pData + nCur) = *volum  ;
            nCur += 2;
        }

    }
    else if (8 == nBitsPerSample)
    {
//        while (nCur < nLen)
//        {
//            BYTE* volum = pData + nCur;
//            *volum = (*volum) * multiple;
//            if (*volum > 255)//爆音的处理
//            {
//                *volum = 255;
//            }
//            *pData  = *volum  ;
//            nCur ++;
//        }

    }
    return S_OK;

}