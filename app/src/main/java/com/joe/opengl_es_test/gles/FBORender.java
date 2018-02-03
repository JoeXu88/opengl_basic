package com.joe.opengl_es_test.gles;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by joe on 01/02/2018.
 * This file shows frame buffer usage
 */

public class FBORender extends GLRender{
    private final String TAG = "FBORender";

    private GLRender mSrcRnd = null;
    private int[] mfFrame = new int[1];
    private int[] mfTexture = new int[1];
    private ByteBuffer mPixelBuffer;
    private boolean msavepixel = false;

    public FBORender() {
        mfFrame[0] = -1;
        mfTexture[0] = -1;
    }

    public void setRenderSrc(@NonNull GLRender src) {
        mSrcRnd = src;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if(mSrcRnd != null)
            mSrcRnd.onSurfaceChanged(gl, width, height);

        if(msavepixel)
            mPixelBuffer = ByteBuffer.allocate(width*height*4);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if(mSrcRnd != null)
            mSrcRnd.onSurfaceCreated(gl, config);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        boolean en_depth = GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST);
        if(en_depth){
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }
        genFrameTexture();
        bindFrameTexture();
        mSrcRnd.onDrawFrame(gl);

        if(msavepixel) {
            GLES20.glReadPixels(0, 0, mSrcRnd.getWidth(), mSrcRnd.getHeight(), GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, mPixelBuffer);
            Log.d(TAG, "pixel size: " + mPixelBuffer.position());
            Bitmap bitmap = Bitmap.createBitmap(mSrcRnd.getWidth(), mSrcRnd.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(mPixelBuffer);
            saveBitmap(bitmap);
            mPixelBuffer.clear();
        }

        unBindFrameTexture();
        if (en_depth) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
    }

    private int[] genTexture(int n) {
        int texture[] = new int[n];
        for (int i=0; i<n; i++) {
            GLES20.glGenTextures(n, texture, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[i]);
            /* set texture params */
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        }

        return texture;
    }

    public int getFBOTexutre() {
        return mfTexture[0];
    }

    private void genFrameTexture() {
        /* if already generated, return directly */
        if(mfTexture[0] != -1 && mfFrame[0] != -1)
            return;

        /* check if need to delete first */
        deleteFrameBuffer();
        GLES20.glGenFramebuffers(1, mfFrame, 0);
        GLES20.glGenTextures(1, mfTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mfTexture[0]);
        /* set default data to null, left for dynamic data render */
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mSrcRnd.getWidth(), mSrcRnd.getHeight(),
                            0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        /* set texture params */
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        /* reset back texture bind */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void bindFrameTexture(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mfFrame[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mfTexture[0], 0);
    }

    private void unBindFrameTexture(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
    }

    private void deleteFrameBuffer() {
        if(mfFrame[0] != -1) {
            GLES20.glDeleteFramebuffers(1, mfFrame, 0);
            mfFrame[0] = -1;
        }

        if(mfTexture[0] != -1) {
            GLES20.glDeleteTextures(1, mfTexture, 0);
            mfTexture[0] = -1;
        }
    }

    public void saveBitmap(final Bitmap b){
        String path = "/sdcard/";
        File folder=new File(path);
        if(!folder.exists()&&!folder.mkdirs()){
            Log.e(TAG, "can not open file dir "+path);
            return;
        }
        final String jpegName=path +"fbo.jpg";
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void setClearScreen(boolean clear) {
        if(mSrcRnd != null)
            mSrcRnd.setClearScreen(clear);
        else
            super.setClearScreen(clear);
    }
}
