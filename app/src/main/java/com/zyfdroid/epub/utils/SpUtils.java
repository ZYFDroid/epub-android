package com.zyfdroid.epub.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

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

    public boolean getFullscreen(){
        return sp.getBoolean("fullscreen",true);
    }

    public void setFullscreen(boolean fullscreen){
        sp.edit().putBoolean("fullscreen",fullscreen).apply();
    }

    public boolean getShowStatusBar(){
        return sp.getBoolean("showStatusBar",true);
    }

    public void setShowStatusBar(boolean b){
        sp.edit().putBoolean("showStatusBar",b).apply();
    }

    public boolean firstRun(){
        boolean firstRun = sp.getBoolean("firstRun",true);
        sp.edit().putBoolean("firstRun",false).apply();
        return firstRun;
    }

    public int getHintRemainCount(String key,int defaultCount){
        String hintKey = "hint_"+key;
        int result = sp.getInt(hintKey,defaultCount+1);
        if(result > 0){
            result--;
            sp.edit().putInt(hintKey,result).apply();
        }
        return result;
    }

    public boolean shouldShowFullscreenHint(){
        return getHintRemainCount("fullscreenhint",3) > 0;
    }

    public String getCustomFont(){
        String str = sp.getString("font","");
        if(!TextUtils.isEmpty(str)){
            if(new File(str).exists()){
                return str;
            }
        }
        return "";
    }

    public void setCustomFont(String fontpath){
        sp.edit().putString("font", Environment.getExternalStorageDirectory().getAbsolutePath()+"/Books/.font/"+fontpath).apply();
    }


}
