package com.yibogame.superrecorder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.blankj.utilcode.util.LogUtils;

/**
 * Created by tanyi on 2017/11/27.
 */

public class PCMPlayer {
    private AudioTrack audioTrack;
    private int bufferSize;

    public int getBufferSize() {
        return bufferSize;
    }

    // 默认
    private int sampleRate = 4000;                          // 采样率  4000 每秒钟采集4000个点
    private int channel = AudioFormat.CHANNEL_OUT_MONO;     // 声道个数 1 单声道
    private int format = AudioFormat.ENCODING_PCM_8BIT;     // 每个采样点8bit量化 采样精度

//    public PCMPlayer() {
//        initPlay();
//    }

    public PCMPlayer(int sampleRate, int channel, int format) {
        this.sampleRate = sampleRate;
        this.channel = channel;
        this.format = format;
        initPlay();
    }

    private void initPlay() {

        /**
         * AudioTrack支持4K-48K采样率
         * (sampleRateInHz< 4000) || (sampleRateInHz > 48000) )
         */

        // 获得缓冲流大小
        bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, format);

        // 初始化AudioTrack
        /**
         * 参数:
         * 1.streamType
         *   STREAM_ALARM：警告声
         *   STREAM_MUSCI：音乐声，例如music等
         *   STREAM_RING：铃声
         *   STREAM_SYSTEM：系统声音
         *   STREAM_VOCIE_CALL：电话声音
         *
         * 2.采样率
         * 3.声道数
         * 4.采样精度
         * 5.每次播放的数据大小
         * 6.AudioTrack中有MODE_STATIC和MODE_STREAM两种分类。
         *   STREAM的意思是由用户在应用程序通过write方式把数据一次一次得写到audiotrack中。
         *   意味着你只需要开启播放后 后续使用write方法(AudioTrack的方法)写入buffer就行
         *
         *   STATIC的意思是一开始创建的时候，就把音频数据放到一个固定的buffer，然后直接传给audiotrack，
         *   后续就不用一次次得write了。AudioTrack会自己播放这个buffer中的数据。
         *   这种方法对于铃声等内存占用较小，延时要求较高的声音来说很适用。
         */
        try {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, format,
                    bufferSize, AudioTrack.MODE_STREAM);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            LogUtils.d("sampleRate="+sampleRate+",channel="+channel+",format="+format);
            return;
        }


        // 播放 后续你直接write数据就行
        audioTrack.play();
    }

    // 写入数据就能播放 但是要先初始化 开启播放器
    public void write(byte[] buffer) {
        /**
         * 1.要播放的buffer
         * 2.播放需要的位移
         * 3.播放的数据长度
         */
        audioTrack.write(buffer, 0, buffer.length);
    }

    /**
     * 释放资源
     */
    public void destoryPlay() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
}
