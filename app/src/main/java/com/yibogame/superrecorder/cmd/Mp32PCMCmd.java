package com.yibogame.superrecorder.cmd;

import cn.gavinliu.android.ffmpeg.box.commands.BaseCommand;
import cn.gavinliu.android.ffmpeg.box.commands.Command;

import static cn.gavinliu.android.ffmpeg.box.utils.TextUtils.cmdFormat;

/**
 * Created by tanyi on 2017/11/28.
 */

public class Mp32PCMCmd extends BaseCommand {
    //ffmpeg -ss 4 -t 16 -i %s -f s16le -acodec pcm_s16le -b:a 16 -ar %d - ac %d %s
    private static final String CMD = "ffmpeg -y -i %s -f s16le -acodec pcm_s16le -b:a 16 -ar %d -ac %d %s";

    Mp32PCMCmd(String command) {
        super(command);
    }

    public static class Builder implements IBuilder {
        String inputFile;

        String outputFile;
        int channel;
        int rate;

        public Mp32PCMCmd.Builder setInputFile(String inputFile) {
            this.inputFile = inputFile;
            return this;
        }

        public Mp32PCMCmd.Builder setOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public Mp32PCMCmd.Builder setChannel(int channel) {
            this.channel = channel;
            return this;
        }

        public Mp32PCMCmd.Builder setRate(int rate) {
            this.rate = rate;
            return this;
        }

        @Override
        public Command build() {
            String cmd = cmdFormat(CMD, inputFile, rate, channel, outputFile);
            return new Mp32PCMCmd(cmd);
        }
    }
}
