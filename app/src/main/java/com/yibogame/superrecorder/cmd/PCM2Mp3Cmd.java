package com.yibogame.superrecorder.cmd;

import cn.gavinliu.android.ffmpeg.box.commands.BaseCommand;
import cn.gavinliu.android.ffmpeg.box.commands.Command;

import static cn.gavinliu.android.ffmpeg.box.utils.TextUtils.cmdFormat;

/**
 * Created by tanyi on 2017/11/28.
 */

public class PCM2Mp3Cmd extends BaseCommand {

    private static final String CMD = "ffmpeg -y -ac %d -ar %d -f s16le -i %s -c:a libmp3lame -q:a 2 %s";

    public PCM2Mp3Cmd(String command) {
        super(command);
    }

    public static class Builder implements IBuilder {
        String inputFile;

        String outputFile;
        int channel;
        int rate;

        public PCM2Mp3Cmd.Builder setInputFile(String inputFile) {
            this.inputFile = inputFile;
            return this;
        }

        public PCM2Mp3Cmd.Builder setOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public PCM2Mp3Cmd.Builder setChannel(int channel) {
            this.channel = channel;
            return this;
        }

        public PCM2Mp3Cmd.Builder setRate(int rate) {
            this.rate = rate;
            return this;
        }

        @Override
        public Command build() {
            String cmd = cmdFormat(CMD, channel, rate, inputFile, outputFile);
            return new PCM2Mp3Cmd(cmd);
        }
    }
}
