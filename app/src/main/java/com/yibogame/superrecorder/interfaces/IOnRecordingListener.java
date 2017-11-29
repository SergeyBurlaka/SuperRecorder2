package com.yibogame.superrecorder.interfaces;

/**
 * Created by parcool on 2017/11/26.
 */

public interface IOnRecordingListener {
    void onDataReceived(byte[] mPCMBuffer, int readSize, double volume);
}
