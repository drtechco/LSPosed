package com.google.xmanager.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileTest {
    
    static final String TAG = "FileTest";
    
    public static void Test() {
        String path = "/data/adb/dand/test.txt";
        String content = "Hello, World!";
        writeFile(path, content);
        String readContent = readFile(path);
        Log.d(TAG, "readContent: " + readContent);
        deleteFile(path);
    }
    
    
    // 写入文件
    private static void writeFile(String path, String content) {
        File file = new File(path);
        FileOutputStream fos = null;
        BufferedWriter writer = null;
        try {
            fos = new FileOutputStream(file);
            writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "writeFile: ", e);
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) writer.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                Log.e(TAG, "writeFile: ", e);
                e.printStackTrace();
            }
        }
    }
    
    // 读取文件
    private static String readFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        FileInputStream fis = null;
        BufferedReader reader = null;
        try {
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "readFile: ", e);
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
                if (fis != null) fis.close();
            } catch (IOException e) {
                Log.e(TAG, "readFile: ", e);
                e.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }
    
    // 删除文件
    private static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "File deleted successfully.");
            } else {
                Log.e(TAG, "Failed to delete file.");
            }
        } else {
            Log.e(TAG, "File does not exist.");
        }
    }
}
