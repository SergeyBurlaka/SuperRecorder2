package com.yibogame.superrecorder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;

import cn.gavinliu.android.ffmpeg.box.FFmpegBox;
import cn.gavinliu.android.ffmpeg.box.commands.BaseCommand;
import cn.gavinliu.android.ffmpeg.box.commands.Command;
import cn.gavinliu.android.ffmpeg.box.commands.CutGifCommand;

public class MainActivity extends BaseActivity {

    private static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnRecord).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RecordActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnFiles).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, FilesActivity.class);
            startActivity(intent);
        });
    }

}
