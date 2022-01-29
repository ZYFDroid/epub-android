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

import com.zyfdroid.epub.utils.SpUtils;

import java.io.File;
import java.io.FilenameFilter;

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
        new AlertDialog.Builder(this).setMessage(R.string.setting_clean_cache_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                gotoAppDetailIntent(SettingActivity.this);
            }
        }).create().show();
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
            }
        }).setCancelable(true).create().show();
    }
}