package com.zyfdroid.epub.utils;

import android.content.Context;
import android.view.InputDevice;

public class ViewUtils {

    public static int dip2px(Context ctx,float dip){
        return (int) (ctx.getResources().getDisplayMetrics().density * dip);
    }

    public static boolean sourceIsGamepad(int source){
        return (source & (InputDevice.SOURCE_CLASS_JOYSTICK | InputDevice.SOURCE_GAMEPAD | InputDevice.SOURCE_JOYSTICK)) != 0;
    }
}
