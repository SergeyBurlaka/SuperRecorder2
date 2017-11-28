package com.yibogame.superrecorder.cmd;

import cn.gavinliu.android.ffmpeg.box.commands.BaseCommand;
import cn.gavinliu.android.ffmpeg.box.commands.Command;

import static cn.gavinliu.android.ffmpeg.box.utils.TextUtils.cmdFormat;

/**
 *
 * @author tanyi
 * @date 2017/11/28
 */

public class ChangeVolumeCmd extends BaseCommand {

    private static final String CMD = "ffmpeg -i %s -af volume=%.1f %s";

    public ChangeVolumeCmd(String command) {
        super(command);
    }

    public static class Builder implements IBuilder {
        String inputFile;

        String outputFile;

        /***
         * 倍数（0.0 ~ 100000.0）
         */
        float times;

        public ChangeVolumeCmd.Builder setInputFile(String inputFile) {
            this.inputFile = inputFile;
            return this;
        }

        public ChangeVolumeCmd.Builder setOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }


        public ChangeVolumeCmd.Builder setTimes(float times) {
            this.times = times;
            return this;
        }

        @Override
        public Command build() {
            String cmd = cmdFormat(CMD, inputFile, times, outputFile);
            return new ChangeVolumeCmd(cmd);
        }
    }
}
