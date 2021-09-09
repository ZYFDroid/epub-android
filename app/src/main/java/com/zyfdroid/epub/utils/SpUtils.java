package com.zyfdroid.epub.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

public class SpUtils{
    private SharedPreferences sp;
    private SpUtils(Context ctx){
        sp = ctx.getSharedPreferences("default",Context.MODE_PRIVATE);
    }

    public static SpUtils getInstance(Context ctx){
        return new SpUtils(ctx);
    }

    public void setTextSize(int ts){
        sp.edit().putInt("readingFontSize",ts).apply();
    }

    public int getTextSize(){
        return sp.getInt("readingFontSize",15);
    }

    public boolean getEinkMode(){return sp.getBoolean("eink",false);}
    public void setEinkMode(boolean b){sp.edit().putBoolean("eink",b).apply();}

    public boolean getAllowNightMode(){
        return sp.getBoolean("nightmode",true);
    }
    public void setAllowNightMode(boolean value){
        sp.edit().putBoolean("nightmode",value).apply();
    }
    public void setOpenWithExternalReader(boolean b){
        sp.edit().putBoolean("openWithExternalReader",b).apply();
    }

    public boolean shouldOpenWithExternalReader(){
        return sp.getBoolean("openWithExternalReader",false);
    }

    public boolean firstRun(){
        boolean firstRun = sp.getBoolean("firstRun",true);
        sp.edit().putBoolean("firstRun",false).apply();
        return firstRun;
    }

}
