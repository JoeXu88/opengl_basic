package com.joe.opengl_es_test.gles;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Created by david on 24/08/2018.
 */

public class GLNativeView extends GLSurfaceView {
    private final static String TAG = "GLNativeView";
    private GLNativeRender mGLRender = null;

    public GLNativeView(Context context) {
        this(context, null);
    }
    public GLNativeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int init(AssetManager mgr) {
        setEGLContextClientVersion(2);
        mGLRender = new GLNativeRender(mgr);
        mGLRender.SetPostProcess(true);
        //setEGLContextFactory(new ContextFactory());
        setRenderer(mGLRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        return 0;
    }

    public void uninit() {
        if(mGLRender != null)
            mGLRender.destroy();
    }

    private class ContextFactory implements GLSurfaceView.EGLContextFactory {
        private final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            Log.w(TAG, "creating OpenGL ES 2.0 context");
            checkEglError("Before eglCreateContext", egl);
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
            checkEglError("After eglCreateContext", egl);
            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            egl.eglDestroyContext(display, context);
        }

        private void checkEglError(String prompt, EGL10 egl) {
            int error;
            while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
                Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, error));
            }
        }
    }

}
