package com.yibogame.superrecorder;

import android.content.Context;
import android.util.Log;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by parcool on 2017/12/1.
 */

public class MoveAssetsToSDCardUtil {
    private static final MoveAssetsToSDCardUtil ourInstance = new MoveAssetsToSDCardUtil();

    public static MoveAssetsToSDCardUtil getInstance() {
        return ourInstance;
    }

    private MoveAssetsToSDCardUtil() {
    }

    void move(Context context, String fileAssetPath, String fileSdPath) {
        //测试把文件直接复制到sd卡中 fileSdPath完整路径
        File file = new File(fileSdPath);
        if (!file.exists()) {
            LogUtils.d("文件不存在,文件创建");
            try {
                copyBigDataToSD(context, fileAssetPath, fileSdPath);
                LogUtils.d("拷贝成功");
            } catch (IOException e) {
                LogUtils.d("拷贝失败");
                e.printStackTrace();
            }
        } else {
            if (file.delete()) {
                LogUtils.d("文件夹存在,文件存在");
                move(context, fileAssetPath, fileSdPath);
            }
            LogUtils.e("无法拷贝：文件夹存在,文件存在");
        }

    }

    private void copyBigDataToSD(Context context, String fileAssetPath, String strOutFileName) throws IOException {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(strOutFileName);
        myInput = context.getAssets().open(fileAssetPath);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }

        myOutput.flush();
        myInput.close();
        myOutput.close();
    }

}
