package com.zyfdroid.epub.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.zyfdroid.epub.R;

public class BatteryView extends TextView{

    public BatteryView(Context context) {
        super(context);
        init(null, 0);
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public BatteryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BatteryView.this.onReceive(context,intent);
            }
        };
        batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        setText("--%");
    }
    private IntentFilter batteryIntentFilter;
    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().registerReceiver(batteryReceiver,batteryIntentFilter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(batteryReceiver);
    }

    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,100);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);
        int percentage =  Math.round(level * 100f / scale);
        setText(percentage+"%");
    }
}