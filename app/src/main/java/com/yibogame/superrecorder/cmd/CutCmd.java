package com.yibogame.superrecorder.cmd;

import cn.gavinliu.android.ffmpeg.box.commands.BaseCommand;
import cn.gavinliu.android.ffmpeg.box.commands.Command;
import cn.gavinliu.android.ffmpeg.box.commands.FormatConvertCommand;

import static cn.gavinliu.android.ffmpeg.box.utils.TextUtils.cmdFormat;

/**
 * Created by tanyi on 2017/11/28.
 */

public class CutCmd extends BaseCommand {

    private static final String CMD = "ffmpeg -y -i %s -ss %s -t %s -acodec copy %s";

    public CutCmd(String command) {
        super(command);
    }

    public static class Builder implements IBuilder {
        String inputFile;

        String outputFile;

        String startTime;
        String endTime;

        public CutCmd.Builder setInputFile(String inputFile) {
            this.inputFile = inputFile;
            return this;
        }

        public CutCmd.Builder setOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }


        public CutCmd.Builder setStartTime(String startTime) {
            this.startTime = startTime;
            return this;
        }

        public CutCmd.Builder setEndTime(String endTime) {
            this.endTime = endTime;
            return this;
        }

        @Override
        public Command build() {
            String cmd = cmdFormat(CMD, inputFile, startTime, endTime, outputFile);
            return new CutCmd(cmd);
        }
    }
}
