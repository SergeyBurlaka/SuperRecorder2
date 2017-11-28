package com.yibogame.superrecorder.cmd;

import java.util.ArrayList;
import java.util.List;

import cn.gavinliu.android.ffmpeg.box.commands.BaseCommand;
import cn.gavinliu.android.ffmpeg.box.commands.Command;

import static cn.gavinliu.android.ffmpeg.box.utils.TextUtils.cmdFormat;

/**
 * Created by tanyi on 2017/11/28.
 */

public class MixCmd extends BaseCommand {
    //ffmpeg -i split1.mp3 -i split2.mp3 -filter_complex amix=inputs=2:duration=1:dropout_transition=2 mix.mp3
    private static final String CMD = "ffmpeg -y%s -filter_complex amix=inputs=%d:duration=%d:dropout_transition=0 %s";

    public MixCmd(String command) {
        super(command);
    }

    public static class Builder implements IBuilder {

        String outputFile;
        List<String> listInputs = new ArrayList<>();
        int durationWhichOne = 1;

        public MixCmd.Builder setOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public MixCmd.Builder addInputs(String inputFile) {
            this.listInputs.add(inputFile);
            return this;
        }

        public String solveList() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < listInputs.size(); i++) {
                sb.append(" -i ");
                sb.append(listInputs.get(i));
            }
            return sb.toString();
        }

        public MixCmd.Builder setDurationWhichOne(int whichOne){
            this.durationWhichOne = whichOne;
            return this;
        }


        @Override
        public Command build() {
            String cmd = cmdFormat(CMD, solveList(), listInputs.size(),durationWhichOne,outputFile);
            return new MixCmd(cmd);
        }
    }
}
