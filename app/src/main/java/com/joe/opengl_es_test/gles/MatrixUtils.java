package com.joe.opengl_es_test.gles;

import android.opengl.Matrix;

/**
 * Created by joe on 05/02/2018.
 */

public class MatrixUtils {
    public static final int TYPE_FITXY=0;
    public static final int TYPE_CENTERCROP=1;
    public static final int TYPE_CENTERINSIDE=2;
    public static final int TYPE_FITSTART=3;
    public static final int TYPE_FITEND=4;

    public MatrixUtils() {}

    public static void rotate(float[] mat, int angle) {
        Matrix.rotateM(mat, 0, angle, 0, 0, 1);
    }

    public static void flip(float[] mat, boolean x, boolean y) {
        if(x || y)
            Matrix.scaleM(mat,0, x?-1:1, y?-1:1,1);
    }

    public static void scale(float[] mat, float x, float y) {
        Matrix.scaleM(mat,0, x, y,1);
    }

    public static float[] getOriginMatrix() {
        return new float[]{
                1,0,0,0,
                0,1,0,0,
                0,0,1,0,
                0,0,0,1
        };
    }

    public static void getDisplayMatrix(float[] mat, int type) {
        getDisplayMatrix(mat, type, -1, -1, -1, -1);
    }

    public static void getDisplayMatrix(float[] mat, int type, int imgWidth, int imgHeight, int viewWidth,
                                        int viewHeight) {
        float[] projection=new float[16];  //for orthographic
        float[] camera=new float[16];      //for view direction
        if(type==TYPE_FITXY){
            Matrix.orthoM(projection,0,-1,1,-1,1,1,3);
            Matrix.setLookAtM(camera,0,0,0,1,0,0,0,0,1,0);
            Matrix.multiplyMM(mat,0,projection,0,camera,0);
        }
        else if(imgHeight>0 && imgWidth>0 && viewWidth>0 && viewHeight>0){
            /* change projection matrix */
            float sWhView=(float)viewWidth/viewHeight;
            float sWhImg=(float)imgWidth/imgHeight;
            if(sWhImg>sWhView){
                switch (type){
                    case TYPE_CENTERCROP:
                        Matrix.orthoM(projection,0,-sWhView/sWhImg,sWhView/sWhImg,-1,1,1,3);
                        break;
                    case TYPE_CENTERINSIDE:
                        Matrix.orthoM(projection,0,-1,1,-sWhImg/sWhView,sWhImg/sWhView,1,3);
                        break;
                    case TYPE_FITSTART:
                        Matrix.orthoM(projection,0,-1,1,1-2*sWhImg/sWhView,1,1,3);
                        break;
                    case TYPE_FITEND:
                        Matrix.orthoM(projection,0,-1,1,-1,2*sWhImg/sWhView-1,1,3);
                        break;
                }
            }else{
                switch (type){
                    case TYPE_CENTERCROP:
                        Matrix.orthoM(projection,0,-1,1,-sWhImg/sWhView,sWhImg/sWhView,1,3);
                        break;
                    case TYPE_CENTERINSIDE:
                        Matrix.orthoM(projection,0,-sWhView/sWhImg,sWhView/sWhImg,-1,1,1,3);
                        break;
                    case TYPE_FITSTART:
                        Matrix.orthoM(projection,0,-1,2*sWhView/sWhImg-1,-1,1,1,3);
                        break;
                    case TYPE_FITEND:
                        Matrix.orthoM(projection,0,1-2*sWhView/sWhImg,1,-1,1,1,3);
                        break;
                }
            }

            //update matrix
            Matrix.setLookAtM(camera,0,0,0,1,0,0,0,0,1,0);
            Matrix.multiplyMM(mat,0,projection,0,camera,0);
        }
    }
}
