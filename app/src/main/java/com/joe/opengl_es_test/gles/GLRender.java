package com.joe.opengl_es_test.gles;

import android.graphics.Bitmap;
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

public abstract class GLRender implements GLSurfaceView.Renderer {
    private final String TAG = "GLRender";

    private String vertexshadercode;
    private String fragmentshadercode;
    protected Bitmap mbitmap;
    protected int mwidth;
    protected int mheight;

    protected int mProgram = 0;
    private int mTextureID = 0;
    protected int mPostionHandle;
    protected int mTexcordHandle;
    protected int mTexHandle;

    private ByteBuffer mVertex = null;
    private ByteBuffer mTexCoord = null;
    private static float[] vertices =  {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,}; // fullscreen
    private static float[] texcoord = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,};// whole-texture

    public GLRender(String vetexshader, String fragshader) {
        this.vertexshadercode = vetexshader;
        this.fragmentshadercode = fragshader;
    }

    public GLRender() {
        this.vertexshadercode = utils.ShaderProgram.texVetexShaderProg;
        this.fragmentshadercode = utils.ShaderProgram.texFragShaderProg;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        checkProgramBuilt();
        mwidth = width;
        mheight = height;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        checkProgramBuilt();
        onGetHandles();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        onClearScreen();
        onSetTextureSrc();
        onBindTextureID(mTextureID);
        onDraw();
    }

    protected void onClearScreen() {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
    }

    protected void onSetTextureSrc() {

    }

    protected void onBindTextureID(int texture) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(mTexHandle, 0);
    }

    protected void onDraw() {
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPostionHandle);
        GLES20.glVertexAttribPointer(mPostionHandle, 2, GLES20.GL_FLOAT, false, 8, mVertex);
        GLES20.glEnableVertexAttribArray(mTexcordHandle);
        GLES20.glVertexAttribPointer(mTexcordHandle, 2, GLES20.GL_FLOAT, false, 8, mTexCoord);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        //GLES20.glFinish();
        GLES20.glDisableVertexAttribArray(mPostionHandle);
        GLES20.glDisableVertexAttribArray(mTexcordHandle);
    }

    protected void onGetHandles() {
        mPostionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        checkError("get position location");
        mTexHandle = GLES20.glGetUniformLocation(mProgram,"vTexture");
        checkError("get texture location");
        mTexcordHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        checkError("get texcord location");
    }

    protected int createbuff() {
        if(mVertex == null) {
            mVertex = ByteBuffer.allocateDirect(vertices.length * 4);
            mVertex.order(ByteOrder.nativeOrder());
            mVertex.asFloatBuffer().put(vertices);
            mVertex.position(0);
        }

        if(mTexCoord == null) {
            mTexCoord = ByteBuffer.allocateDirect(texcoord.length * 4);
            mTexCoord.order(ByteOrder.nativeOrder());
            mTexCoord.asFloatBuffer().put(texcoord);
            mTexCoord.position(0);
        }

        return 0;
    }

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

    public void setTextureID(int id) {
        mTextureID = id;
    }

    public int getWidth() {
        return mwidth;
    }

    public int getHeight() {
        return mheight;
    }

}
