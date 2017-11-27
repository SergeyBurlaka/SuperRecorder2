package com.yibogame.superrecorder.interfaces;

/**
 * Created by tanyi on 2017/11/27.
 */

public interface IRecordListener {
    int TYPE_VOICE = 1,TYPE_BG = 2;
    void initRecorder(int type);

    void startRecord(int type);

    void pauseRecord(int type);

    void stopRecord(int type);
}
