package com.wyh.cameratest;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 音视频混合线程
 */
public class MediaMuxerThread extends Thread {

    private static final String TAG = "MediaMuxerThread";

    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;

    private final Object lock = new Object();

    private final File mToFilr;


    private MediaMuxer mediaMuxer;
    private ArrayBlockingQueue<MuxerData> MutexDataQueue=new ArrayBlockingQueue(100);

    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;


    // 音轨添加状态
    private volatile boolean isVideoTrackAdd;
    private volatile boolean isAudioTrackAdd;

    private volatile boolean isExit = false;

    public MediaMuxerThread(File toFile) throws IOException {
        mToFilr=toFile;
        // 构造函数
        mediaMuxer=new MediaMuxer(toFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    // 停止音视频混合任务
    public void stopMuxer() {
            exit();
    }
    public synchronized void addMuxerData(MuxerData data) {
        if (!isMuxerStart()) {
            return;
        }
        MutexDataQueue.add(data);
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * 添加视频／音频轨
     *
     * @param index
     * @param mediaFormat
     */
    public synchronized void addTrackIndex(int index, MediaFormat mediaFormat) {
        if (isMuxerStart()) {
            return;
        }
        if (mediaMuxer != null) {
            int track = 0;
            try {
                track = mediaMuxer.addTrack(mediaFormat);
            } catch (Exception e) {
                Log.e(TAG, "addTrack 异常:" + e.toString());
                return;
            }
            if (index == TRACK_VIDEO) {
                videoTrackIndex = track;
                isVideoTrackAdd = true;
                Log.e(TAG, "添加视频轨完成");
            } else {
                audioTrackIndex = track;
                isAudioTrackAdd = true;
                Log.e(TAG, "添加音轨完成");
            }
            //requestStart();
        }
    }

    /**
     * 当前是否添加了音轨
     *
     * @return
     */
    public boolean isAudioTrackAdd() {
        return isAudioTrackAdd;
    }

    /**
     * 当前是否添加了视频轨
     *
     * @return
     */
    public boolean isVideoTrackAdd() {
        return isVideoTrackAdd;
    }

    /**
     * 当前音视频合成器是否运行了
     *
     * @return
     */
    public boolean isMuxerStart() {
        return isAudioTrackAdd && isVideoTrackAdd;
    }


    private void initMuxer() {

    }

    @Override
    public void run() {
        super.run();
        // 初始化混合器
        initMuxer();
        while(!isMuxerStart()){
        }
        mediaMuxer.start();
        System.out.println("++++++++++++++++++++++++++++start++++++++++++++++++++++++++++");
        try {
            while (!isExit||!MutexDataQueue.isEmpty()) {
                MuxerData data = null;
                data = MutexDataQueue.take();
                int track;
                if (data.trackIndex == TRACK_VIDEO) {
                    track = videoTrackIndex;
                } else {
                    track = audioTrackIndex;
                }
                Log.e(TAG, "写入混合数据 " + data.bufferInfo.size);
                try {
                    mediaMuxer.writeSampleData(track, data.byteBuf, data.bufferInfo);
                } catch (Exception e) {
                    Log.e(TAG, "写入混合数据失败!" + e.toString());
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            try {
                mediaMuxer.release();
                mediaMuxer.stop();
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
        }
    }





    private void restartAudioVideo() {

    }

    private void exit() {
        isExit=true;
    }

    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {

        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }


}
