package com.yibogame.superrecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.yibogame.superrecorder.interfaces.IBufferDataChangeInterface;
import com.yibogame.superrecorder.interfaces.IOnRecordingListener;
import com.yibogame.superrecorder.interfaces.OnByteBufferDataChangeListener;
import com.yibogame.superrecorder.interfaces.OnShortBufferDataChangeListener;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * Created by parcool on 2017/11/26.
 */

public class Recorder {
    private AudioRecord mAudioRecord;
    private ByteBuffer byteBuffer;
    private ShortBuffer shortBuffer;
    private IOnRecordingListener iOnRecordingListener;

    private Thread mThreadVolume;

    public IOnRecordingListener getOnRecordingListener() {
        return iOnRecordingListener;
    }

    public void setOnRecordingListener(IOnRecordingListener iOnRecordingListener) {
        this.iOnRecordingListener = iOnRecordingListener;
    }

    private OnPeriodInFramesChangeListener l;

    private Recorder() {
    }

    /**
     * 构造方法传入采样率和回调接口，如果你是8Bit的数据，那么必须要用{@link OnByteBufferDataChangeListener}
     * 来进行回调。如果你是16Bit的数据，可以使用{@link OnByteBufferDataChangeListener}或者{@link OnShortBufferDataChangeListener}
     * 来进行回调，只不过一个传出去的是byteBuffer，一个是shortBuffer,请注意，当你使用ByteBuffer的时候，
     * byteBuffer的大小是period的两倍，取数据的时候请注意大小
     *
     * @param audioSource 输入源 {@link android.media.MediaRecorder.AudioSource}
     * @param period      处理sample数量
     * @param listener    读取完数据的回调
     */
    public Recorder(
            int audioSource,
            int period,
            final IBufferDataChangeInterface listener) {
        LogUtils.d("start init recorder.");
        mLock = new Object();
        mAudioRecord = findAudioRecord(audioSource);
        mAudioRecord.setPositionNotificationPeriod(period);
        if (isEncodingPCM16Bit()) {
            if (listener instanceof OnByteBufferDataChangeListener) {
                byteBuffer = ByteBuffer.allocate(period * 2);
            } else {
                shortBuffer = ShortBuffer.allocate(period);
            }
        } else {
            if (listener instanceof OnShortBufferDataChangeListener) {
                throw new IllegalArgumentException("Audio format is pcm 8 bit, so you only use OnByteBufferDataChangeListener!");
            }
            byteBuffer = ByteBuffer.allocate(period);
        }

//        mAudioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
//            @Override
//            public void onMarkerReached(AudioRecord recorder) {
//
//            }
//
//            @Override
//            public void onPeriodicNotification(AudioRecord recorder) {
//                if (l != null)
//                    l.onFrames(recorder);
//
//                if (listener == null) {
//                    return;
//                }
//                if (listener instanceof OnShortBufferDataChangeListener) {
//                    int position = read(shortBuffer.array());
//                    ((OnShortBufferDataChangeListener) listener).onDataChange(position, shortBuffer);
//                } else if (listener instanceof OnByteBufferDataChangeListener) {
//                    int position = read(byteBuffer.array());
//                    ((OnByteBufferDataChangeListener) listener).onDataChange(position, byteBuffer);
//                }
//
//            }
//        });
        mPCMBuffer = new short[bufferSizeInBytes];
        LogUtils.d("recorder inited.");
    }

    private short[] mPCMBuffer;
    private boolean isRecording = false;

    public void startRecording() {
        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            LogUtils.e("录音中呢，怎么开始呢？");
            return;
        }
        mAudioRecord.startRecording();
        isRecording = true;
        if (mThreadVolume == null) {
            mThreadVolume = new Thread(() -> {
                while (isRecording) {
                    int readSize = mAudioRecord.read(mPCMBuffer, 0, bufferSizeInBytes);
                    if (readSize > 0) {
                        calculateRealVolume(mPCMBuffer, readSize);
                    }
                }
            });
        }
        mThreadVolume.start();
    }

    Object mLock;

    private void calculateRealVolume(short[] mPCMBuffer, int readSize) {
        long v = 0;
        // 将 buffer 内容取出，进行平方和运算
        for (int i = 0; i < mPCMBuffer.length; i++) {
            v += mPCMBuffer[i] * mPCMBuffer[i];
        }
        // 平方和除以数据总长度，得到音量大小。
        double mean = v / (double) readSize;
        double volume = 10 * Math.log10(mean);
        if (iOnRecordingListener != null) {
            iOnRecordingListener.onDataReceived(mPCMBuffer, readSize, volume);
        }
//        LogUtils.d("分贝值:" + volume);
        // 大概一秒十次
        synchronized (mLock) {
            try {
                mLock.wait(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (mAudioRecord == null) {
            LogUtils.e("mAudioRecord is null!");
            return;
        }
        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
            return;
        mAudioRecord.stop();
        isRecording = false;
    }

    public void release() {
        stop();
        mAudioRecord.release();
    }

    public boolean isRecording() {
        return mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

    public void setOnPeriodInFramesChangeListener(OnPeriodInFramesChangeListener listener) {
        l = listener;
    }


    private int read(byte[] data) {
        return mAudioRecord.read(data, 0, data.length);
    }

    private int read(short[] data) {
        return mAudioRecord.read(data, 0, data.length);
    }

    /**
     * 返回当前AudioRecord的AudioFormat信息，判断是否为16bit来进行读取byte[]或者short[]
     *
     * @return 当前音源是否为PCM16比特
     */
    private boolean isEncodingPCM16Bit() {
        return mAudioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT;
    }

    /**
     * 当走完设定的period*frame的时候调用
     */
    public interface OnPeriodInFramesChangeListener {
        void onFrames(AudioRecord record);
    }

    private static int[] mSampleRates = new int[]{8000, 11025, 22050, 44100};

    public AudioRecord findAudioRecord(int audioSource) {
        int bufferSizeTemp = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT);
        if (bufferSizeTemp != AudioRecord.ERROR_BAD_VALUE) {
            this.sampleRateInHz = 44100;
            this.channelConfig = AudioFormat.CHANNEL_IN_MONO;
            this.audioFormat = AudioFormat.ENCODING_PCM_8BIT;
            this.bufferSizeInBytes = bufferSizeTemp;
            AudioRecord recorderTemp = new AudioRecord(audioSource, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT, bufferSizeTemp);
            LogUtils.i("default config is test ok,so return it.");
            return recorderTemp;
        }

        this.audioSource = audioSource;
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT}) {
                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                    try {
                        //Log.d("audioSetup", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
                        if (bufferSize > 0 && bufferSize <= 256) {
                            bufferSize = 256;
                        } else if (bufferSize > 256 && bufferSize <= 512) {
                            bufferSize = 512;
                        } else if (bufferSize > 512 && bufferSize <= 1024) {
                            bufferSize = 1024;
                        } else if (bufferSize > 1024 && bufferSize <= 2048) {
                            bufferSize = 2048;
                        } else if (bufferSize > 2048 && bufferSize <= 4096) {
                            bufferSize = 4096;
                        } else if (bufferSize > 4096 && bufferSize <= 8192) {
                            bufferSize = 8192;
                        } else if (bufferSize > 8192 && bufferSize <= 16384) {
                            bufferSize = 16384;
                        } else {
                            bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
                        }

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(audioSource, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                Log.d("found", "sampleRateInHz: " + rate + " channelConfig: " + channelConfig + " bufferSize: " + bufferSize + " audioFormat: " + audioFormat);
                                this.sampleRateInHz = rate;
                                this.channelConfig = channelConfig;
                                this.audioFormat = audioFormat;
                                this.bufferSizeInBytes = bufferSize;
                                return recorder;
                            }
                        }
                    } catch (Exception e) {
                        Log.d("audioSetup", rate + "Exception, keep trying.", e);
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    private int audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes;

    public int getAudioSource() {
        return audioSource;
    }

    public int getSampleRateInHz() {
        return sampleRateInHz;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public int getBufferSizeInBytes() {
        return bufferSizeInBytes;
    }
}
