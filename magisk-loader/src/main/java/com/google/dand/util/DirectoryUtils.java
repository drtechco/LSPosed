package com.google.dand.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class DirectoryUtils {
    
    /**
     * 打印目录树结构
     * @param path 要打印的目录路径
     * @param tag 日志标签前缀
     */
    public static void printDirectoryTree(String path, String tag) {
        Utils.logD(tag + ": Starting to print directory tree for path: " + path);
        
        if (path == null || path.isEmpty()) {
            Utils.logE(tag + ": Invalid path provided");
            return;
        }
        
        File root = new File(path);
        if (!root.exists()) {
            Utils.logE(tag + ": Path does not exist: " + path);
            return;
        }
        
        StringBuilder tree = new StringBuilder();
        try {
            printDirectoryTreeInternal(root, "", "", tree);
            Utils.logD(tag + ": Directory Tree:\n" + tree.toString());
        } catch (SecurityException e) {
            Utils.logE(tag + ": Security exception while accessing directory", e);
        } catch (Exception e) {
            Utils.logE(tag + ": Error while printing directory tree", e);
        }
    }
    
    /**
     * 递归打印目录树的内部方法
     */
    private static void printDirectoryTreeInternal(File file, String indent, String prefix, StringBuilder tree) {
        tree.append(indent).append(prefix).append(file.getName()).append("\n");
        
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    boolean isLast = (i == files.length - 1);
                    printDirectoryTreeInternal(
                            files[i],
                            indent + (isLast ? "    " : "│   "),
                            isLast ? "└── " : "├── ",
                            tree
                    );
                }
            }
        }
    }
    
    /**
     * 专门用于打印 proc 目录下文件描述符的树结构
     * @param fd 文件描述符
     * @param tag 日志标签前缀
     */
    public static void printProcFdTree(int fd, String tag) {
        String path = "/proc/self/fd/" + fd;
        Utils.logD(tag + ": Examining file descriptor: " + fd);
        
        try {
            // 获取真实路径
            String realPath = new File(path).getCanonicalPath();
            Utils.logD(tag + ": Real path: " + realPath);
            
            // 打印文件信息
            Process process = Runtime.getRuntime().exec("ls -l " + path);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    Utils.logD(tag + ": File details: " + line);
                }
            }
            
            // 打印目录树
            printDirectoryTree(path, tag);
            
        } catch (IOException e) {
            Utils.logE(tag + ": Error accessing fd " + fd, e);
        }
    }
}