package com.wyh.cameratest;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaTrackerHelper {
    public MediaExtractor videoExtractor;
    private MediaExtractor audioExtractor;
    private int videoTrackIndex;
    private int audioTrackIndex;
    private long VideoSampleTime;
    private long AudioSampleTime;
    public MediaTrackerHelper(String file) throws IOException {
        init(file);
    }
    public void init(String file) throws IOException {
        videoExtractor=new MediaExtractor();
        audioExtractor=new MediaExtractor();
        videoExtractor.setDataSource(file);
        audioExtractor.setDataSource(file);
    }
    public void release(){
        videoExtractor.release();
        audioExtractor.release();
        videoExtractor=null;
        audioExtractor=null;
    }
    public MediaFormat setVideoTrakerAndGetMediaFormat(){
        videoTrackIndex = getTrackIndex(videoExtractor, "video/");
        if (videoTrackIndex >= 0) {
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(videoTrackIndex);
//            int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
//            int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
//            float time = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000000;
            videoExtractor.selectTrack(videoTrackIndex);
            return mediaFormat;
        }
        return null;
    }
    public MediaFormat setAudioTrakerAndGetMediaFormat(){
        audioTrackIndex = getTrackIndex(audioExtractor, "audio/");
        if (audioTrackIndex >= 0) {
            MediaFormat mediaFormat = audioExtractor.getTrackFormat(audioTrackIndex);
//            int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
//            int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
//            float time = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000000;
            audioExtractor.selectTrack(audioTrackIndex);
            return mediaFormat;
        }
        return null;
    }
    /**
     * 获取媒体类型的轨道
     * @param extractor
     * @param mediaType
     * @return
     */
    private static int getTrackIndex(MediaExtractor extractor, String mediaType) {
        int trackIndex = -1;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = extractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mediaType)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }

    public long getVideoSampleTime(){
        return VideoSampleTime;
    }
    public long getAudioSampleTime(){
        return AudioSampleTime;
    }
    public int decodeVideoData(ByteBuffer inputBuffer) throws IOException {
        //boolean isMediaEOS = false;
        int sampleSize = videoExtractor.readSampleData(inputBuffer, 0);
        if (sampleSize < 0) {
            return -1;
        } else {
            VideoSampleTime=videoExtractor.getSampleTime();
            videoExtractor.advance();
        }
        return sampleSize;
    }
    public int decodeAudioData(ByteBuffer inputBuffer) throws IOException {
       //boolean isMediaEOS = false;
        int sampleSize = audioExtractor.readSampleData(inputBuffer, 0);
        if (sampleSize < 0) {
            return -1;
        } else {
            AudioSampleTime=audioExtractor.getSampleTime();
            audioExtractor.advance();
        }
        return sampleSize;
    }
}
