package com.joe.opengl_es_test.gles;

import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by joe on 29/01/2018.
 */

public class TextureRender extends GLRender {

    private final String TAG = "TextureRender";

    private ByteBuffer mVertex = null;
    private ByteBuffer mTexCoord = null;
    private int mPostionHandle;
    private int mTexcordHandle;
    private int mTexHandle;

    private static float[] vertices =  {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,}; // fullscreen
    private static float[] texcoord = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,};// whole-texture


    public TextureRender(String vetexshader, String fragshader) {
        super(vetexshader, fragshader);
        createbuff();
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        mPostionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        checkError("get position location");
        mTexHandle = GLES20.glGetUniformLocation(mProgram,"vTexture");
        checkError("get texture location");
        mTexcordHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        checkError("get texcord location");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);


        GLES20.glEnableVertexAttribArray(mPostionHandle);
        GLES20.glVertexAttribPointer(mPostionHandle, 2, GLES20.GL_FLOAT, false, 8, mVertex);

        GLES20.glEnableVertexAttribArray(mTexcordHandle);
        GLES20.glVertexAttribPointer(mTexcordHandle, 2, GLES20.GL_FLOAT, false, 8, mTexCoord);

        GLES20.glUniform1i(mTexHandle, 0);

        createTexture();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);

        GLES20.glFinish();
        GLES20.glDisableVertexAttribArray(mPostionHandle);
        GLES20.glDisableVertexAttribArray(mTexcordHandle);
    }

    private int createTexture() {
        int texture[] = new int[1];
        if(mbitmap != null && !mbitmap.isRecycled()) {
            GLES20.glGenTextures(1, texture, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mbitmap, 0);

            return texture[0];
        }
        else {
            Log.e(TAG, "bitmap error");
        }

        return 0;
    }

    @Override
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

}
