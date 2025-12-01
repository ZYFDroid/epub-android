package com.zyfdroid.epub.utils;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class AppUtils {
    public static final String XTEINL_PACKAGE_NAME = "com.xteink.xtplushpaper";
    /**
     * 检查安装包名为 com.xteink.xtplushpaper 的应用是否已安装。
     *
     * @param context 上下文对象，用于访问系统服务。
     * @return 如果应用已安装，返回 true；否则返回 false。
     */
    public static boolean isXTEinkAppInstalled(Context context) {
        // 目标应用的包名


        if (context == null) {
            // 如果上下文为空，无法执行检查，直接返回 false
            return false;
        }

        PackageManager packageManager = context.getPackageManager();

        try {
            // 尝试获取应用的包信息
            // getPackageInfo 方法会在应用未安装时抛出 NameNotFoundException
            packageManager.getPackageInfo(XTEINL_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            // 如果没有抛出异常，说明应用已安装
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            // 捕获到异常，说明应用未安装
            return false;
        }
        catch (Exception ex){
            Log.e("AppUtils", "isXTEinkAppInstalled: ", ex);
            return false;
        }
    }
}
