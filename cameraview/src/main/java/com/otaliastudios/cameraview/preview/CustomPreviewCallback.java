package com.otaliastudios.cameraview.preview;

public interface CustomPreviewCallback {
    void onCrop(float scaleX, float scaleY);
    void onSurfaceCreatedCallback();
    void onSurfaceDestroyedCallback();
    void onSurfaceChangedCallback(int w, int h);
    void onDrawFrameCallback();

//    void onSurfaceCreated();
//    void onSurfaceChanged(int w, int h);
//    void onDrawFrame();
}
