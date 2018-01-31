package com.joe.opengl_es_test.gles;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by joe on 12/01/2018.
 */

public class GLView extends GLSurfaceView{

    private GLRender mGLRender = null;

    public GLView(Context context) {
        this(context, null);
    }

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(int rendertype) {
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);

        if(rendertype == utils.RenderType.TRIANGLE) {
            mGLRender = new TriangleRender(utils.ShaderProgram.simpleVetexShaderProg, utils.ShaderProgram.simpleFragShaderProg);
        }
        else if(rendertype == utils.RenderType.BITMAP){
            mGLRender = new TextureRender(utils.ShaderProgram.texVetexShaderProg, utils.ShaderProgram.texFragShaderProg);
        }

        setRenderer(mGLRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setImage(Bitmap bitmap) {
        if(mGLRender != null) {
            mGLRender.setImage(bitmap);
        }
        else {
            Log.e("GLView", "wrong status for setting");
        }
    }

    public void update() {
        //update when dirty
        requestRender();
    }
}
