package com.yibogame.superrecorder.interfaces;

import java.nio.ByteBuffer;

/**
 * Created by parcool on 2017/11/26.
 */

public interface OnByteBufferDataChangeListener extends IBufferDataChangeInterface<ByteBuffer> {

    @Override
    void onDataChange(int position, ByteBuffer byteBuffer);
}