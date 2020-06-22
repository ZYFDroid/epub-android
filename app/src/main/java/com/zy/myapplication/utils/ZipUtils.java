package com.zy.myapplication.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @作者: JJ
 * @创建时间: 2018/9/12 上午11:58
 * @Version 1.0
 * @描述: 解压工具类
 */
public class ZipUtils {

    /**
     * 解压指定文件到指定目录
     *
     * @param filePath 压缩包路径
     * @param savePath 解压后保存的路径
     * @param fileName 指定解压的文件名
     * @return 成功true
     */
    public static boolean zipSpecifiedFile(final String filePath, final String savePath, final String fileName) {
        ZipFile zipFile = null;
        try {
            File file = new File(filePath);
            file.getParentFile();
            zipFile = new ZipFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            ZipEntry zipEntry = zipFile.getEntry(fileName);
            ZipUtils.getContent(zipFile, zipEntry, savePath + fileName);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 从zip包中读取给定文件名的内容
     *
     * @param zipFile
     * @param zipEntry
     * @return
     * @throws IOException
     */
    public static void getContent(final ZipFile zipFile,
                                  final ZipEntry zipEntry, final String savePath) throws IOException {
        InputStream inputStream = zipFile.getInputStream(zipEntry);

        File file = FileUtils.createNewFile(savePath);

        FileOutputStream fileOutputStream = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        int length;

        while ((length = (inputStream.read(buffer))) != -1) {
            if (length == 4096) {
                fileOutputStream.write(buffer);
            } else {
                byte[] readBytes = new byte[length];
                System.arraycopy(buffer, 0, readBytes, 0, length);
                fileOutputStream.write(readBytes);
            }
        }
        inputStream.close();

        fileOutputStream.flush();
        fileOutputStream.close();
    }

    /**
     * 合并数组
     *
     * @param a
     * @return
     */
    public static byte[] mergeArray(byte[]... a) {
        // 合并完之后数组的总长度
        int index = 0;
        int sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum = sum + a[i].length;
        }
        byte[] result = new byte[sum];
        for (int i = 0; i < a.length; i++) {
            int lengthOne = a[i].length;
            if (lengthOne == 0) {
                continue;
            }
            // 拷贝数组
            System.arraycopy(a[i], 0, result, index, lengthOne);
            index = index + lengthOne;
        }
        return result;
    }

}