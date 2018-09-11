#include <jni.h>
#include <string>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "glrender.h"

static glrender *render = NULL;
static FILE *finput_yuv = NULL;
static int frame_size = 320 * 240 * 3 / 2;
static char *frame_data = NULL;
static bool post_process = false;
//currently no usage
static EGLContext g_egl_context = NULL;
static EGLDisplay g_egl_display = NULL;

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_joe_opengl_1es_1test_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jint JNICALL
Java_com_joe_opengl_1es_1test_gles_GLNativeRender_init(
        JNIEnv *env,
        jobject /* this */, int w, int h, jobject assetManager) {
    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);

    if(!render) {
        render = new glrender(mgr);
        if(render->init(w, h, post_process)) {
            render->uninit();
            delete render;
            render = NULL;
            return -1;
        }
    }

    if(!finput_yuv) {
        finput_yuv = fopen("/sdcard/test_15frames.yuv", "rb");
        if(!finput_yuv) {
            LOGE("can not open yuv input file: /sdcard/test_15frames.yuv");
            return -1;
        }
    }

    if(!frame_data) {
        frame_data = (char*)malloc(frame_size);
    }

    return 0;
}

JNIEXPORT void JNICALL
Java_com_joe_opengl_1es_1test_gles_GLNativeRender_updateReso(
        JNIEnv *env,
        jobject /* this */, int w, int h) {
    if(render) {
        render->update_reso(w, h);
    }

}

JNIEXPORT void JNICALL
Java_com_joe_opengl_1es_1test_gles_GLNativeRender_setPostProcess(
        JNIEnv *env,
        jobject /* this */, jboolean postProcess) {
    if(render) {
        render->setPostProcess(postProcess);
    } else {
        post_process = postProcess;
    }
}

JNIEXPORT void JNICALL
Java_com_joe_opengl_1es_1test_gles_GLNativeRender_uinit(
        JNIEnv *env,
        jobject /* this */) {
    if(render) {
        render->uninit();
        delete render;
        render = NULL;
    }

    if(finput_yuv) {
        fclose(finput_yuv);
        finput_yuv = NULL;
    }

    if(frame_data) {
        free(frame_data);
        frame_data = NULL;
    }
}

JNIEXPORT jint JNICALL
Java_com_joe_opengl_1es_1test_gles_GLNativeRender_drawYUV(
        JNIEnv *env,
        jobject /* this */) {
    if(!render || !finput_yuv || !frame_data) {
        LOGE("error status, not correct init yet");
        return -1;
    }

    size_t read_size = fread(frame_data, 1, frame_size, finput_yuv);
    if(read_size < frame_size) {
        LOGE("no data for read");
        return -2;
    }

    //LOGI("read frame:%d,%d,%d",frame_data[0], frame_data[1], frame_data[2]);
    render->draw(frame_data);
    //render->drawTriangle();

    return 0;
}

}