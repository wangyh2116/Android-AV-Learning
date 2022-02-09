package com.wyh.cameratest;

import android.media.AudioFormat;

public class AudioConfig {
    //采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
    public final static int FRENQUENCY=16000;
    //声道数。CHANNEL_IN_MONO and CHANNEL_IN_STEREO. 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的
    public final static int CHANNEL_CONFIG= AudioFormat.CHANNEL_IN_MONO;
    //返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
    public final static int AUDIO_FORMAT=AudioFormat.ENCODING_PCM_16BIT;
    public static final int BIT_RATE = 64000;
    public static final String MIME_TYPE = "audio/mp4a-latm";
    public static String fileNamePCM="testAudio.pcm";
    public static String fileNameWAV="testAudio.wav";
}
