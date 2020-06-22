package com.zyfdroid.epub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.zyfdroid.epub.utils.SpUtils;

public class SettingActivity extends AppCompatActivity {

    CheckBox chkOpenExternal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        chkOpenExternal = findViewById(R.id.chkExternalOpen);
        chkOpenExternal.setChecked(SpUtils.getInstance(this).shouldOpenWithExternalReader());
        chkOpenExternal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SpUtils.getInstance(SettingActivity.this).setOpenWithExternalReader(isChecked);
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
}