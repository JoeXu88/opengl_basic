package com.joe.opengl_es_test;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.joe.opengl_es_test.gles.GLNativeView;
import com.joe.opengl_es_test.gles.GLView;

public class MainActivity extends AppCompatActivity {

    private GLView mGLView = null;
    private GLNativeView mGLNativeView = null;
    private AssetManager mAssetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAssetManager = getResources().getAssets();


        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        /**two ways to show gl render content**/
        //--1: setContentView directly
        //mGLView = new GLView(this);
        //mGLView.init(0);
        //setContentView(mGLView);

        //--2: put and get glview in actitity xml file
        /*
        mGLView = (GLView) findViewById(R.id.glview);
        //mGLView.init(utils.RenderType.BITMAP); //0--triangle, 1--texture
        //mGLView.setImage(BitmapFactory.decodeFile("sdcard/tf/persons/img_Angela_0.jpg"));
        mGLView.init();
        mGLView.setRenderer(new MultiRender());
        mGLView.update();
        if(NATIVE)
            mGLView.setVisibility(View.INVISIBLE);
        */

        mGLNativeView = (GLNativeView) findViewById(R.id.glnativeview);
        mGLNativeView.init(mAssetManager);
        /*******************/

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mGLView != null)
            mGLView.onResume();
        if(mGLNativeView != null)
            mGLNativeView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mGLView != null)
            mGLView.onPause();
        if(mGLNativeView != null)
            mGLNativeView.onPause();
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
