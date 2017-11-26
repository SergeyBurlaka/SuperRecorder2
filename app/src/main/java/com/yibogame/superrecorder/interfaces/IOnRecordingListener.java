package com.yibogame.superrecorder.interfaces;

/**
 * Created by parcool on 2017/11/26.
 */

public interface IOnRecordingListener {
    void onDataReceived(short[] mPCMBuffer, int readSize, double volume);
}
