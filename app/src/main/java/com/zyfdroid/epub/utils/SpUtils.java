package com.zyfdroid.epub.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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


    public int getNightMode(){
        return sp.getInt("nightmode2",0);
    }
    public void setNightMode(int value){
        sp.edit().putInt("nightmode2",value).apply();
    }
    public void setOpenWithExternalReader(boolean b){
        sp.edit().putBoolean("openWithExternalReader",b).apply();
    }

    public boolean shouldOpenWithExternalReader(){
        return sp.getBoolean("openWithExternalReader",false);
    }

    public boolean getFullscreen(){
        return sp.getBoolean("fullscreen3",false);
    }

    public void setFullscreen(boolean fullscreen){
        sp.edit().putBoolean("fullscreen3",fullscreen).apply();
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

    public String getDesktopSlot(int index){
        return sp.getString("desktop_"+index,"");
    }

    public void setDesktopSlot(int index,String uuid){
        sp.edit().putString("desktop_"+index,uuid).apply();
    }

    public boolean isDesktopEmpty(){
        for (int i = 0; i < DESKTOP_SLOT_COUNT; i++) {
            if(!TextUtils.isEmpty(getDesktopSlot(i))){
                return false;
            }
        }
        return true;
    }

    public static final int DESKTOP_SLOT_COUNT = 18;

    public boolean isInDesktop(String uuid){
        for (int i = 0; i < DESKTOP_SLOT_COUNT; i++) {
            if(getDesktopSlot(i).equals(uuid)){
                return true;
            }
        }
        return false;
    }

    public void removeFromDesktop(String uuid){
        for (int i = 0; i < DESKTOP_SLOT_COUNT; i++) {
            if(getDesktopSlot(i).equals(uuid)){
                setDesktopSlot(i,"");
            }
        }
    }

    public List<DBUtils.BookEntry> getDesktopBooks(){
        List<DBUtils.BookEntry> desktopBooks = new ArrayList<>();
        for (int i = 0; i < DESKTOP_SLOT_COUNT; i++) {
            String uuid = getDesktopSlot(i);
            if(TextUtils.isEmpty(uuid)){
                desktopBooks.add(null);
            }
            else{
                List<DBUtils.BookEntry> result = DBUtils.queryBooks("uuid = ?",uuid);
                if(result.size() > 0){
                    desktopBooks.add(result.get(0));
                }
                else{
                    desktopBooks.add(null);
                    setDesktopSlot(i,"");
                }
            }
        }
        return desktopBooks;
    }
}
