package com.zyfdroid.epub.views;

import android.app.Activity;
import android.content.Context;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.zyfdroid.epub.utils.SpUtils;

public class MyABDrawerToggle extends ActionBarDrawerToggle {
    private Context ctx;
    private DrawerLayout drwMain;
    public MyABDrawerToggle(Activity activity, DrawerLayout drawerLayout, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
        super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
        ctx=activity;
        drwMain = drawerLayout;
    }

    public MyABDrawerToggle(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
        super(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes);
        ctx=activity;
        drwMain = drawerLayout;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(ctx, "aireufghauirg", Toast.LENGTH_SHORT).show();
        if(SpUtils.getInstance(ctx).getEinkMode()){
            if (item != null && item.getItemId() == android.R.id.home) {
                if(drwMain.isDrawerOpen(GravityCompat.START)){
                    drwMain.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    drwMain.closeDrawer(GravityCompat.START);
                    drwMain.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                }
                else{
                    drwMain.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    drwMain.openDrawer(GravityCompat.START);
                    drwMain.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
                }
                return true;
            }
            return false;
        }
        return super.onOptionsItemSelected(item);
    }
}
