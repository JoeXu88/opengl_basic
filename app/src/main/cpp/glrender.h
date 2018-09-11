//
// Created by david on 24/08/2018.
//

#ifndef OPENGL_BASIC_GLRENDER_H
#define OPENGL_BASIC_GLRENDER_H

#include <GLES2/gl2.h>
#include <EGL/egl.h>
#include <android/log.h>
#include <android/asset_manager.h>
#include "cl_gl.h"
#include "FrmKernel.h"
#include "FrmUtils.h"

#if 0
#define  LOG_TAG    "native_glrender"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#endif

typedef enum _type {
    TYPE_NONE,
    TYPE_GL,
    TYPE_FBO,
}RND_TYPE;

class glrender {
public:
    glrender(AAssetManager *mgr);
    virtual ~glrender();
    int init(int w, int h, bool postProcess);
    int init(int w, int h, EGLContext context, EGLDisplay display);
    int uninit();
    void update_reso(int w, int h);
    void draw(char* yuvdata);
    void drawTriangle();
    void setPostProcess(bool process);

private:
    bool mPostProcess;
    AAssetManager *mAssetMgr;
    //general params
    int mWidth;
    int mHeight;
    int mTextureW;
    int mTextureH;
    int mYSize;
    int mUVSize;
    bool mTriangle;

    //gl params
    GLuint mTextures[3];//yuv texures
    GLuint mProgram;
    GLuint mVetex_shader;
    GLuint mFrag_shader;
    GLint _posLocate;
    GLint _texLocate;

    //FBO params
    GLuint mFBOPrgram;
    GLuint mFBOVetex_shader;
    GLuint mFBOFrag_shader;
    GLint _FBOposLocate;
    GLint _FBOtexLocate;
    GLuint mFBOBufId;
    GLuint mFBOTexID;
    GLuint mFBODepBufID;
    size_t m_nFBOHeight;
    size_t m_nFBOWidth;

    //cl params
    cl_command_queue m_commandQueue;// OpenCL command queue
    cl_program m_clprogram; // OpenCL program
    cl_kernel m_kernel;// OpenCL kernel
    cl_mem m_fboMem;// OpenCL image object pointer to FBO memory
    cl_mem m_filteredTexMem;// OpenCL image object pointer to result texture memory
    bool m_sharedBuffer;
    BYTE* m_pHostBuf;// Host memory buffer for intermediate copying step if no buffersharing
    cl_context m_clcontext;
    cl_device_id *m_devices;
    cl_platform_id m_clplatform;// OpenCL platform
    INT32         m_deviceCount;// Number of devices
    EGLDisplay			m_eglDisplay;
    EGLSurface			m_eglSurface;
    EGLContext			m_eglContext;

    //gl functions
    GLuint create_program(RND_TYPE type);
    GLuint compile_shader(GLenum type, const char* code);
    void setup_textures();
    int enable_attrib(RND_TYPE type);
    int checkError(const char* option);

    //fbo functions
    int createFBO();
    int freeFBO();

    //cl functions
    int cl_init();
    int cl_createContext(cl_device_type deviceType);
    void cl_destroyContext();
    int cl_post_process();
    int InitializeEgl();
};

#endif //OPENGL_BASIC_GLRENDER_H
