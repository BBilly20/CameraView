package com.otaliastudios.cameraview.preview;

public interface RendererCallbacks {
    void onSurfaceCreatedCallback();
    void onSurfaceChangedCallback(int w, int h);
    void onDrawFrameCallback();
}
