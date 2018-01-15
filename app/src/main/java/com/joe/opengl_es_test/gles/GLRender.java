package com.joe.opengl_es_test.gles;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by joe on 12/01/2018.
 * Notice: all GLES related functions must be called inside GL thread
 *         so we must make calling in Render interfaces
 */

public class GLRender implements GLSurfaceView.Renderer {
    private final String TAG = "GLRender";

    private String vertexshadercode =
              "attribute vec4 vPosition;"
            + "void main() {"
            + "  gl_Position = vPosition;" //mainly to get vertext position
            + "}";

    private String fragmentshadercode =
             "precision mediump float;"
            +"uniform vec4 vColor;"
            + "void main() {"
            + "  gl_FragColor = vColor;" //mainly to get frament drawing color
            + "}";

    private float vertex[] = {
            -0.3f, -0.3f, 0.0f,
            0.5f, -0.3f, 0.0f,
            0.5f, 0.5f, 0.0f
    };

    private float color[] = { 0.0f, 1.0f, 0.0f, 1.0f }; //r,g,b, alpha

    private int VERTEX_COORD_SIZE = 3; //how many dimensions each vertex
    private int VERTEX_STRIDE = VERTEX_COORD_SIZE * 4; //each dimension is float, 4 bytes

    private ByteBuffer mVertexBuff = null;
    private ByteBuffer mColorBuff = null;
    private int mProgram = 0;
    private int mPostionHandle;
    private int mColorHandle;

    public GLRender() {
        init();
    }

    protected void init() {
        createbuff();
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


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        mPostionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        checkError("get position location");
        GLES20.glEnableVertexAttribArray(mPostionHandle);
        GLES20.glVertexAttribPointer(mPostionHandle, VERTEX_COORD_SIZE, GLES20.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuff);

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        checkError("get color location");
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3); //triangle have 3 points

        GLES20.glDisableVertexAttribArray(mPostionHandle); //disable at last
    }

    private int createbuff() {
        if(mVertexBuff == null) {
            mVertexBuff = ByteBuffer.allocateDirect(vertex.length * 4);
            mVertexBuff.order(ByteOrder.nativeOrder());
            mVertexBuff.asFloatBuffer().put(vertex);
            mVertexBuff.position(0);
        }

        if(mColorBuff == null) {
            mColorBuff = ByteBuffer.allocateDirect(color.length * 4);
            mColorBuff.order(ByteOrder.nativeOrder());
            mColorBuff.asFloatBuffer().put(color);
            mColorBuff.position(0);
        }

        return 0;
    }

    private int checkProgramBuilt() {
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

    private void checkError(String option) {
        int error = GLES20.glGetError();
        if(error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "***" + option + "error: " + error);
            throw new RuntimeException(option + "gl error" + error);
        }
    }

}
