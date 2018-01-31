package com.joe.opengl_es_test.gles;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by joe on 12/01/2018.
 * Notice: all GLES related functions must be called inside GL thread
 *         so we must make calling in Render interfaces
 */

public abstract class GLRender implements GLSurfaceView.Renderer {
    private final String TAG = "GLRender";

    private String vertexshadercode;
    private String fragmentshadercode;
    protected Bitmap mbitmap;

    protected int mProgram = 0;

    public GLRender(String vetexshader, String fragshader) {
        this.vertexshadercode = vetexshader;
        this.fragmentshadercode = fragshader;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        checkProgramBuilt();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        checkProgramBuilt();
    }

    protected abstract int createbuff();

    protected int checkProgramBuilt() {
        if(mProgram != 0) {
            Log.d(TAG, "gl program built yet");
            return 0;
        }

        int vertexshader = loadshader(GLES20.GL_VERTEX_SHADER, vertexshadercode);
        int fragmentshader = loadshader(GLES20.GL_FRAGMENT_SHADER, fragmentshadercode);

        mProgram = GLES20.glCreateProgram();
        checkError("create program");
        GLES20.glAttachShader(mProgram, vertexshader);
        GLES20.glAttachShader(mProgram, fragmentshader);
        GLES20.glLinkProgram(mProgram);

        return 0;
    }

    private int loadshader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        checkError("create shader");
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        return shader;
    }

    protected void checkError(String option) {
        int error = GLES20.glGetError();
        if(error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "***" + option + "error: " + error);
            throw new RuntimeException(option + "gl error" + error);
        }
    }

    public void setImage(Bitmap bitmap) {
        this.mbitmap = bitmap;
    }

}
