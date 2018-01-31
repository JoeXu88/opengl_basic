package com.joe.opengl_es_test;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.joe.opengl_es_test.gles.GLView;
import com.joe.opengl_es_test.gles.utils;

public class MainActivity extends AppCompatActivity {

    private GLView mGLView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        /**two ways to show gl render content**/
        //--1: setContentView directly
        //mGLView = new GLView(this);
        //mGLView.init(0);
        //setContentView(mGLView);

        //--2: put and get glview in actitity xml file
        mGLView = (GLView) findViewById(R.id.glview);
        mGLView.init(utils.RenderType.BITMAP); //0--triangle, 1--texture
        mGLView.setImage(BitmapFactory.decodeFile("sdcard/tf/persons/img_Angela_0.jpg"));
        mGLView.update();
        /*******************/

    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    /**
     * Add a native method, prepared for JNI calling...
     * Currently, no use here
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
