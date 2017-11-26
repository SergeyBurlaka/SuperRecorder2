package com.yibogame.superrecorder.interfaces;

import java.nio.Buffer;

/**
 * Created by parcool on 2017/11/26.
 */

public interface IBufferDataChangeInterface<T extends Buffer> {
    void onDataChange(int position, T t);
}