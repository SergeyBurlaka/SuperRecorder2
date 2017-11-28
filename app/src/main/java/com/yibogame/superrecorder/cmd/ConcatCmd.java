package com.yibogame.superrecorder.cmd;

import java.util.ArrayList;
import java.util.List;

import cn.gavinliu.android.ffmpeg.box.commands.BaseCommand;
import cn.gavinliu.android.ffmpeg.box.commands.Command;

import static cn.gavinliu.android.ffmpeg.box.utils.TextUtils.cmdFormat;

/**
 * Created by tanyi on 2017/11/28.
 */

public class ConcatCmd extends BaseCommand {
    //ffmpeg -i 片头.wav -i 内容.WAV -i 片尾.wav -filter_complex '[0:0] [1:0] [2:0] concat=n=3:v=0:a=1 [a]' -map [a] 合成.wav
    //-i "concat:123.mp3|124.mp3" -acodec copy output.mp3
    private static final String CMD = "ffmpeg -y -i concat:%s -acodec copy %s";

    public ConcatCmd(String command) {
        super(command);
    }

    public static class Builder implements IBuilder {

        String outputFile;
        List<String> listInputs = new ArrayList<>();


        public ConcatCmd.Builder setOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public ConcatCmd.Builder addInputs(String inputFile) {
            this.listInputs.add(inputFile);
            return this;
        }

        public String solveList() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < listInputs.size(); i++) {
                sb.append(listInputs.get(i));
                if (i != listInputs.size() - 1) {
                    sb.append("|");
                }
            }
            return sb.toString();
        }


        @Override
        public Command build() {
            String cmd = cmdFormat(CMD, solveList(), outputFile);
            return new ConcatCmd(cmd);
        }
    }
}
