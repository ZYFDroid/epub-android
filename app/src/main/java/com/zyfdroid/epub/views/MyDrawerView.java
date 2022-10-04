package com.zyfdroid.epub.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.zyfdroid.epub.utils.SpUtils;

public class MyDrawerView extends DrawerLayout {
    public MyDrawerView(@NonNull Context context) {
        super(context);
        init();
    }

    public MyDrawerView(@NonNull  Context context, @Nullable  AttributeSet attrs) {
        super(context, attrs);init();
    }

    public MyDrawerView(@NonNull  Context context, @Nullable  AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);init();
    }

    void init(){
        noAnim = SpUtils.getInstance(getContext()).getEinkMode();
        if(noAnim){
            setScrimColor(Color.TRANSPARENT);
            setDrawerElevation(3);
            addDrawerListener(new DrawerListener() {
               @Override
               public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                   if(!MyDrawerView.this.isDrawerOpen(GravityCompat.START)){
                       MyDrawerView.this.openDrawer(GravityCompat.START);
                       MyDrawerView.this.clearFocus();
                   }else{
                       MyDrawerView.this.closeDrawer(GravityCompat.START);
                       MyDrawerView.this.clearFocus();
                   }
               }

               @Override
               public void onDrawerOpened(@NonNull View drawerView) {

                   setDrawerLockMode(LOCK_MODE_LOCKED_OPEN);
               }

               @Override
               public void onDrawerClosed(@NonNull View drawerView) {
                   setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED);

               }

               @Override
               public void onDrawerStateChanged(int newState) {

               }
           });

            setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED);
        }
    }



    boolean noAnim = false;

    @Override
    public void openDrawer(@NonNull  View drawerView, boolean animate) {
        animate = animate && (!noAnim);
        super.openDrawer(drawerView, animate);
    }

    @Override
    public void closeDrawer(@NonNull  View drawerView, boolean animate) {

        animate = animate && (!noAnim);
        super.closeDrawer(drawerView, animate);
    }
}
