package com.otaliastudios.cameraview.preview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.filter.MultiFilter;
import com.otaliastudios.cameraview.filter.NoFilter;
import com.otaliastudios.cameraview.filters.CombineFilter;
import com.otaliastudios.cameraview.internal.GlTextureDrawer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ARPreview extends GlCameraPreview {

    private CombineFilter combineFilter = null;

    public ARPreview(@NonNull Context context, @NonNull ViewGroup parent) {
        super(context, parent);
    }

    @NonNull
    @Override
    protected Renderer instantiateRenderer() {
        return new Renderer();
    }

    @NonNull
    @Override
    protected GLSurfaceView onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        GLSurfaceView v = super.onCreateView(context, parent);

        v.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        return v;
    }

    @Override
    public void setFilter(final @NonNull Filter filter) {
        super.setFilter(prepareFilter(filter));
    }

    private Filter prepareFilter(Filter filter){
        MultiFilter f = filter instanceof MultiFilter ? (MultiFilter) filter : new MultiFilter(filter);

        if(combineFilter == null)
            combineFilter = new CombineFilter();

        f.insertFilter(combineFilter);

        return f;
    }

    public class Renderer extends GlCameraPreview.Renderer implements RendererCallbacks {
        @RendererThread
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            if (mCurrentFilter == null) {
                mCurrentFilter = prepareFilter(new NoFilter());
            }
            mOutputTextureDrawer = new GlTextureDrawer();
            mOutputTextureDrawer.setFilter(mCurrentFilter);
            final int textureId = mOutputTextureDrawer.getTexture().getId();
            mInputSurfaceTexture = new SurfaceTexture(textureId);
            getView().queueEvent(new Runnable() {
                @Override
                public void run() {
                    for (RendererFrameCallback callback : mRendererFrameCallbacks)
                        callback.onRendererTextureCreated(textureId);
                }
            });

            GLES20.glClearColor(0,0,0,0);

            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            onSurfaceCreatedCallback();
        }

        @Override
        public void onSurfaceChanged(GL10 glUnused, int width, int height)
        {
            super.onSurfaceChanged(glUnused, width, height);

            onSurfaceChangedCallback(width, height);
        }

        @RendererThread
        @Override
        public void onDrawFrame(GL10 gl) {
            if (mInputSurfaceTexture == null || mInputStreamWidth <= 0 || mInputStreamHeight <= 0) return;

            final float[] transform = mOutputTextureDrawer.getTextureTransform();
            mInputSurfaceTexture.updateTexImage();
            mInputSurfaceTexture.getTransformMatrix(transform);

            if (mDrawRotation != 0) {
                Matrix.translateM(transform, 0, 0.5F, 0.5F, 0);
                Matrix.rotateM(transform, 0, mDrawRotation, 0, 0, 1);
                Matrix.translateM(transform, 0, -0.5F, -0.5F, 0);
            }

            if (isCropping()) {
                combineFilter.setCropScale(mCropScaleX, mCropScaleY);

                Matrix.translateM(transform, 0, (1F - mCropScaleX) / 2F, (1F - mCropScaleY) / 2F, 0);
                Matrix.scaleM(transform, 0, mCropScaleX, mCropScaleY, 1);
            }

            combineFilter.bindRenderBuffer();
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            onDrawFrameCallback();

            mOutputTextureDrawer.draw(mInputSurfaceTexture.getTimestamp() / 1000L);

            for (RendererFrameCallback callback : mRendererFrameCallbacks) {
                callback.onRendererFrame(mInputSurfaceTexture, mDrawRotation, mCropScaleX, mCropScaleY);
            }
        }

        public void onSurfaceCreatedCallback(){ }

        public void onSurfaceChangedCallback(int w, int h){ }

        public void onDrawFrameCallback(){ }
    }
}