package com.joe.opengl_es_test.gles;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created by joe on 12/01/2018.
 */

public class GLView extends GLSurfaceView{

    private GLRender mGLRender;

    public GLView(Context context) {
        this(context, null);
    }

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mGLRender = new GLRender();
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        setRenderer(mGLRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void update() {
        //update when dirty
        requestRender();
    }
}
