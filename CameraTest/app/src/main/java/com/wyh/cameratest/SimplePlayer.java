package com.wyh.cameratest;

import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SimplePlayer {
   private int mWidth;
   private int mHeight;
   private float mDuration;
   private int mInputBufferSize;
   private AudioTrack audioTrack;
   private final Surface mSurface;
   private H264Decoder mH264VideoDecoder;
   private H264Decoder mH264AudioDecoder;

   private MediaTrackerHelper mMediaTrackerHelper;
   private AutoFitSurfaceView mSurfaceView;
   private volatile boolean isRun;
   private boolean isEnd;
   private boolean isStart;
   private long startMs;

   public SimplePlayer(AutoFitSurfaceView surfaceView ,String file) throws IOException {
      mSurfaceView=surfaceView;

      mMediaTrackerHelper=new MediaTrackerHelper(file);
      MediaFormat mediaFormatVideo=mMediaTrackerHelper.setVideoTrakerAndGetMediaFormat();
      mWidth = mediaFormatVideo.getInteger(MediaFormat.KEY_WIDTH);
      mHeight = mediaFormatVideo.getInteger(MediaFormat.KEY_HEIGHT);
      mDuration = (float) (mediaFormatVideo.getLong(MediaFormat.KEY_DURATION) / 1000000.0);
      float radio=((float) mHeight)/mWidth;
      mSurfaceView.setAspectRatio(mSurfaceView.getWidth(),(int)(mSurfaceView.getWidth()*radio));
      mSurface=surfaceView.getHolder().getSurface();
      MediaCodec mediaCodecVideo = MediaCodec.createDecoderByType(mediaFormatVideo.getString(MediaFormat.KEY_MIME));
      mediaCodecVideo.configure(mediaFormatVideo, mSurface, null,0);
      mH264VideoDecoder=new H264Decoder(mediaCodecVideo);
      MediaFormat mediaFormatAudio=mMediaTrackerHelper.setAudioTrakerAndGetMediaFormat();
      int audioChannels = mediaFormatAudio.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
      int audioSampleRate = mediaFormatAudio.getInteger(MediaFormat.KEY_SAMPLE_RATE);
      int channelConfig = //mediaFormatAudio.getInteger(MediaFormat.KEY_CHANNEL_MASK);
              AudioFormat.CHANNEL_OUT_MONO;
      int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate,
              (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
              AudioFormat.ENCODING_PCM_16BIT);
      int maxInputSize = mediaFormatAudio.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
      mInputBufferSize = minBufferSize > 0 ? minBufferSize * 4 : maxInputSize;
      audioTrack = new AudioTrack(
              new AudioAttributes.Builder()
                      .setUsage(AudioAttributes.USAGE_MEDIA)
                      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                      .build(),
              new AudioFormat.Builder().setSampleRate(audioSampleRate)
                      .setEncoding(AudioConfig.AUDIO_FORMAT)
                      .setChannelMask(channelConfig)
                      .build(),
              mInputBufferSize,
              AudioTrack.MODE_STREAM,
              AudioManager.AUDIO_SESSION_ID_GENERATE);
      MediaCodec mediaCodecAudio = MediaCodec.createDecoderByType(mediaFormatAudio.getString(MediaFormat.KEY_MIME));
      mediaCodecAudio.configure(mediaFormatAudio, null, null, 0);
      mH264AudioDecoder=new H264Decoder(mediaCodecAudio);
      //audioTrack.play();

      mH264VideoDecoder.setmEncoderListener(new H264Decoder.EncoderListener() {
         @Override
         public H264Decoder.QueueInputData onInBufferOk(ByteBuffer inputBuffer) {
            int size=0;
            try {
               if(mMediaTrackerHelper!=null) {
                  //ByteBuffer byteBuffer = ByteBuffer.allocate(mWidth * mHeight);
                 size = mMediaTrackerHelper.decodeVideoData(inputBuffer);
               }
            } catch (IOException e) {
               e.printStackTrace();
            }finally {
               if(size>0){
                  return new H264Decoder.QueueInputData(size,mMediaTrackerHelper.getVideoSampleTime(),0);
               }else
                  return null;
            }
         }

         @Override
         public void onOutputBufferOk(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
            decodeDelay(bufferInfo, startMs);
         }

         @Override
         public void onOutputBufferFormatChange(int outputIndex, MediaFormat mediaFormat) {

         }

         @Override
         public void onEnd() {

         }
      });
      mH264AudioDecoder.setmEncoderListener(new H264Decoder.EncoderListener() {
         @Override
         public H264Decoder.QueueInputData onInBufferOk(ByteBuffer inputBuffer) {
            int size=0;
            try {
               if (mMediaTrackerHelper != null) {
                  size = mMediaTrackerHelper.decodeAudioData(inputBuffer);
               }
            }catch (Exception e){
               e.printStackTrace();
            }finally {
               if(size>0){
                  return new H264Decoder.QueueInputData(size,mMediaTrackerHelper.getAudioSampleTime(),0);
               }else
                  return null;
            }
         }

         @Override
         public void onOutputBufferOk(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
            byte[] data=new byte[bufferInfo.size];
            outputBuffer.clear();
            outputBuffer.get(data);
            if(audioTrack!=null){
               audioTrack.write(data,0,data.length);
            }
            //decodeDelay(bufferInfo, startMs);
         }

         @Override
         public void onOutputBufferFormatChange(int outputIndex, MediaFormat mediaFormat) {

         }

         @Override
         public void onEnd() {
            endPlay();
         }
      });
   }
   public void startPlay(){
      if(!isStart){
         startMs= System.currentTimeMillis();
         mH264AudioDecoder.startDecoder();
         mH264VideoDecoder.startDecoder();
         audioTrack.play();
         isStart=true;
      }
   }
   public void endPlay(){
      if(!isEnd){
         mH264AudioDecoder.stopRecord();
         mH264VideoDecoder.stopRecord();
         mMediaTrackerHelper.release();
         audioTrack.stop();
         audioTrack.release();
         isEnd=true;
      }

   }
   private void decodeDelay(MediaCodec.BufferInfo bufferInfo, long startMillis) {
      while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMillis) {
         try {
            Thread.sleep(10);
         } catch (InterruptedException e) {
            e.printStackTrace();
            break;
         }
      }
   }
}
