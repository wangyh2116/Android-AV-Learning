package com.wyh.cameratest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

public class AudioRecorder {
    private AudioRecord mAudioRecorder = null;
    private int bufSize = 0;
    public volatile boolean isRecord=false;
    private PcmToWavUtil mPcmToWavUtil=null;
    private Context mContext;

    private RecorderListener mRecorderListener=null;
    public AudioRecorder(Context context) {
        mContext=context;
        mPcmToWavUtil=new PcmToWavUtil(AudioConfig.FRENQUENCY,
                AudioConfig.CHANNEL_CONFIG,
                AudioConfig.AUDIO_FORMAT);
        bufSize = AudioRecord.getMinBufferSize(AudioConfig.FRENQUENCY,
                AudioConfig.CHANNEL_CONFIG,
                AudioConfig.AUDIO_FORMAT);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            throw new RuntimeException("Record Permission is not allowed!");
        }
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioConfig.FRENQUENCY,
                AudioConfig.CHANNEL_CONFIG,
                AudioConfig.AUDIO_FORMAT,
                bufSize);
    }
    public void startRecord(){
        if(mAudioRecorder==null){
            throw new RuntimeException("a AudioRercorder can use only once!");
        }
        mAudioRecorder.startRecording();
        isRecord=true;
        new Thread(()->{
//            File pcmFile=new File(mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),AudioConfig.fileNamePCM);
//            File wavFile=new File(mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),AudioConfig.fileNameWAV);
//            FileOutputStream os=null;
            try{
                //os=new FileOutputStream(pcmFile);
                //if(os!=null){
                    if(mRecorderListener!=null){
                        mRecorderListener.onStart();
                    }
                    byte[] data=new byte[bufSize];
                    while(isRecord){
                        int error=mAudioRecorder.read(data,0,bufSize);
                        if(error!=AudioRecord.ERROR){
                            if(mRecorderListener!=null){
                                mRecorderListener.onRecord(data);
                            }
                        }else{
                            if(mRecorderListener!=null){
                                mRecorderListener.onError(error);
                            }
                        }
                    }
                   //mPcmToWavUtil.pcmToWav(pcmFile,wavFile);
                //}
//            }catch (FileNotFoundException e){
//                e.printStackTrace();
//            }catch(IOException e){
//                e.printStackTrace();
            }finally {
                //CloseUtil.close(os);
                if (this.mAudioRecorder != null) {
                    mAudioRecorder.stop();
                    mAudioRecorder.release();
                    mAudioRecorder = null;
                }
                if(mRecorderListener!=null){
                    mRecorderListener.onEnd();
                }
            }
        }).start();
    }
    public boolean isRecord() {
        return isRecord;
    }

    public void setRecord(boolean record) {
        isRecord = record;
    }
    public RecorderListener getmRecorderListener() {
        return mRecorderListener;
    }

    public void setmRecorderListener(RecorderListener mRecorderListener) {
        this.mRecorderListener = mRecorderListener;
    }

    public interface RecorderListener{
        void onStart();
        void onRecord(byte[] data);
        void onError(int error);
        void onEnd();
    }
}
