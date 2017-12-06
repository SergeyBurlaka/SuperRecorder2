package com.yibogame.superrecorder;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by parcool on 2017/12/1.
 */

public class RecordBgMusicUtil {
    private static final RecordBgMusicUtil ourInstance = new RecordBgMusicUtil();

    public static RecordBgMusicUtil getInstance() {
        return ourInstance;
    }

    private RecordBgMusicUtil() {
    }

    void appendMusic(String oriBgPath, String filePath, long skipMills, long readTimeMill, float volumePercent, boolean append) {

//        int offset = (int) (88200 * ((float) (skipMills) / 1000f));
//        int length = (int) (88200 * ((float) (readTimeMill) / 1000f));
//        LogUtils.d("skipMills=" + skipMills + ",readTimeMill=" + readTimeMill + ",offset=" + offset + ",length=" + length + ",aaaaa=" + ((float) (skipMills) / 1000f));
        try {
//            byte[] bytes = readSDFile(oriBgPath, offset, length, volumePercent);
//            writeAudioDataToFile(filePath, bytes, append);
            byte[] bytes = readSDFile(oriBgPath, skipMills, (int) readTimeMill, volumePercent);
            writeAudioDataToFile(filePath, bytes, append);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void appendMusic(String oriBgPath, String filePath, long needWriteBytes, float volumePercent, boolean append) {
        long offset = FileUtils.getFileLength(filePath);
        int length = (int) needWriteBytes;
        try {
            byte[] bytes = readSDFile(oriBgPath, offset, length, volumePercent);
            writeAudioDataToFile(filePath, bytes, append);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void appendMusic(String filePath, byte[] bytes, float volumePercent, boolean append) {
        bytes = VolumeUtil.getInstance().resetVolume(bytes, volumePercent);
        writeAudioDataToFile(filePath, bytes, append);
    }

    /***
     * 读取文件为byte[]
     * @param fileName
     * @return
     * @throws IOException
     */
    public byte[] readSDFile(String fileName, long offset, int length, float db) throws IOException {
//        byte[] bytesForReturn = null;
        byte[] bytes = new byte[length];
        ByteArrayOutputStream arrayOutputStream = null;
        int byteread = 0;
        try {
            FileInputStream inputStream = new FileInputStream(fileName);
            arrayOutputStream = new ByteArrayOutputStream();
            long actualSkiped = inputStream.skip(FileUtils.getFileLength(fileName) == 0 ? offset : offset % FileUtils.getFileLength(fileName));
//            LogUtils.d("offset=" + offset);
//            if (actualSkiped == -1 || actualSkiped < offset % FileUtils.getFileLength(fileName)) {
//                if (inputStream.markSupported()) {
//                    inputStream.reset();
//                }
//                long firstRead = FileUtils.getFileLength(fileName) - offset % FileUtils.getFileLength(fileName);
//                actualSkiped = inputStream.skip(firstRead);
//                offset = offset - firstRead;
//                actualSkiped = inputStream.skip(offset);
//                int read = inputStream.read(bytes, (int) firstRead, (int) offset);
//            } else {
            int read = inputStream.read(bytes, 0, length);
//            }
            if (db != -1) {
                bytes = VolumeUtil.getInstance().resetVolume(bytes, db);
            }
            inputStream.close();
            arrayOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public void writeAudioDataToFile(String filePath, byte[] bytes, boolean append) {
        if (bytes == null) {
            return;
        }
        // Write the output audio in byte
//        short sData[] = new short[bytes.length / 2];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath, append);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (os == null) {
            return;
        }
//        byte bData[] = short2byte(sData);
        try {
//            os.write(bData, bytes);
            os.write(bytes, 0, bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }
}
