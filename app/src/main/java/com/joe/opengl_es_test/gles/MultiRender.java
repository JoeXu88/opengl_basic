package com.joe.opengl_es_test.gles;

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

    public MultiRender() {
        mtriangle = new TriangleRender(utils.ShaderProgram.simpleVetexShaderProg,
                                        utils.ShaderProgram.simpleFragShaderProg);
        mfbo = new FBORender();
        mshow = new TextureRender();

        mfbo.setRenderSrc(mtriangle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mfbo.onSurfaceChanged(gl, width, height);
        mshow.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mfbo.onSurfaceCreated(gl, config);
        mshow.onSurfaceCreated(gl, config);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mfbo.onDrawFrame(gl);
        mshow.setExtTexID(mfbo.getFBOTexutre());
        mshow.onDrawFrame(gl);
    }
}
