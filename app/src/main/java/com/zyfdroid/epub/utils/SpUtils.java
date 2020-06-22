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

    @SuppressLint("ApplySharedPref")
    public void setTextSize(int ts){
        sp.edit().putInt("readingFontSize",ts).commit();
    }

    public int getTextSize(){
        return sp.getInt("readingFontSize",15);
    }

    @SuppressLint("ApplySharedPref")
    public void setOpenWithExternalReader(boolean b){
        sp.edit().putBoolean("openWithExternalReader",b).commit();
    }

    public boolean shouldOpenWithExternalReader(){
        return sp.getBoolean("openWithExternalReader",false);
    }


}
