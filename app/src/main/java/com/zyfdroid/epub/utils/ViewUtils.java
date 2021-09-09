package com.zyfdroid.epub.utils;

import android.content.Context;

public class ViewUtils {

    public static int dip2px(Context ctx,float dip){
        return (int) (ctx.getResources().getDisplayMetrics().density * dip);
    }
}
