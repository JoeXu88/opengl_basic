package com.joe.opengl_es_test.gles;

import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by joe on 02/02/2018.
 * Using FBO to render multi things
 */

public class MultiRender extends GLRender{
    private TriangleRender mtriangle = null;
    private FBORender mfbo = null;
    private TextureRender mshow = null;
    private TextureRender mbackground = null;

    public MultiRender() {
        mtriangle = new TriangleRender(utils.ShaderProgram.simpleVetexShaderProg,
                                        utils.ShaderProgram.simpleFragShaderProg);
        mfbo = new FBORender();
        mshow = new TextureRender();
        mbackground = new TextureRender();
        mbackground.setImage(BitmapFactory.decodeFile("sdcard/tf/persons/img_Angela_0.jpg"));

        mfbo.setRenderSrc(mtriangle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mbackground.onSurfaceChanged(gl, width, height);
        mfbo.onSurfaceChanged(gl, width, height);
        mshow.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mbackground.onSurfaceCreated(gl, config);
        mfbo.onSurfaceCreated(gl, config);
        mshow.onSurfaceCreated(gl, config);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mbackground.onDrawFrame(gl);
        mfbo.setClearScreen(false);
        mfbo.onDrawFrame(gl);
        mshow.setExtTexID(mfbo.getFBOTexutre());
        GLES20.glViewport(300, 200, 500, 500);
        mshow.setClearScreen(false);
        mshow.onDrawFrame(gl);
        GLES20.glViewport(0, 0, mshow.getWidth(), mshow.getHeight());
    }
}
