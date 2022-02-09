package com.wyh.cameratest;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraHelper {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray(4);

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private Context mContext;
    private int mCameraFacing=CameraCharacteristics.LENS_FACING_BACK;
    private String mCameraId = null;
    private Handler mPreviewHandler;
    CameraDevice mCamera;
    private static final String TAG="CameraHelper";
    private ImageReader mImageReader=null;
    private CaptureRequest.Builder mRequestBuilder;
    private AutoFitTextureView mAutoFitTextureView=null;
    private int mWidth=0;
    private int mHeight=0;



    private Integer mSensorOrientation;

    public Size getSize(){
        return new Size(mWidth,mHeight);
    }
    private CameraCaptureSession mPreviewSession;

    private CameraSessionCallback mCameraSessionCallback;
    private ImageReader.OnImageAvailableListener mRecordCallbackListener;
    private static class CameraHelperInstance {
        private static final CameraHelper sInstance = new CameraHelper();
    }

    public static CameraHelper getInstance() {
        CameraHelper c = CameraHelperInstance.sInstance;
        return c;
    }

    public CameraHelper() {
        mPreviewHandler = new Handler(Looper.getMainLooper());
    }


    private String getSpecificCameraId(Context context,final int c) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String result=null;
        try {
            for(String cameraId:manager.getCameraIdList()){
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                Log.i(TAG, "facing:" + facing);
                if (facing != null && facing == c) {
                    result= cameraId;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG,e.toString());
        }
        return result;
    }
    public CameraHelper setmCameraSessionCallback(CameraSessionCallback mCameraSessionCallback) {
        this.mCameraSessionCallback = mCameraSessionCallback;
        return this;
    }
    public CameraHelper setmRecordCallbackListener(ImageReader.OnImageAvailableListener recordCallback){
        this.mRecordCallbackListener=recordCallback;
        return this;
    }
    public CameraHelper setCameraFacing(final int cameraId){
        this.mCameraFacing=cameraId;
        return this;
    }
    public CameraHelper setAutoOptimalWandH(int width,int height){
        mWidth=width;
        mHeight=height;
        return this;
    }
    public CameraHelper setAutoFitTextureView(AutoFitTextureView textureView){
        this.mAutoFitTextureView=textureView;
        return this;
    }
    public void openCamera(final Context context) {
        if(mAutoFitTextureView==null){
            Log.e(TAG,"mAutoFitTextureView cannot be null");
            return;
        }
        mContext = context;
        SurfaceTexture surfaceTextureurface=this.mAutoFitTextureView.getSurfaceTexture();
        assert surfaceTextureurface!=null;
        Surface viewSurface=new Surface(surfaceTextureurface);
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId=getOptimalCamera(context,mWidth,mHeight);
            final Semaphore lock=new Semaphore(1);
            if (!lock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG,"not granted");
                return;
            }
            manager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    lock.release();
                    mCamera=camera;
                    initImageReader(context,mWidth,mHeight);
                    try {
                        mCamera.createCaptureSession(Arrays.asList(viewSurface,mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                try {
                                    mPreviewSession =session;
                                    mRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    mRequestBuilder.addTarget(viewSurface);
                                    //mRequestBuilder.addTarget(viewSurface);
                                    mRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    mRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                    session.setRepeatingRequest(mRequestBuilder.build(),null,mPreviewHandler);
                                    mCameraSessionCallback.onCameraSessionAvailable();
                                } catch (CameraAccessException e) {
                                    Log.e(TAG,e.toString());
                                }
                            }
                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                Log.e(TAG,"onConfigureFailed falied.");
                                mCameraSessionCallback.onCameraSessionUnAvailable();
                            }
                        },mPreviewHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG,e.toString());
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    lock.release();
                    camera.close();
                    mCamera=null;
                    Log.i(TAG,"Camera disconnected.");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    lock.release();
                    camera.close();
                    mCamera = null;
                    Log.e(TAG,"Camera error ------> "+error);
                }
            }, mPreviewHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }
    private String getOptimalCamera(final Context context,final int width,final int height) throws CameraAccessException {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String resultCamera=getSpecificCameraId(context,mCameraFacing);
        CameraCharacteristics characteristics=manager.getCameraCharacteristics(resultCamera);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size opSize=chooseOptimalSize(map.getOutputSizes(ImageFormat.YUV_420_888),width,height);
        mWidth=opSize.getWidth();
        mHeight=opSize.getHeight();
        mAutoFitTextureView.setAspectRatio(opSize.getWidth(),opSize.getHeight());
        return resultCamera;
    }
    private Size chooseOptimalSize(Size[] choices, final int width, final int height) {
        Size resultSize=null;
        double bestRatio=Double.MAX_VALUE;
        double targetRatio=(double)height/width;
        double minDiff = Double.MAX_VALUE;
        if(choices.length>0){
            if(choices[0].getHeight()/choices[0].getWidth()!=height/width){
                for(int i=0;i<choices.length;i++){
                    choices[i]=new Size(choices[i].getHeight(),choices[i].getWidth());
                }
            }
        }
        Arrays.sort(choices,(a,b)->b.getWidth()-a.getWidth());
        for (Size size : choices) {
            double ratio = (double) size.getHeight()/size.getWidth();
            if (Math.abs(ratio-targetRatio) < minDiff) {
                bestRatio=ratio;
                //resultSize=size;
                minDiff = Math.abs(ratio-targetRatio);
            }
        }
        resultSize=new Size(width,(int)(width*bestRatio));
        return resultSize;
    }
    private void initImageReader(Context context, int width, int height) {
        mImageReader=ImageReader.newInstance(width,height, ImageFormat.YUV_420_888,1);
        mImageReader.setOnImageAvailableListener(this.mRecordCallbackListener,mPreviewHandler);
    }
    public void close(Surface outputTarget){
        if(mCamera != null){
           mRequestBuilder.removeTarget(outputTarget);
            mCamera.close();
        }
    }

    public void startRecord(){
        try {
            mPreviewSession.stopRepeating();
            mRequestBuilder.addTarget(mImageReader.getSurface());
            CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);
            mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            mPreviewSession.setRepeatingRequest(mRequestBuilder.build(),null,mPreviewHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG,e.toString());
            //e.printStackTrace();
        }
    }
    public void stopRecord(){
        try {
            mPreviewSession.stopRepeating();
            mRequestBuilder.removeTarget(mImageReader.getSurface());
            mPreviewSession.setRepeatingRequest(mRequestBuilder.build(),null,mPreviewHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG,e.toString());
            //e.printStackTrace();
        }
    }
    private CaptureRequest.Builder initDngBuilder() {
        CaptureRequest.Builder captureBuilder = null;
        try {
            captureBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureBuilder.addTarget(mImageReader.getSurface());
            // Required for RAW capture
            captureBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) ((214735991 - 13231) / 2));
            captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, (10000 - 100) / 2);//设置 ISO，感光度
            //设置每秒30帧
            CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);
            Range<Integer> fps[] = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps[fps.length - 1]);
        } catch (CameraAccessException e) {
            Log.i("initDngBuilder", "initDngBuilder");
            e.printStackTrace();
        }
        return captureBuilder;
    }
    public Integer getmSensorOrientation() {
        return mSensorOrientation;
    }
    public interface CameraSessionCallback {
        public void onCameraSessionAvailable();
        public void onCameraSessionUnAvailable();
    }
}
