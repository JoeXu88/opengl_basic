package com.joe.opengl_es_test.gles;

import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by david on 24/08/2018.
 */

public class GLNativeRender implements GLSurfaceView.Renderer {
    final String TAG = "GLNativeRender";
    AssetManager mAssetManager;
    public GLNativeRender(AssetManager mgr) {
        mAssetManager = mgr;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG,"onSurfaceCreated");
        uinit();
        init(320 ,240, mAssetManager);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d(TAG, "onDrawFrame");
        if(drawYUV() != 0) {
            destroy();
            Log.e("native render","render error");
        }
    }

    public void SetPostProcess(boolean process) {
        setPostProcess(process);
    }

    public void destroy() {
        uinit();
    }

    private native int init(int w, int h, AssetManager mgr);
    private native int drawYUV();
    private native void uinit();
    private native void updateReso(int w, int h);
    private native void setPostProcess(boolean process);
}
