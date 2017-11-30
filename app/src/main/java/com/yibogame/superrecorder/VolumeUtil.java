package com.yibogame.superrecorder;

/**
 * Created by parcool on 2017/12/1.
 */

public class VolumeUtil {
    private static final VolumeUtil ourInstance = new VolumeUtil();

    public static VolumeUtil getInstance() {
        return ourInstance;
    }

    private VolumeUtil() {
    }

    byte[] resetVolume(byte[] pcmBytes, float db) {
        for (int i = 0; i < pcmBytes.length; i+=2) {
            short s1 = (short)(((pcmBytes[i + 1] & 0xFF) << 8) | (pcmBytes[i] & 0xFF));
            s1 = (short)(s1 * db);
            pcmBytes[i + 1] = (byte)((s1 >> 8) & 0xFF);
            pcmBytes[i] = (byte)(s1 & 0xFF);
        }
        return pcmBytes;
    }
}
