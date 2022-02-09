package com.wyh.cameratest;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class H264Decoder {
    private final static int TIMEOUT_USEC = 12000;

    private MediaCodec mediaCodec;

    private boolean isRuning = false;
    private byte[] configbyte;
    private byte[] keyFrameByte;
    private ArrayBlockingQueue<QueueInputData> originDataQueue=new ArrayBlockingQueue(1000);
    private File mToFile;
    private int mWidth,mHeight,mFrameRate;

    public void setmEncoderListener(EncoderListener mEncoderListener) {
        this.mEncoderListener = mEncoderListener;
    }

    private EncoderListener mEncoderListener=null;
    public H264Decoder(MediaFormat mediaFormat,String mimeType) {
//        this.mToFile=file;
//        if(this.mToFile.exists()){
//            mToFile.delete();
//        }
        try {
            mediaCodec = MediaCodec.createEncoderByType(mimeType);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public H264Decoder(MediaCodec mediaCodec) {
//        this.mToFile=file;
//        if(this.mToFile.exists()){
//            mToFile.delete();
//        }
        this.mediaCodec = mediaCodec;
    }
    public void putData(QueueInputData  data){
        originDataQueue.add(data);
    }
    public void startDecoder(){
        mediaCodec.start();
        new Thread(()->{
            isRuning=true;
            boolean isEnd=false;
            try {
                while (isRuning||!isEnd) {
                    int inputIndex = mediaCodec.dequeueInputBuffer(-1);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputIndex);
                        QueueInputData originData=null;
                        if(mEncoderListener!=null){
                            originData=mEncoderListener.onInBufferOk(inputBuffer);
                        }
                        if (originData == null) {
                            isRuning=false;
                            isEnd=true;
                            break;
                        }
                        mediaCodec.queueInputBuffer(inputIndex, 0, originData.length, originData.presentationTimeUs, originData.flag);
                    }
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                    if((bufferInfo.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0){
                        isEnd=true;
                    }else{
                        isEnd=false;
                    }
                    while(outputIndex >= 0) {
                        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputIndex);
                        outputBuffer.clear();
                        byte[] outbyte=new byte[outputBuffer.limit()];
                        outputBuffer.get(outbyte);
                        if(mEncoderListener!=null){
                            mEncoderListener.onOutputBufferOk(outputBuffer,bufferInfo);
                        }
                        mediaCodec.releaseOutputBuffer(outputIndex,true);
                        outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                    }
                    if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ){
                        if(mEncoderListener!=null){
                            MediaFormat mediaFormat=mediaCodec.getOutputFormat();
                            mEncoderListener.onOutputBufferFormatChange(outputIndex,mediaFormat);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("H264Decoder",e.toString());
            }finally {
                // 停止编解码器并释放资源
                try {
                    mediaCodec.stop();
                    mediaCodec.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void stopRecord(){
        this.isRuning=false;
    }
    public interface EncoderListener{
        QueueInputData onInBufferOk(ByteBuffer inputBuffer);
        void onOutputBufferOk(ByteBuffer outputBuffer,MediaCodec.BufferInfo bufferInfo);
        void onOutputBufferFormatChange(int outputIndex, MediaFormat mediaFormat);
        void onEnd();
    }
    public static class QueueInputData{
        public QueueInputData(int length, long presentationTimeUs, int flag) {
            this.length = length;
            this.presentationTimeUs = presentationTimeUs;
            this.flag = flag;
        }

        public int length;
        public long presentationTimeUs;
        public int flag;
    }
}
