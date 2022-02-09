package com.wyh.cameratest;

import android.content.Context;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class H264Encoder {
    private final static int TIMEOUT_USEC = 12000;

    private MediaCodec mediaCodec;

    private boolean isRuning = false;
    private byte[] configbyte;
    private byte[] keyFrameByte;
    private ArrayBlockingQueue<byte[]> originDataQueue=new ArrayBlockingQueue(1000);
    private File mToFile;
    private int mWidth,mHeight,mFrameRate;

    public void setmEncoderListener(EncoderListener mEncoderListener) {
        this.mEncoderListener = mEncoderListener;
    }

    private EncoderListener mEncoderListener=null;
    public H264Encoder(int width, int height, int framerate) {
        this.mWidth=width;
        this.mHeight=height;
        this.mFrameRate=framerate;
//        this.mToFile=file;
//        if(this.mToFile.exists()){
//            mToFile.delete();
//        }
        MediaFormat mediaFormat=MediaFormat.createVideoFormat("video/avc",width,height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public H264Encoder(MediaFormat mediaFormat,String mimeType) {
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
    public void putData(byte[]  buffer){
        originDataQueue.add(buffer);
    }
    public void startEncoder(){
        mediaCodec.start();
        new Thread(()->{
//            boolean isOutOver=false;
            isRuning=true;
            boolean isEnd=false;
//            FileOutputStream outputStream=null;
            try {
                while (isRuning||!isEnd) {
                    byte[] originData = originDataQueue.take();
                    if (originData == null) continue;
                    int inputIndex = mediaCodec.dequeueInputBuffer(-1);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputIndex);
                        inputBuffer.clear();
                        inputBuffer.put(originData);
                        mediaCodec.queueInputBuffer(inputIndex, 0, originData.length, System.nanoTime()/1000, 0);
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
                        if(mEncoderListener!=null){
                            mEncoderListener.onOutputBufferOk(outputBuffer,bufferInfo);
                        }
//                        System.out.println(bufferInfo.flags);
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
                        mediaCodec.releaseOutputBuffer(outputIndex,false);
                        //bufferInfo = new MediaCodec.BufferInfo();
                        outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                    }
                    if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ){
                        if(mEncoderListener!=null){
                            MediaFormat mediaFormat=mediaCodec.getOutputFormat();
                            mEncoderListener.onOutputBufferFormatChange(outputIndex,mediaFormat);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Log.e("H264Encoder",e.toString());
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
    public void encodeYUVImageAndPut(Image image, Context context, int degree){
        Image.Plane[] planes=image.getPlanes();
        byte[] y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
        byte[] uv = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
        byte[] vu = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
        image.getPlanes()[0].getBuffer().get(y);
        image.getPlanes()[1].getBuffer().get(uv);
        image.getPlanes()[2].getBuffer().get(vu);
        byte[] nv12 = new byte[image.getWidth()*image.getHeight()*3/2];
        for(int i=0;i<image.getHeight();i++){
            System.arraycopy(y,i*image.getPlanes()[0].getRowStride(),nv12,i*image.getWidth(),
                    image.getWidth());
        }
        for(int i=0;i<image.getHeight()/2;i++){
            if(i==image.getHeight()/2-1){
                System.arraycopy(uv,i*image.getPlanes()[1].getRowStride(),nv12,
                        image.getWidth()*image.getHeight()+i*image.getWidth(),
                        image.getWidth()-1);
            }else{
                System.arraycopy(uv,i*image.getPlanes()[1].getRowStride(),nv12,
                        image.getWidth()*image.getHeight()+i*image.getWidth(),
                        image.getWidth());
            }
        }
        for(int i=0;i<degree/90;i++){
            nv12=rotate90(nv12,image.getWidth(),image.getHeight());
        }
//                                    try{
//                                        YuvImage aImage = new YuvImage(nv12, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
//                                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                                        aImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 80, stream);
//                                        Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
//                                        //bitmap保存至本地
//                                        FileOutputStream fOut = new FileOutputStream(new
//                                                File(getExternalFilesDir(Environment.DIRECTORY_MOVIES),"testpic.jpg"));
//                                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
//                                        fOut.flush();
//                                        fOut.close();
//                                        bmp.recycle();
//                                        stream.close();
//                                    } catch (Exception ex) {
//                                        Log.e("Sys", "Error:" + ex.getMessage());
//                                    }
        //System.arraycopy(v,0,nv21,y.length+u.length,v.length);
        //YUVUtil.yuvToNv12(y,u,v,nv21,planes[0].getRowStride(),image.getHeight());
        this.putData(nv12);
    }
    public byte[] rotate90(byte[] nv12,int width,int height) {
        byte[] result=new byte[width*height*3/2];
        for(int i=0;i<height;i++){
            for(int j=0;j<width;j++){
                result[height-i-1+j*height]=nv12[i*width+j];
            }
        }
        for(int i=0;i<height/2;i++){
            for(int j=0;j<width;j+=2){
                result[width*height+(height/2-i-1)*2+j/2*height]=
                        nv12[width*height+i*width+j];
                result[width*height+(height/2-i-1)*2+j/2*height+1]=
                        nv12[width*height+i*width+j+1];
            }
        }
        return result;
    }

    public interface EncoderListener{
        void onOutputBufferOk(ByteBuffer outputBuffer,MediaCodec.BufferInfo bufferInfo);
        void onOutputBufferFormatChange(int outputIndex, MediaFormat mediaFormat);
    }
}
