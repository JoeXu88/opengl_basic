//
// Created by david on 24/08/2018.
//

#define CL_USE_DEPRECATED_OPENCL_1_1_APIS

#include "glrender.h"
#include <stdio.h>
#include <stdlib.h>
#include <android/asset_manager.h>
#include <android/native_window.h>
#include <cstring>

const char vetex_code[] =
    "attribute vec4 vPosition;\n"
    "attribute vec2 a_texCoord;\n"
    "varying vec2 tc;\n"
    "void main() {\n"
        "gl_Position = vPosition;\n"
        "tc = a_texCoord;\n"
    "}";
const char frag_code[] =
    "precision mediump float;\n"
    "uniform sampler2D tex_y;\n"
    "uniform sampler2D tex_u;\n"
    "uniform sampler2D tex_v;\n"
    "varying vec2 tc;\n"
    "void main() {\n"
        "vec3 yuv;\n"
        "yuv.x = texture2D(tex_y, tc).r  - 16./255.;\n"
        "yuv.y = texture2D(tex_u, tc).r - 128./255.;\n"
        "yuv.z = texture2D(tex_v, tc).r - 128./255.;\n"
        "vec3 c = mat3(1.164, 1.164, 1.164,\n"
                      "0, -0.391, 2.018,\n"
                      "1.596, -0.813, 0) * yuv;\n"
        "gl_FragColor = vec4(c, 1);\n"
    "}";

const char fbo_vetex_code[] =
        "attribute vec4 vPosition;\n"
        "attribute vec2 a_texCoord;\n"
        "varying vec2 tc;\n"
        "void main() {\n"
            "gl_Position = vPosition;\n"
            "tc = a_texCoord;\n"
        "}";

const char fbo_frag_code[] =
        "precision mediump float;\n"
        "uniform sampler2D tex;\n"
        "varying vec2 tc;\n"
        "void main() {\n"
            "gl_FragColor = texture2D(tex, tc);\n"
        "}";

const char triangle_vetex[] =
        "attribute vec4 vPosition;\n"
        "void main() {\n"
        "    gl_Position = vPosition;\n"
        "}";

const char triangle_frag[] =
        "precision mediump float;"
        "uniform vec4 vColor;"
        "void main() {"
        "  gl_FragColor = vColor;" //mainly to get frament drawing color
        "}";

const char *YUVTEX_UNIFORM[3] = {"tex_y", "tex_u", "tex_v"};
const GLfloat vertices[20] =
        // X, Y, Z, U, V 90degree
        {1, -1, 0, 1, 1, //Bottom Right
         1, 1, 0, 1, 0, //Top Right
         -1, 1, 0, 0, 0, //Top Left
         -1, -1, 0, 0, 1}; // Bottom Left

const char g_indices[] = { 0, 3, 2, 0, 2, 1 };

glrender::glrender(AAssetManager *mgr)
{
    mPostProcess = true;
    mAssetMgr = mgr;

    mTriangle = false;
    mProgram = 0;
    mVetex_shader = 0;
    mFrag_shader = 0;
    _posLocate = -1;
    _texLocate = -1;

    mFBOPrgram = 0;
    mFBOTexID = 0;
    mFBOVetex_shader = 0;
    mFBOFrag_shader = 0;
    _FBOposLocate = -1;
    _FBOtexLocate = -1;

    mTextures[0] = 0;
    mTextures[1] = 0;
    mTextures[2] = 0;
    mTextureH = 0;
    mTextureW = 0;

    m_clplatform = 0;
    m_clcontext = 0;
    m_devices = 0;
    m_deviceCount = 0;
    m_sharedBuffer = false;
    m_kernel = 0;
    m_fboMem = 0;
    m_filteredTexMem = 0;
}

glrender::~glrender() {
    uninit();
}

int glrender::init(int w, int h, EGLContext context, EGLDisplay display) {
    m_eglContext = context;
    m_eglDisplay = display;
    return init(w, h, mPostProcess);
}

int glrender::init(int w, int h, bool postProcess) {
    update_reso(w, h);

    setPostProcess(postProcess);

    int ret = 0;
    mProgram = create_program(TYPE_GL);
    if(!mProgram) {
        LOGE("can not create program");
        return -1;
    }

    mFBOPrgram = create_program(TYPE_FBO);
    if(!mFBOPrgram) {
        LOGE("can not create fbo program");
        return -1;
    }

    //LOGI("Got program:%d",mProgram);
    //glUseProgram(mProgram);

    if(!mTriangle) {
        ret = enable_attrib(TYPE_GL);
        ret |= enable_attrib(TYPE_FBO);
        if (ret)
            return ret;
        setup_textures(); //only need do once
        createFBO();
    }

    if(!cl_init()) return -2;

    return 0;
}

void glrender::update_reso(int w, int h) {
    mWidth = w;
    mHeight = h;
    mYSize = mWidth * mHeight;
    mUVSize = mYSize / 4;
    m_nFBOHeight = mHeight;
    m_nFBOWidth = mWidth;
}

int glrender::enable_attrib(RND_TYPE type) {
    if(type == TYPE_NONE) return 0;
    glUseProgram(type==TYPE_GL? mProgram:mFBOPrgram);
    GLint *posLoc = type==TYPE_GL? &_posLocate:&_FBOposLocate;
    GLint *texLoc = type==TYPE_GL? &_texLocate:&_FBOtexLocate;

    if(*posLoc < 0)
        *posLoc = glGetAttribLocation(type==TYPE_GL? mProgram:mFBOPrgram, "vPosition");
    if(checkError("glGetAttribLocation:vPosition") || *posLoc == -1) return -1;
    //float pos[] = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,}; // fullscreen
    glVertexAttribPointer(*posLoc, 3, GL_FLOAT, 0, 5 * sizeof(GLfloat), vertices);//
    glEnableVertexAttribArray(*posLoc);

    if(*texLoc < 0)
        *texLoc = glGetAttribLocation(type==TYPE_GL? mProgram:mFBOPrgram, "a_texCoord");
    if(checkError("glGetAttribLocation:a_texCoord") || *texLoc == -1) return -1;
    //float coordVertices[] = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,};// whole-texture
    //glVertexAttribPointer(_texLocate, 2, GL_FLOAT, GL_FALSE, 8, coordVertices);
    glVertexAttribPointer(*texLoc, 2, GL_FLOAT, 0, 5 * sizeof(GLfloat), &vertices[3]);
    glEnableVertexAttribArray(*texLoc);

    return 0;
}

int glrender::uninit() {
    if(mFrag_shader) glDeleteShader(mFrag_shader);
    if(mVetex_shader) glDeleteShader(mVetex_shader);
    if(mProgram) glDeleteProgram(mProgram);
    glUseProgram(0);
    if(!mTriangle) {
        glDisableVertexAttribArray(_posLocate);
        glDisableVertexAttribArray(_texLocate);
    }
    glFinish();

    cl_destroyContext();

    return 0;
}

GLuint glrender::create_program(RND_TYPE type) {
    if(type == TYPE_NONE) return 0;
    else if(type == TYPE_GL && mProgram) return mProgram;
    else if(type == TYPE_FBO && mFBOPrgram) return mFBOPrgram;

    GLuint program = 0;
    if(type == TYPE_GL) {
        if(mVetex_shader) glDeleteShader(mVetex_shader);
        mVetex_shader = compile_shader(GL_VERTEX_SHADER, mTriangle ? triangle_vetex : vetex_code);
        if(mFrag_shader) glDeleteShader(mFrag_shader);
        mFrag_shader = compile_shader(GL_FRAGMENT_SHADER, mTriangle? triangle_frag:frag_code);
    }
    else if(type == TYPE_FBO) {
        if(mFBOVetex_shader) glDeleteShader(mFBOVetex_shader);
        mFBOVetex_shader = compile_shader(GL_VERTEX_SHADER, fbo_vetex_code);
        if(mFBOFrag_shader) glDeleteShader(mFBOFrag_shader);
        mFBOFrag_shader = compile_shader(GL_FRAGMENT_SHADER, fbo_frag_code);
    }

    if((type == TYPE_GL && mVetex_shader && mFrag_shader) ||
        (type == TYPE_FBO && mFBOVetex_shader && mFBOFrag_shader)   ) {
        if(type == TYPE_GL)
            LOGI("got vexShader:%d, fragShader:%d",mVetex_shader, mFrag_shader);
        if(type == TYPE_FBO)
            LOGI("got fbo vexShader:%d, fragShader:%d",mFBOVetex_shader, mFBOFrag_shader);
        program = glCreateProgram();
        if(checkError("create program")) return 0;

        if (program) {
            glAttachShader(program, (type==TYPE_GL)?mVetex_shader:mFBOVetex_shader);
            if(checkError("glAttachShader")) {
                glDeleteProgram(program);
                return 0;
            }
            glAttachShader(program, (type==TYPE_GL)?mFrag_shader:mFBOFrag_shader);
            if(checkError("glAttachShader")) {
                glDeleteProgram(program);
                return 0;
            }
            glLinkProgram(program);
            GLint linkStatus = GL_FALSE;
            glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
            if (linkStatus != GL_TRUE) {
                GLint bufLength = 0;
                glGetProgramiv(program, GL_INFO_LOG_LENGTH, &bufLength);
                if (bufLength) {
                    char* buf = (char*) malloc(bufLength);
                    if (buf) {
                        glGetProgramInfoLog(program, bufLength, NULL, buf);
                        LOGE("Could not link program:\n%s\n", buf);
                        free(buf);
                    }
                }
                glDeleteProgram(program);
                program = 0;
            }
        }
    }

    return program;
}

GLuint glrender::compile_shader(GLenum type, const char * code) {
    GLuint shader = glCreateShader(type);
    if(checkError("shader")) return 0;

    if(shader) {
        glShaderSource(shader, 1, &code, NULL);
        glCompileShader(shader);

        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*) malloc(infoLen);
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    LOGE("Could not compile shader %d:\n%s\n",
                         type, buf);
                    free(buf);
                }
                glDeleteShader(shader);
                shader = 0;
            }
        }
    }

    return shader;
}

void glrender::setup_textures() {
    if(!mProgram) {
        LOGE("error program");
        return;
    }
    glUseProgram(mProgram);
    glDeleteTextures(3, mTextures);
    glGenTextures(3, mTextures);
    LOGI("got yuv textures:%d, %d, %d", mTextures[0], mTextures[1], mTextures[2]);

    for(GLenum i=0; i<3; i++) {
        glActiveTexture(GL_TEXTURE0 + i);
        glBindTexture(GL_TEXTURE_2D, mTextures[i]);
        glUniform1i(glGetUniformLocation(mProgram, YUVTEX_UNIFORM[i]), i);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        checkError("setup text");
        //LOGI("finish text %d set up",i);
    }
    if(!checkError("setup textures")) {
        LOGI("set up textures ok");
    } else {
        LOGE("set up textures error");
    }
}

void glrender::draw(char* yuvdata) {
    if(!mProgram) {
        LOGE("error program, do nothing");
        return;
    }
    if(!yuvdata) {
        LOGE("error yuv data");
        return;
    }

    char *data[3];
    int yframe_size = mWidth * mHeight;
    LOGI("frame w:%d, h:%d",mWidth,mHeight);
    data[0] = yuvdata;                      //y_data
    data[1] = data[0] + yframe_size;        //u_data
    data[2] = data[1] + yframe_size / 4;    //v_data
    LOGI("y:%d,%d,%d",data[0][0],data[0][1],data[0][2]);
    LOGI("u:%d,%d,%d",data[1][0],data[1][1],data[1][2]);
    LOGI("v:%d,%d,%d",data[2][0],data[2][1],data[2][2]);

#if 0
    glBindTexture(GL_TEXTURE_2D, mFBOTexID);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,
                 mWidth, mHeight, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, data[0]);
#endif

    if(mPostProcess) {
        //do cl post process
        if (!m_sharedBuffer)
            memcpy(m_pHostBuf, data[0], yframe_size);
        if (!cl_post_process()) {
            LOGE("cl post process error");
            return;
        }
    }

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mTextures[0]);
    if(mPostProcess) {
        if (!m_sharedBuffer) {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,
                         mWidth, mHeight, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, m_pHostBuf);
        }
    } else {
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,
                     mWidth, mHeight, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, data[0]);
    }

    int h = (mHeight + 1) / 2;
    int w = (mWidth + 1) / 2;
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, mTextures[1]);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,
                 w, h, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, data[1]);

    glActiveTexture(GL_TEXTURE2);
    glBindTexture(GL_TEXTURE_2D, mTextures[2]);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,
                 w, h, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, data[2]);

#if 0
    //load data to textures
    for (GLenum i = 0; i < 3; i++) {
        int h = (i == 0) ? mHeight : (mHeight + 1) / 2;
        int w = (i == 0) ? mWidth : (mWidth + 1) / 2;
        glActiveTexture(GL_TEXTURE0 + i);
        glBindTexture(GL_TEXTURE_2D, mTextures[i]);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,
                w, h, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, data[i]);
    }
#endif

    glClearColor(0.5, 0.5f, 0.5f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
    glUseProgram(mProgram);
    //glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, g_indices);
    checkError("render");
    glFinish();
}

void glrender::drawTriangle() {
    glClearColor(0.5, 0.5f, 0.5f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
    glUseProgram(mProgram);
    GLuint _postion = glGetAttribLocation(mProgram, "vPosition");
    if(checkError("glGetAttribLocation:vPosition") || _postion == -1) return;
    int VERTEX_COORD_SIZE = 3; //how many dimensions each vertex
    int VERTEX_STRIDE = VERTEX_COORD_SIZE * 4; //each dimension is float, 4 bytes
    float vertex[] = {
            -0.3f, -0.3f, 0.0f,
            0.5f, -0.3f, 0.0f,
            0.5f, 0.5f, 0.0f
    };
    glVertexAttribPointer(_postion, VERTEX_COORD_SIZE, GL_FLOAT, false, VERTEX_STRIDE, vertex);
    glEnableVertexAttribArray(_postion);

    GLuint _corlor = glGetUniformLocation(mProgram, "vColor");
    float color[] = { 0.0f, 0.0f, 1.0f, 0.5f }; //r,g,b, alpha
    glUniform4fv(_corlor, 1, color);

    glDrawArrays(GL_TRIANGLES, 0, 3); //triangle have 3 points

}

int glrender::freeFBO() {
    if(mFBOBufId) glDeleteFramebuffers(1, &mFBOBufId);
    if(mFBOTexID) glDeleteTextures(1, &mFBOTexID);
    if(mFBODepBufID) glDeleteRenderbuffers(1, &mFBODepBufID);

    return checkError("free fbo");
}

int glrender::createFBO() {
    freeFBO();

    glUseProgram(mFBOPrgram);
    glGenFramebuffers( 1, &mFBOBufId );
    glBindFramebuffer( GL_FRAMEBUFFER, mFBOBufId );

    glGenTextures( 1, &mFBOTexID );
    glBindTexture( GL_TEXTURE_2D, mFBOTexID );
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR );
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR );
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,     GL_CLAMP_TO_EDGE );
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,     GL_CLAMP_TO_EDGE );
    glTexImage2D( GL_TEXTURE_2D, 0, GL_RGBA, mWidth, mHeight,
                  0, GL_RGBA, GL_UNSIGNED_BYTE, NULL );
    checkError("set up fbo text");

    glActiveTexture( GL_TEXTURE3 );
    glBindTexture( GL_TEXTURE_2D, mFBOTexID );
    glFramebufferTexture2D( GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mFBOTexID, 0 );

    glGenRenderbuffers(1, &mFBODepBufID);
    glBindRenderbuffer(GL_RENDERBUFFER, mFBODepBufID);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, mWidth, mHeight);
    glBindRenderbuffer(GL_RENDERBUFFER, 0);

    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, mFBODepBufID);

    checkError("create fbo");

    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    return 0;
}

int glrender::cl_createContext(cl_device_type deviceType) {
    cl_int errNum;
    cl_uint numPlatforms = 0;
    cl_platform_id platformId[2];

    // Get the first platform ID
    errNum = clGetPlatformIDs( 2, platformId, &numPlatforms );
    if (errNum != CL_SUCCESS || numPlatforms <= 0)
    {
        LOGE("No OpenCL platforms found.");
        return FALSE;
    }

    m_clplatform = platformId[0];

    // Get the number of devices for the requested device type (CPU, GPU, ALL)
    cl_uint numDevices = 0;
    errNum = clGetDeviceIDs( m_clplatform, deviceType, 0, NULL, &numDevices );
    if (errNum != CL_SUCCESS || numDevices <= 0)
    {
        LOGE("No matching OpenCL devices found.");
        return FALSE;
    }

    char platformInfo[1024];
    char logMessage[2048];
    errNum = clGetPlatformInfo( m_clplatform, CL_PLATFORM_VENDOR, sizeof(platformInfo), platformInfo, NULL );
    if (errNum != CL_SUCCESS)
    {
        LOGE("ERROR: getting platform info.");
        return FALSE;
    }
    FrmSprintf( logMessage, sizeof(logMessage), "OpenCL Platform: %s\n", platformInfo );
    FrmLogMessage( logMessage );

    // Get the devices
    m_devices = new cl_device_id[numDevices];
    m_deviceCount = numDevices;
    errNum = clGetDeviceIDs( m_clplatform, deviceType, numDevices, m_devices, NULL );


    if (errNum != CL_SUCCESS)
    {
        LOGE("Erorr getting OpenCL device(s).");
        return FALSE;
    }

    switch (deviceType)
    {
        case CL_DEVICE_TYPE_GPU:
            LOGI("Selected device: GPU\n");
            break;
        case CL_DEVICE_TYPE_CPU:
            LOGI("Selected device: CPU\n");
            break;
        case CL_DEVICE_TYPE_DEFAULT:
        default:
            LOGI("Selected device: DEFAULT\n");
            break;
    }
    LOGI("get device count:%d",m_deviceCount);

    for (int i = 0; i < m_deviceCount; i++)
    {
        char deviceInfo[1024];
        errNum = clGetDeviceInfo( m_devices[i], CL_DEVICE_NAME, sizeof(deviceInfo), deviceInfo, NULL );
        if (errNum == CL_SUCCESS )
        {
            FrmSprintf( logMessage, sizeof(logMessage), "OpenCL Device Name (%d) : %s\n", i , deviceInfo );
            LOGI("%s", logMessage );
        }
    }

#if 0
    if(!InitializeEgl()){
        LOGE("init egl error");
        return FALSE;
    }
#endif

    // Finally, create the context
    cl_context_properties contextProperties[] =
    {
        CL_CONTEXT_PLATFORM,
        (cl_context_properties)m_clplatform,
        CL_GL_CONTEXT_KHR,
        (cl_context_properties)eglGetCurrentContext(),//m_eglContext
        CL_EGL_DISPLAY_KHR,
        (cl_context_properties)eglGetCurrentDisplay(),
        0
    };

    m_clcontext = clCreateContext( contextProperties, m_deviceCount, m_devices, NULL, NULL, &errNum );
    if (errNum != CL_SUCCESS)
    {
        LOGE("Could not create OpenCL context.");
        return FALSE;
    }

    return TRUE;
}

void glrender::cl_destroyContext() {
    if ( m_clcontext != 0 )
    {
        clReleaseContext( m_clcontext );
        m_clcontext = 0;
    }

    if ( m_devices )
    {
        delete [] m_devices;
        m_devices = NULL;
    }

    if( m_fboMem )          clReleaseMemObject( m_fboMem );
    if( m_filteredTexMem )  clReleaseMemObject( m_filteredTexMem );
}

int glrender::cl_init() {
    cl_int errNum;

    if(mAssetMgr)
        g_pAssetManager = mAssetMgr;
    else {
        LOGE("got not asset manager");
        return -1;
    }

    if(!cl_createContext(CL_DEVICE_TYPE_GPU))
        return -1;

    // Create the command queue
    m_commandQueue = clCreateCommandQueue( m_clcontext, m_devices[0], CL_QUEUE_PROFILING_ENABLE, &errNum );
    if ( errNum != CL_SUCCESS )
    {
        LOGE( "Failed to create command queue" );
        return FALSE;
    }

    if( FALSE == FrmBuildComputeProgramFromFile( "PostProcessCLGLES.cl", &m_clprogram, m_clcontext,
                                                 &m_devices[0], 1, "-cl-fast-relaxed-math" ) )
    {
        return FALSE;
    }

    m_kernel = clCreateKernel( m_clprogram, "SobelFilter" , &errNum );
    if ( errNum != CL_SUCCESS )
    {
        CHAR str[256];
        FrmSprintf( str, sizeof(str), "ERROR: Failed to create kernel 'SobelFilter'.\n" );
        LOGE("%s", str );
        return FALSE;
    }

    if(mFBOTexID == 0 || mTextures[0] == 0) {
        LOGE("not correct texture for cl memory");
        return FALSE;
    }

    if (m_sharedBuffer)
    {
        m_filteredTexMem = clCreateFromGLTexture2D( m_clcontext, CL_MEM_WRITE_ONLY, GL_TEXTURE_2D, 0, mTextures[0], &errNum );
        if( errNum != CL_SUCCESS )
        {
            FrmLogMessage( "Error creating OpenCL image object from GL texture." );
            return FALSE;
        }

        // use memory already allocated in GL context
        m_fboMem = clCreateFromGLTexture2D( m_clcontext, CL_MEM_READ_ONLY, GL_TEXTURE_2D, 0, mFBOTexID, &errNum );
        if( errNum != CL_SUCCESS )
        {
            LOGE( "Error creating OpenCL image object from GL FBO, err:%d",errNum );
            return FALSE;
        }
    }
    else // allocate new memory in cl context
    {
        cl_image_format image_format = {CL_RGBA, CL_UNORM_INT8};
        //CL 1.1
        m_fboMem = clCreateImage2D (m_clcontext, CL_MEM_READ_ONLY, &image_format, m_nFBOWidth, m_nFBOHeight, 0, 0, &errNum);
        if( errNum != CL_SUCCESS )
        {
            FrmLogMessage( "Error creating CL image object." );
            return FALSE;
        }

        //CL 1.1
        m_filteredTexMem = clCreateImage2D (m_clcontext, CL_MEM_WRITE_ONLY, &image_format, m_nFBOWidth, m_nFBOHeight, 0, 0, &errNum);
        if( errNum != CL_SUCCESS )
        {
            FrmLogMessage( "Error creating CL image object." );
            return FALSE;
        }

        if (!m_pHostBuf) { m_pHostBuf = new BYTE[m_nFBOWidth*m_nFBOHeight]; }
    }

    return TRUE;
}

int glrender::cl_post_process() {
    cl_int errNum;

    // Acquire the FBO and texture from OpenGL
    cl_mem memObjects[2] =
    {
        m_fboMem,
        m_filteredTexMem
    };
    size_t origin[] = {0, 0, 0};
    size_t region[] = {m_nFBOWidth, m_nFBOHeight, 1};

    if (m_sharedBuffer)
    {
        errNum = clEnqueueAcquireGLObjects( m_commandQueue, 2, &memObjects[0], 0, NULL, NULL );
        if( errNum != CL_SUCCESS )
        {
            LOGE( "Error acquiring FBO and texture to OpenCL memory object." );
            return FALSE;
        }
    }
    else
    {
        // copy host memory to CL
        errNum = clEnqueueWriteImage (m_commandQueue, m_fboMem,
                                      CL_TRUE,origin, region,
                                      0, 0, m_pHostBuf,
                                      0 ,    0 , 0);
        if( errNum != CL_SUCCESS )
        {
            LOGE( "Error writing image to buffer" );
            return FALSE;
        }
    }

    // Set the kernel arguments
    errNum |= clSetKernelArg( m_kernel, 0, sizeof(cl_mem), &m_fboMem );
    errNum |= clSetKernelArg( m_kernel, 1, sizeof(cl_mem), &m_filteredTexMem );
    errNum |= clSetKernelArg( m_kernel, 2, sizeof(cl_int), &m_nFBOWidth );
    errNum |= clSetKernelArg( m_kernel, 3, sizeof(cl_int), &m_nFBOHeight );
    if( errNum != CL_SUCCESS )
    {
        LOGE( "Error setting kernel arguments." );
        return FALSE;
    }

    // Launch the kernel to compute the vertices
    size_t globalWorkSize[2] = { m_nFBOWidth, m_nFBOHeight };
    errNum = clEnqueueNDRangeKernel( m_commandQueue, m_kernel, 2, NULL, globalWorkSize,
                                     NULL, 0, NULL, NULL );
    if( errNum != CL_SUCCESS )
    {
        LOGE( "Error queuing kernel for execution." );
        return FALSE;
    }

    // Finish executing kernel
    clFinish( m_commandQueue );

    if (m_sharedBuffer)
    {
        // Release the VBO back to OpenGLGL
        errNum = clEnqueueReleaseGLObjects( m_commandQueue, 2, &memObjects[0], 0, NULL, NULL );
        if( errNum != CL_SUCCESS )
        {
            LOGE( "Error releasing VBO and texture from OpenCL back to OpenGL." );
            return FALSE;
        }
    }
    else
    {
        // copy CL buffer back to host memory
        errNum = clEnqueueReadImage (m_commandQueue, m_filteredTexMem,
                                     CL_TRUE,origin, region,
                                     0, 0, m_pHostBuf,
                                     0, NULL, NULL);
        if( errNum != CL_SUCCESS )
        {
            LOGE( "Error reading image from buffer" );
            return FALSE;
        }
    }

    return TRUE;
}

int glrender::InitializeEgl() {
    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(display, 0, 0);

    EGLConfig config;
    EGLint numConfigs;
    EGLint format;
    EGLint configAttribs[] =
    {
        EGL_SURFACE_TYPE,		EGL_WINDOW_BIT,
        EGL_RED_SIZE,			5,
        EGL_GREEN_SIZE,	    	6,
        EGL_BLUE_SIZE,	    	5,
        EGL_DEPTH_SIZE,			16,
        EGL_STENCIL_SIZE,       8,
        EGL_RENDERABLE_TYPE,	EGL_OPENGL_ES2_BIT,
        EGL_NONE
    };

    eglChooseConfig(display, configAttribs, &config, 1, &numConfigs);
    eglGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_ID, &format);
    //ANativeWindow_setBuffersGeometry(m_pAndroidApp->window, 0, 0, format);

    //EGLSurface surface = eglCreateWindowSurface(display, config, m_pAndroidApp->window, NULL);

    EGLint contextAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE	};
    EGLContext context = eglCreateContext(display, config, NULL, contextAttribs);

#if 0
    if (eglMakeCurrent(display, surface, surface, context) == EGL_FALSE)
    {
        return FALSE;
    }
#endif

    m_eglDisplay = display;
    m_eglContext = context;
    //m_eglSurface = surface;

    return TRUE;
}

void glrender::setPostProcess(bool process) {
    mPostProcess = process;
    LOGI("turn %s cl post process", mPostProcess? "on":"off");
}

int glrender::checkError(const char *option) {
    int error = glGetError();
    if(error != GL_NO_ERROR) {
        LOGE("gl %s status error:%d", option, error);
        return error;
    }
    return 0;
}