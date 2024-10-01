package com.zyfdroid.epub.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.zyfdroid.epub.R;

public class ScreenUtils {

    public static void adaptScreens(Activity activity) {



        activity.getWindow().setNavigationBarColor(activity.getResources().getColor(R.color.whitebg));
        View decor = activity.getWindow().getDecorView();
        if (Color.blue(activity.getResources().getColor(R.color.whitebg)) > 127) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR );
                }
                else{

                    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
                }
                View contentView = activity.findViewById(android.R.id.content);
                contentView.setPadding(contentView.getPaddingLeft(),contentView.getPaddingTop(),contentView.getPaddingRight(),contentView.getPaddingBottom());
            }
        }
        if (SpUtils.getInstance(activity).getFullscreen()) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            // 隐藏状态栏和导航栏
            View decorView =  activity.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);

// 设置界面沉浸式全屏显示
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }

// 如果需要恢复状态栏和导航栏可见，可以调用以下代码
//decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        } else {
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.whitebg));
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            activity.getWindow().addFlags(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT);
        }

    }
}
