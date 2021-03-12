package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.opengl.core.Egloo;
import com.otaliastudios.opengl.texture.GlFramebuffer;
import com.otaliastudios.opengl.texture.GlTexture;


public class CombineFilter extends BaseFilter {

    private static CombineFilter latest_instance;

    public static void setCropScale(float x, float y) {
        if(latest_instance != null)
            latest_instance.setCropScale(new float[]{x, y});
    }

    public static void bindRenderBuffer(){
        if(latest_instance != null)
            latest_instance.bind();
    }

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n" +
        "uniform samplerExternalOES sTexture;\n" +
        "uniform sampler2D overlayTex;\n" +
        "uniform vec2 cropScale;\n" +
        "vec2 rotate(vec2 v, float a){\n" +
        "  float s = sin(radians(a));\n" +
        "  float c = cos(radians(a));\n" +
        "  return (mat2(c,s,-s,c) * (v - 0.5)) / cropScale + 0.5;\n" +
        "}\n" +
        "void main(){\n" +
        "  vec4 overlay = texture2D(overlayTex, rotate("+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+", 90.0));\n" +
        "  vec4 src = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n" +
//        "  if(all(lessThanEqual("+ DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME +", vec2(0.5, 0.5)))){\n" +
//        "  if(false){\n" +
        "    gl_FragColor = mix(src, overlay, overlay.a);\n" +
//        "    gl_FragColor = mix(overlay, src, src.a);\n" +
//        "  }else{\n" +
//        "    gl_FragColor = src;\n" +
//        "  }\n" +
        "}\n";

    private int overlayLocation = -1, cropScaleLocation = -1, renderTargetIdx = 1;
    private GlTexture overlayTexture = null;
    private GlFramebuffer overlayBuffer = null;

    private float[] cropScale = new float[]{1, 1};
    public void setCropScale(float[] newScale) { cropScale = newScale; }

    public CombineFilter(){
        if(latest_instance != null)
            latest_instance.onDestroy();

        latest_instance = this;
    }

    @NonNull
    @Override
    public String getFragmentShader() { return FRAGMENT_SHADER; }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);

        overlayLocation = GLES20.glGetUniformLocation(programHandle, "overlayTex");
        Egloo.checkGlProgramLocation(overlayLocation, "overlayTex");

        cropScaleLocation = GLES20.glGetUniformLocation(programHandle, "cropScale");
        Egloo.checkGlProgramLocation(overlayLocation, "cropScale");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        release();
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);

        overlayTexture.bind();

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

//        GLES20.glClearColor(0,0,0,0);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glUniform1i(overlayLocation, renderTargetIdx);
        Egloo.checkGlError("glUniform1i");

        GLES20.glUniform2fv(cropScaleLocation, 1, cropScale, 0);
        Egloo.checkGlError("glUniform2fv");
    }

//    @Override
//    protected void onPostDraw(@SuppressWarnings("unused") long timestampUs){
//        super.onPostDraw(timestampUs);
//
//        GLES20.glDisable(GLES20.GL_BLEND);
//    }

    @Override
    public void setSize(int width, int height) {
        if(width > 0 && height > 0 && (size == null || size.getWidth() != width || size.getHeight() != height))
            init(width, height);

        super.setSize(width, height);
    }

    private void init(int w, int h){
        if(overlayBuffer != null)
            release();

        overlayBuffer = new GlFramebuffer();
        overlayBuffer.attach(overlayTexture = new GlTexture(GLES20.GL_TEXTURE0 + renderTargetIdx, GLES20.GL_TEXTURE_2D, w, h));
    }

    private void release(){
        if(overlayBuffer == null) return;

        overlayTexture.release();
        overlayTexture = null;
        overlayBuffer.release();
        overlayBuffer = null;
    }

    public void bind(){
        if(overlayBuffer == null) return;

        overlayBuffer.bind();
    }
}
