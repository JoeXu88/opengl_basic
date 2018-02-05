package com.joe.opengl_es_test.gles;

/**
 * Created by david on 31/01/2018.
 */

public class utils {

    public class ShaderProgram {
        public ShaderProgram(){}

        /** basic render for vertex specified **/
        static final String simpleVetexShaderProg = "attribute vec4 vPosition;"
                + "uniform mat4 vMatrix;"
                + "void main() {"
                + "  gl_Position = vPosition;" //mainly to get vertext position
                + "}";

        static final String simpleFragShaderProg =
                "precision mediump float;"
                +"uniform vec4 vColor;"
                + "void main() {"
                + "  gl_FragColor = vColor;" //mainly to get frament drawing color
                + "}";
    /*-----------------------------------------*/

        /** texture render for bitmap rgb data **/
        static final String texVetexShaderProg = "attribute vec4 vPosition;"
                +"uniform mat4 vMatrix;"
                +"attribute vec2 vCoordinate;"
                +"varying vec2 aCoordinate;"
                + "void main() {"
                + "  gl_Position = vMatrix * vPosition;" //mainly to get vertext position
                + "  aCoordinate = vCoordinate;"
                + "}";

        static final String texFragShaderProg = "precision mediump float;"
                +"uniform sampler2D vTexture;"
                +"uniform sampler2D vAlphaTexture;"
                +"varying vec2 aCoordinate;"
                + "void main() {"
                + "  vec4 color = texture2D(vTexture,aCoordinate);"
                + "  gl_FragColor = color;" //mainly to get frament drawing color
                + "}";
    /*-----------------------------------------*/
    }

    public class RenderType {
        public RenderType() {}

        public static final int TRIANGLE   = 1;
        public static final int BITMAP     = 2;
        public static final int YUV        = 3;
    }

}
