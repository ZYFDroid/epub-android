package com.zyfdroid.epub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.zyfdroid.epub.utils.CinematicProgressDialog;
import com.zyfdroid.epub.utils.DBUtils;
import com.zyfdroid.epub.utils.SpUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.Buffer;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

public class SettingActivity extends AppCompatActivity {

    CheckBox chkOpenExternal;
    CheckBox chkAllowNight;
    CheckBox chkEink;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        chkOpenExternal = findViewById(R.id.chkExternalOpen);
        chkOpenExternal.setChecked(SpUtils.getInstance(this).shouldOpenWithExternalReader());
        chkAllowNight = findViewById(R.id.chkAllowNight);
        chkAllowNight.setChecked(SpUtils.getInstance(this).getAllowNightMode());

        chkEink = findViewById(R.id.chkEink);
        chkEink.setChecked(SpUtils.getInstance(this).getEinkMode());
        chkOpenExternal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SpUtils.getInstance(SettingActivity.this).setOpenWithExternalReader(isChecked);
            }
        });
        chkAllowNight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SpUtils.getInstance(SettingActivity.this).setAllowNightMode(isChecked);
            }
        });
        chkEink.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SpUtils.getInstance(SettingActivity.this).setEinkMode(isChecked);
            }
        });
    }

    public void clearCache(View view) {
        new AlertDialog.Builder(this).setMessage(R.string.setting_clean_cache_message).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                File f = new File(getCacheDir(),"book");
                delFileAndDir(f);
                f.mkdirs();
                Toast.makeText(SettingActivity.this, "清除成功。", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton(android.R.string.no,null).create().show();
    }

    public static void gotoAppDetailIntent(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }

    public void chooseFont(View view) {
        File fontDir = new File(new File(Environment.getExternalStorageDirectory(),"Books"),".font");
        if(!fontDir.exists()){
            fontDir.mkdirs();
        }
        if(!fontDir.isDirectory()){
            Toast.makeText(this, R.string.err_font_file, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] fonts = fontDir.list((dir, name) -> name.toLowerCase().endsWith(".ttf"));
        final String[] options = new String[fonts.length+1];
        options[0] = getString(R.string.default_font);
        for (int i = 0; i < fonts.length; i++) {
            options[i+1] = fonts[i];
        }
        if(fonts.length==0){
            Toast.makeText(this, R.string.err_no_font, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle(getString(R.string.fontselect)).setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==0){
                    SpUtils.getInstance(SettingActivity.this).setCustomFont("");
                }
                SpUtils.getInstance(SettingActivity.this).setCustomFont(options[which]);
                //Clear font cache
                delFileAndDir(new File(getCacheDir(),"bvc"));
            }
        }).setCancelable(true).create().show();
    }

    private void delFileAndDir(File file){
        if(file.exists()&&file.isDirectory()){
            File[] files = file.listFiles();
            for(File f:files){
                if(f.isFile()){
                    f.delete();
                }else {
                    delFileAndDir(f);
                }
                f.delete();
            }
        }
    }

    public void exportComplete(View view) {
        List<DBUtils.BookEntry> books = DBUtils.queryBooks("type=2");
        try {
            File f = new File(Environment.getExternalStorageDirectory(),"Books"+File.separator+getString(R.string.filename_complete_reading));
            if(f.exists()){
                f.delete();
            }
            PrintStream ps = new PrintStream(f);
            for (DBUtils.BookEntry be : books) {
                String filename = be.getPath();
                String uniqueKey = "";
                if(filename.contains("/")){
                    uniqueKey  = filename.substring(filename.lastIndexOf('/')+1);
                }
                else if(filename.contains("\\")){
                    uniqueKey  = filename.substring(filename.lastIndexOf('\\')+1);
                }
                else{
                    uniqueKey = filename;
                }
                ps.println(uniqueKey);
            }
            ps.close();
            Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: "+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    public void importComplete(View view) {

        try {
            File f = new File(Environment.getExternalStorageDirectory(),"Books"+File.separator+getString(R.string.filename_complete_reading));
            if(!f.exists()){
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }
            InputStream is = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            HashSet<String> readedBooks = new HashSet<>();
            String line;
            while ((line = br.readLine()) != null){
                if(!line.trim().isEmpty()){
                    readedBooks.add(line);
                }
            }
            br.close();
            List<DBUtils.BookEntry> books = DBUtils.queryBooks("type=0");
            for (DBUtils.BookEntry be : books) {
                String filename = be.getPath();
                String uniqueKey = "";
                if(filename.contains("/")){
                    uniqueKey  = filename.substring(filename.lastIndexOf('/')+1);
                }
                else if(filename.contains("\\")){
                    uniqueKey  = filename.substring(filename.lastIndexOf('\\')+1);
                }
                else{
                    uniqueKey = filename;
                }
                if(readedBooks.contains(uniqueKey)){
                    DBUtils.execSql("update library set type=2 where uuid=?",be.getUUID());
                }
            }

            Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: "+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}