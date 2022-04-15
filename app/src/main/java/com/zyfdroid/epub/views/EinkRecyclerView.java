package com.zyfdroid.epub.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.zyfdroid.epub.utils.ViewUtils;

public class EinkRecyclerView extends RecyclerView {
    public EinkRecyclerView(@NonNull  Context context) {
        super(context);
    }

    public EinkRecyclerView(@NonNull  Context context, @Nullable  AttributeSet attrs) {
        super(context, attrs);
    }

    public EinkRecyclerView(@NonNull  Context context, @Nullable  AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void startEinkMode(int orientation,int scrollstep){
        isEinkMode = true;
        threhold = ViewUtils.dip2px(getContext(),20);
        this.orientation = orientation;
        this.scrollstep = scrollstep;
        threhold = threhold * threhold;
        Log.d("EinkRecyclerView",""+scrollstep);
    }
    private boolean isEinkMode = false;
    private int orientation = LinearLayout.VERTICAL;
    private int scrollstep=100;
    private int threhold = 100;
    private float pressedX = 0,pressedY=0;
    boolean moved = false;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if(isEinkMode){
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                pressedX = e.getX();pressedY = e.getY();
                moved = false;
                return false;
            }
            if(e.getAction()==MotionEvent.ACTION_MOVE){
                float cx = e.getX();float cy = e.getY();
                float dx = cx-pressedX,dy = cy-pressedY;
                float d = dx * dx + dy * dy;
                if(d > threhold){
                    moved = true;
                }
            }
            if(e.getAction()==MotionEvent.ACTION_UP){

                float cx = e.getX();float cy = e.getY();
                float dx = cx-pressedX,dy = cy-pressedY;
                float d = dx * dx + dy * dy;
                if(d > threhold){
                    int fx = 0;int fy = 0;
                    if(orientation == LinearLayout.VERTICAL){
                        fy = dy > 0 ? -1 : 1;
                    }
                    if(orientation == LinearLayout.HORIZONTAL){
                        fx = dx > 0 ? -1 : 1;
                    }
                    this.scrollBy(fx*scrollstep,fy*scrollstep);
                    moved = false;
                    return true;
                }
                else{
                    moved = false;
                }
            }
            return moved;
        }
        else {
            return super.onTouchEvent(e);
        }
    }
    public void pageUp(){
        scrollPage(1);
    }
    public void pageDown(){
        scrollPage(-1);
    }

    private void scrollPage(int p){
        int fx = 0;int fy = 0;
        if(orientation == LinearLayout.VERTICAL){
            fy =p > 0 ? -1 : 1;
        }
        if(orientation == LinearLayout.HORIZONTAL){
            fx = p > 0 ? -1 : 1;
        }
        this.scrollBy(fx*scrollstep,fy*scrollstep);
        moved = false;
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if(isEinkMode){
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                pressedX = e.getX();pressedY = e.getY();
                moved = false;
                return false;
            }
            if(e.getAction()==MotionEvent.ACTION_MOVE){
                float cx = e.getX();float cy = e.getY();
                float dx = cx-pressedX,dy = cy-pressedY;
                float d = dx * dx + dy * dy;
                if(d > threhold){
                    moved = true;
                }
            }
            return moved;
        }
        else {
            return super.onInterceptTouchEvent(e);
        }
    }
}
