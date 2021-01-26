package com.otaliastudios.cameraview.preview;

public interface RendererCallbacks {
    void onSurfaceCreated();
    void onSurfaceChanged(int w, int h);
    void onDrawFrame();
}
