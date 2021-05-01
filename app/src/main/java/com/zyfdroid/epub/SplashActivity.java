package com.zyfdroid.epub;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.zyfdroid.epub.utils.SpUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SplashActivity extends AppCompatActivity {


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE" };


    public void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasStorage(){
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(this,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            return permission == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }



    Handler hWnd = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(hasStorage()) {
            jump();
        }else{
            verifyStoragePermissions(this);
        }
    }

    public void jump(){
        if(SpUtils.getInstance(this).firstRun()){
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().build());
            File targetFile = new File(Environment.getExternalStorageDirectory(),"Books");
            if(!targetFile.exists()){targetFile.mkdirs();}
            targetFile = new File(targetFile,"AliceInWonderland.epub");
            if(!targetFile.exists()){
                try {
                    targetFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    InputStream is = getResources().getAssets().open("alice.epub");
                    byte[] buffer = new byte[2048];
                    int len=0;
                    while ((len = is.read(buffer))>0){
                        fos.write(buffer,0,len);
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        hWnd.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, BookshelfActivity.class));
                overridePendingTransition(R.anim.anim_splash_fadein, R.anim.anim_splash_fadeout);
                finish();
            }
        }, 440);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_EXTERNAL_STORAGE){
            if(hasStorage()) {
                jump();
            }else{
                finish();
            }
        }
    }

}
