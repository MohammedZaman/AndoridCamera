package com.example.mohammedzaman.andoridcamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.util.List;

public class ImageSurfaceView extends SurfaceView implements SurfaceHolder.Callback,Camera.PreviewCallback {

    SurfaceHolder mHolder;
    Camera mCamera;




    public ImageSurfaceView (Context context) {
        super(context);
        init(context);

    }




    public ImageSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context context){

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }





    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
            // TODO: add more exception handling logic here
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        mCamera.stopPreview();
        mCamera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();




        Camera.Size previewSize = previewSizes.get(0);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);


    }

    public synchronized Bitmap getS() {
        return s;
    }

    public void setS(Bitmap s) {
        this.s = s;
    }

    private Bitmap s;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        Camera.Parameters parameters = camera.getParameters();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;

        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 10, out);

        byte[] bytes = out.toByteArray();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);


        this.setS(bitmap);


    }
}