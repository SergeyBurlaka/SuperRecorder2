package com.yibogame.superrecorder.interfaces;

import java.nio.ShortBuffer;

/**
 * Created by parcool on 2017/11/26.
 */

public interface OnShortBufferDataChangeListener extends IBufferDataChangeInterface<ShortBuffer> {

    @Override
    void onDataChange(int position, ShortBuffer shortBuffer);

}