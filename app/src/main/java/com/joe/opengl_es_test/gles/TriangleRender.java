package com.joe.opengl_es_test.gles;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by joe on 31/01/2018.
 */

public class TriangleRender extends GLRender {

    private ByteBuffer mVertexBuff = null;
    private ByteBuffer mColorBuff = null;
    private int mPostionHandle;
    private int mColorHandle;
    private int VERTEX_COORD_SIZE = 3; //how many dimensions each vertex
    private int VERTEX_STRIDE = VERTEX_COORD_SIZE * 4; //each dimension is float, 4 bytes

    private float vertex[] = {
            -0.3f, -0.3f, 0.0f,
            0.5f, -0.3f, 0.0f,
            0.5f, 0.5f, 0.0f
    };

    private float color[] = { 0.0f, 0.0f, 1.0f, 0.5f }; //r,g,b, alpha

    public TriangleRender(String vetexshader, String fragshader) {
        super(vetexshader, fragshader);
        createbuff();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        mPostionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        checkError("get position location");
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        checkError("get color location");
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        GLES20.glEnableVertexAttribArray(mPostionHandle);
        GLES20.glVertexAttribPointer(mPostionHandle, VERTEX_COORD_SIZE, GLES20.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuff);

        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3); //triangle have 3 points

        GLES20.glDisableVertexAttribArray(mPostionHandle); //disable at last
    }

    @Override
    protected int createbuff() {
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
}
