package com.wyh.cameratest.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;

import com.wyh.cameratest.AudioConfig;
import com.wyh.cameratest.AudioRecorder;
import com.wyh.cameratest.AutoFitTextureView;
import com.wyh.cameratest.CameraHelper;
import com.wyh.cameratest.H264Encoder;
import com.wyh.cameratest.MediaMuxerThread;
import com.wyh.cameratest.R;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    /**
     * =============================================permission start========================================
     */
    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    //被拒绝的权限列表
    private List<String> mPermissionList = new ArrayList<>();
    private static final int MY_PERMISSIONS_REQUEST = 1001;
    private AudioRecorder mAudioRecorder;
    private File mFile;

    //    private byte[] configbyte;
//    FileOutputStream outputStream=null;
    private void checkPermissions() {
        // Marshmallow开始才用申请运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                        PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (!mPermissionList.isEmpty()) {
                String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, permissions[i] + " 权限被用户禁止！");
                }
            }
            // 运行时权限的申请不是本demo的重点，所以不再做更多的处理，请同意权限申请。
        }
    }
    /**
     * =============================================permission end========================================
     */
    /**
     * =============================================Button process========================================
     */
    @BindView(R.id.texture) public AutoFitTextureView cameraTextureView;
    Surface mSurface;
    @BindView(R.id.start_record)public Button startRecordButton;
    @OnClick(R.id.start_record)public void onStartRecordClick(){
        if(startRecordButton.getText().toString().intern()!="STOP RECORD"){
            /**
             * =============================================START RECORD========================================
             * mH264VideoEncoder initial,mH264AudioEncoder initial,mAudioRecorder initial,mMediaMuxerThread initial
             * mCameraHelper.startRecord();
             *             mAudioRecorder.startRecord();
             *             mMediaMuxerThread.start();
             *             mH264VideoEncoder.startEncoder();
             */
            playVideoButton.setClickable(false);
            mH264VideoEncoder =new H264Encoder(mCameraHelper.getSize().getWidth(),mCameraHelper.getSize().getHeight(),30);
            mH264VideoEncoder.setmEncoderListener(new H264Encoder.EncoderListener() {
                @Override
                public void onOutputBufferOk(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        bufferInfo.size = 0;
                    }
                    if (bufferInfo.size != 0) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        mMediaMuxerThread.addMuxerData(new MediaMuxerThread.MuxerData(MediaMuxerThread.TRACK_VIDEO,outputBuffer,bufferInfo));
                    }
                    //                        System.out.println(bufferInfo.flags);
//                    try {
//                        byte[] outData = new byte[bufferInfo.size];
//                        outputBuffer.get(outData);
//                        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//                            configbyte = new byte[bufferInfo.size];
//                            configbyte = outData;
//                        } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_SYNC_FRAME) {
//                            byte[] keyframe = new byte[bufferInfo.size + configbyte.length];
//                            System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
//                            System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);
//                            outputStream.write(keyframe, 0, keyframe.length);
//                        } else {
//                            outputStream.write(outData, 0, outData.length);
//                        }
//                    }catch (IOException e){
//                        e.printStackTrace();
//                    }
                }
                @Override
                public void onOutputBufferFormatChange(int outputIndex, MediaFormat mediaFormat) {
                    mMediaMuxerThread.addTrackIndex(MediaMuxerThread.TRACK_VIDEO,mediaFormat);
                }
            });
            MediaFormat audioFormat = MediaFormat.createAudioFormat(AudioConfig.MIME_TYPE, AudioConfig.FRENQUENCY, 1);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AudioConfig.BIT_RATE);
            audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, AudioConfig.FRENQUENCY);
            mH264AudioEncoder=new H264Encoder(audioFormat,AudioConfig.MIME_TYPE);
            mH264AudioEncoder.setmEncoderListener(new H264Encoder.EncoderListener() {
                @Override
                public void onOutputBufferOk(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        bufferInfo.size = 0;
                    }
                    if (bufferInfo.size != 0) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        mMediaMuxerThread.addMuxerData(new MediaMuxerThread.MuxerData(MediaMuxerThread.TRACK_AUDIO,outputBuffer,bufferInfo));
                    }
                }

                @Override
                public void onOutputBufferFormatChange(int outputIndex, MediaFormat mediaFormat) {
                    mMediaMuxerThread.addTrackIndex(MediaMuxerThread.TRACK_AUDIO,mediaFormat);
                }
            });
            mAudioRecorder=new AudioRecorder(this);
            mAudioRecorder.setmRecorderListener(new AudioRecorder.RecorderListener() {
                @Override
                public void onStart() {}
                @Override
                public void onRecord(byte[] data) {
                    if(mH264AudioEncoder !=null){
                        mH264AudioEncoder.putData(data);
                    }
                }
                @Override
                public void onError(int error) {
                    Log.e(TAG,"=================audioRecorder error===================="+error);
                }

                @Override
                public void onEnd() {}
            });

            if(mFile.exists()){
                mFile.delete();
            }
//            try {
//                outputStream=new FileOutputStream(file);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
            try {
                mMediaMuxerThread=new MediaMuxerThread(mFile);
            } catch (IOException e) {
                Log.e(TAG,e.toString());
                return;
            }
            mCameraHelper.startRecord();
            mAudioRecorder.startRecord();
            mMediaMuxerThread.start();
            mH264VideoEncoder.startEncoder();
            mH264AudioEncoder.startEncoder();
            startRecordButton.setText("STOP RECORD");
            /**
             * =============================================START RECORD END========================================
             */
        }else{
            /**
             * =============================================STOP RECORD========================================
             */
            playVideoButton.setClickable(true);
            mMediaMuxerThread.stopMuxer();
            mCameraHelper.stopRecord();
            startRecordButton.setText("START RECORD");
            mH264VideoEncoder.stopRecord();
            mH264AudioEncoder.stopRecord();
            mAudioRecorder.setRecord(false);
            /**
             * =============================================STOP END========================================
             */
        }
    }
    @BindView(R.id.play_video)public Button playVideoButton;
    @OnClick(R.id.play_video)public void onPlayVideoClick(){
        PlayerActivity.openPlayerActivity(this,mFile);
    }
    /**
     * =============================================Button process End========================================
     */
    private CameraHelper mCameraHelper;
    private H264Encoder mH264VideoEncoder;
    private H264Encoder mH264AudioEncoder;
    private MediaMuxerThread mMediaMuxerThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        ButterKnife.bind(this);
        startRecordButton.setClickable(false);
        mFile=new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES),"testVideo.mp4");
        /**
         * =============================================camera2 open========================================
         */
        cameraTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                mCameraHelper=CameraHelper.getInstance();
                mCameraHelper.setAutoFitTextureView(cameraTextureView)
                        .setCameraFacing(CameraCharacteristics.LENS_FACING_BACK)
                        .setAutoOptimalWandH(cameraTextureView.getWidth(),(int)(cameraTextureView.getWidth()*1.3333))
                        .setmCameraSessionCallback(new CameraHelper.CameraSessionCallback() {
                            @Override
                            public void onCameraSessionAvailable() {                                                
                                startRecordButton.setClickable(true);
                            }
                            @Override
                            public void onCameraSessionUnAvailable() {
                                startRecordButton.setClickable(false);
                            }
                        })
                        .setmRecordCallbackListener(new ImageReader.OnImageAvailableListener() {
                            @Override
                            public void onImageAvailable(ImageReader reader) {
                                Image image=reader.acquireNextImage();
                                //image.getPlanes()[0].
                                //image.getFormat()
                                Log.i(TAG,image.toString());
                                //System.out.println(getWindowManager().getDefaultDisplay().getRotation());
                                if(mH264VideoEncoder !=null){
                                    mH264VideoEncoder.encodeYUVImageAndPut(image,getApplicationContext(),mCameraHelper.getmSensorOrientation());
                                }
                                image.close();
                            }
                        })
                        .openCamera(MainActivity.this.getApplicationContext());

            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
        /**
         * =============================================camera2 end========================================
         */
    }
}