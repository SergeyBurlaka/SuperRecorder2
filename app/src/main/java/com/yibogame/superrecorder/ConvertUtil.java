package com.yibogame.superrecorder;

/**
 * Created by tanyi on 2017/12/8.
 */

public class ConvertUtil {
    private static final ConvertUtil ourInstance = new ConvertUtil();

    public static ConvertUtil getInstance() {
        return ourInstance;
    }

    private ConvertUtil() {
    }

    public short[] toShortArray(byte[] src) {

        int count = src.length >> 1;
        short[] dest = new short[count];
        for (int i = 0; i < count; i++) {
            dest[i] = (short) (src[i * 2] << 8 | src[2 * i + 1] & 0xff);
        }
        return dest;
    }

    public byte[] toByteArray(short[] src) {

        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2] = (byte) (src[i] >> 8);
            dest[i * 2 + 1] = (byte) (src[i] >> 0);
        }

        return dest;
    }
}
