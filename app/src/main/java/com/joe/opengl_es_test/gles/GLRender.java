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

    //basic information
    private String vertexshadercode;
    private String fragmentshadercode;
    protected Bitmap mbitmap;
    protected int   mwidth;
    protected int   mheight;
    private boolean mClear = true;

    //for pipeline creating
    protected int   mProgram = 0;
    protected int   mPostionHandle;
    protected int   mTexcordHandle;
    protected int   mTexHandle;
    protected int   mMatrixHandle;
    private int     mTextureID = 0;

    //for display matrix
    private float[] mMatrix;
    private int     mViewType = MatrixUtils.TYPE_FITXY;
    private int     mRotateAngle = 0;
    private boolean mFlipx = false;
    private boolean mFlipy = false;


    //for vertex and corrd
    private ByteBuffer mVertex = null;
    private ByteBuffer mTexCoord = null;
    private float[] vertices =  {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,}; // fullscreen
    private float[] texcoord = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,};// whole-texture

    public GLRender(String vetexshader, String fragshader) {
        this.vertexshadercode = vetexshader;
        this.fragmentshadercode = fragshader;
        mMatrix = MatrixUtils.getOriginMatrix();
    }

    public GLRender() {
        this.vertexshadercode = utils.ShaderProgram.texVetexShaderProg;
        this.fragmentshadercode = utils.ShaderProgram.texFragShaderProg;
        mMatrix = MatrixUtils.getOriginMatrix();
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
        onBegin();
        onSetTextureSrc();
        onSetMatrix();
        onBindTextureID();
        onDraw();
        onEnd();
    }

    protected void onClearScreen() {
        if(mClear) {
            GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        }
    }

    protected void onSetTextureSrc() {

    }

    private void onBegin() {
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPostionHandle);
        GLES20.glEnableVertexAttribArray(mTexcordHandle);
    }

    private void onEnd() {
        GLES20.glFinish();
        GLES20.glDisableVertexAttribArray(mPostionHandle);
        GLES20.glDisableVertexAttribArray(mTexcordHandle);
        GLES20.glUseProgram(0);
    }

    protected void onSetMatrix(){
        if(mbitmap != null)
            MatrixUtils.getDisplayMatrix(mMatrix, MatrixUtils.TYPE_CENTERINSIDE, mbitmap.getWidth(), mbitmap.getHeight(),mwidth,mheight);
        else
            MatrixUtils.getDisplayMatrix(mMatrix, MatrixUtils.TYPE_FITXY);
        MatrixUtils.flip(mMatrix, mFlipx, mFlipy);
        if(mRotateAngle != 0)
            MatrixUtils.rotate(mMatrix, mRotateAngle);
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0);
    }

    /**
    ** only bind one texture here, if want to bind multi texture need to override this function
    **/
    protected void onBindTextureID() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
        GLES20.glUniform1i(mTexHandle, 0);
    }

    protected void onDraw() {
        GLES20.glVertexAttribPointer(mPostionHandle, 2, GLES20.GL_FLOAT, false, 8, mVertex);
        GLES20.glVertexAttribPointer(mTexcordHandle, 2, GLES20.GL_FLOAT, false, 8, mTexCoord);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
    }

    protected void onGetHandles() {
        mPostionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        checkError("get position location");
        mTexHandle = GLES20.glGetUniformLocation(mProgram,"vTexture");
        checkError("get texture location");
        mTexcordHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        checkError("get texcord location");
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram,"vMatrix");
        checkError("get matrix location");
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

    public void setClearScreen(boolean clear) {
        mClear = clear;
    }

    public void setMatrix(float[] matrix) {
        mMatrix = matrix;
    }

    public float[] getMatrix() {
        return mMatrix;
    }

    public void setRotate(int angle) {
        mRotateAngle = angle;
    }

    public void setFlip(boolean x, boolean y) {
        mFlipx = x;
        mFlipy = y;
    }

    public void setViewType(int viewType) {
        mViewType = viewType;
    }

}
