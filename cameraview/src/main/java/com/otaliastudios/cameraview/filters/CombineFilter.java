package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.opengl.core.Egloo;
import com.otaliastudios.opengl.texture.GlFramebuffer;
import com.otaliastudios.opengl.texture.GlTexture;


public class CombineFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
        + "precision mediump float;\n"
        + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
        + "uniform samplerExternalOES sTexture;\n"
        + "uniform sampler2D overlayTex;\n"
        + "void main() {\n"
        + "  vec4 overlay = texture2D(overlayTex, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
        + "  gl_FragColor = mix(texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+"), overlay, overlay.a);\n"
        + "}\n";

    private int overlayLocation = -1, renderTargetIdx = 2;
    private GlTexture overlayTexture = null;
    private GlFramebuffer overlayBuffer = null;

    @NonNull
    @Override
    public String getFragmentShader() { return FRAGMENT_SHADER; }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);

        overlayLocation = GLES20.glGetUniformLocation(programHandle, "overlayTex");
        Egloo.checkGlProgramLocation(overlayLocation, "overlayTex");
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

        GLES20.glUniform1i(overlayLocation, renderTargetIdx);
        Egloo.checkGlError("glUniform1i");
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);

        release();
        init(width, height);
    }

    private void init(int w, int h){
        overlayBuffer = new GlFramebuffer();
        overlayBuffer.attach(overlayTexture = new GlTexture(GLES20.GL_TEXTURE0 + renderTargetIdx, GLES20.GL_TEXTURE_2D, w, h));
    }

    private void release(){
        if(overlayBuffer == null)
            return;

        overlayTexture.release();
        overlayTexture = null;
        overlayBuffer.release();
        overlayBuffer = null;
    }

    public void bindRenderBuffer(){
        overlayBuffer.bind();
    }
}
