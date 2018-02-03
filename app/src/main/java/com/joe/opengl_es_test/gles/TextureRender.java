package com.joe.opengl_es_test.gles;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Created by joe on 29/01/2018.
 * Basic bitmap render
 */

public class TextureRender extends GLRender {

    private final String TAG = "TextureRender";
    private int mTexID = -1;


    public TextureRender(String vetexshader, String fragshader) {
        super(vetexshader, fragshader);
        createbuff();
    }

    public TextureRender() {
        createbuff();
    }

    public void setExtTexID(int id) {
        //Log.d(TAG, "got ext texture id: "+id);
        mTexID = id;
    }

    @Override
    public void onSetTextureSrc() {
        if(mTexID == -1) {
            mTexID = genTexture(mbitmap);
        }

        setTextureID(mTexID);
    }

    private int genTexture(Bitmap bitmap) {
        if(bitmap != null && !bitmap.isRecycled()) {
            int texture[] = new int[1];
            GLES20.glGenTextures(1, texture, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            return texture[0];
        }
        else {
            Log.e(TAG, "bitmap error");
        }

        return 0;
    }

}
