package cn.gavinliu.android.ffmpeg.box;

import android.text.TextUtils;

import cn.gavinliu.android.ffmpeg.box.commands.Command;

/**
 * FFmpeg Tool box
 * <p>
 * Created by Gavin on 17-3-6.
 */

public class FFmpegBox {

    private static FFmpegBox sInstance;

    public static FFmpegBox getInstance() {
        if (sInstance == null) {
            sInstance = new FFmpegBox();
        }
        return sInstance;
    }

    public FFmpegBox() {
    }

    public int execute(Command command) {
        if (command == null || TextUtils.isEmpty(command.getCommand())) {
            return -2;
        }

        String[] commands = command.getCommand().split("\\s");
        return execute(commands);
    }

    public native String urlProtocolInfo();

    public native String avCodecInfo();

    public native String avFilterInfo();

    public native String avFormatInfo();

    private native int execute(String[] commonds);

    static {
        System.loadLibrary("avutil-55");
        System.loadLibrary("swresample-2");
        System.loadLibrary("avcodec-57");
        System.loadLibrary("avfilter-6");
        System.loadLibrary("swscale-4");
        System.loadLibrary("avdevice-57");
        System.loadLibrary("avformat-57");
        System.loadLibrary("postproc-54");
        System.loadLibrary("ffmpeg_box");
    }


}
