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
        } else {
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.whitebg));
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            activity.getWindow().addFlags(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT);
        }

    }
}
