package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.opengl.core.Egloo;
import com.otaliastudios.opengl.texture.GlFramebuffer;
import com.otaliastudios.opengl.texture.GlTexture;


public class CombineFilter extends BaseFilter {

//    private static CombineFilter latest_instance;

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
//        "  if(true){\n" +
        "    gl_FragColor = mix(src, overlay, overlay.a);\n" + //***
//        "    gl_FragColor = mix(overlay, src, src.a);\n" +
//        "    gl_FragColor = vec4(src.xyz, overlay.a);\n" +
//        "  }else{\n" +
//        "    gl_FragColor = src;\n" +
//        "  }\n" +
        "}\n";

    private int overlayLocation = -1, cropScaleLocation = -1, renderTargetIdx = 1;

//    private static int width, height, count = 0;
//    private static boolean initialized = false;
    private static GlTexture overlayTexture = null;
    private static GlFramebuffer overlayBuffer = null;

    private static float[] cropScale = new float[]{1, 1};
    public static void setCropScale(float x, float y) { cropScale = new float[]{x, y}; }

//    public CombineFilter(){ }

    @NonNull
    @Override
    public String getFragmentShader() { return FRAGMENT_SHADER; }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);

//        count++;

        overlayLocation = GLES20.glGetUniformLocation(programHandle, "overlayTex");
        Egloo.checkGlProgramLocation(overlayLocation, "overlayTex");

        cropScaleLocation = GLES20.glGetUniformLocation(programHandle, "cropScale");
        Egloo.checkGlProgramLocation(cropScaleLocation, "cropScale");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        if(--count == 0)
//            release();
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

    @Override
    protected void onPostDraw(@SuppressWarnings("unused") long timestampUs){
        super.onPostDraw(timestampUs);

        overlayTexture.unbind();

        GLES20.glDisable(GLES20.GL_BLEND);
    }

//    @Override
//    public void setSize(int w, int h) {
//        if(w > 0 && h > 0 && (w != width || h != height))
//            init(w, h);
//
//        super.setSize(w, h);
//    }

    @Override
    public void setSize(int width, int height) {
        if(width > 0 && height > 0 && (this.size == null || this.size.getWidth() != width || this.size.getHeight() != height))
            init(width, height);

        super.setSize(width, height);
    }

    private void init(int width, int height){
//        width = w;
//        height = h;

        if(overlayBuffer != null)
            release();

        overlayTexture = new GlTexture(GLES20.GL_TEXTURE0 + renderTargetIdx, GLES20.GL_TEXTURE_2D, width, height);

        overlayBuffer = new GlFramebuffer();
        overlayBuffer.attach(overlayTexture);

        overlayBuffer.bind();

        int[] bufferHandles = new int[1];
        GLES20.glGenRenderbuffers(1, bufferHandles, 0);
        Egloo.checkGlError("glGenRenderbuffers");
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, bufferHandles[0]);
        Egloo.checkGlError("glBindRenderbuffer");
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
        Egloo.checkGlError("glRenderbufferStorage");
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, bufferHandles[0]);
        Egloo.checkGlError("glFramebufferRenderbuffer");

        overlayBuffer.unbind();
    }

    public static void bindRenderBuffer(){
        if(overlayBuffer != null)
            overlayBuffer.bind();
    }

    public static void unbindRenderBuffer(){
        if(overlayBuffer != null)
            overlayBuffer.unbind();
    }

    private void release(){
        if(overlayBuffer == null) return;

        overlayTexture.release();
        overlayTexture = null;
        overlayBuffer.release();
        overlayBuffer = null;
    }
}
