package com.zyfdroid.epub.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zyfdroid.epub.R;

/**
 * Created by ZYFDroid on 2020-06-19.
 */

public class CinematicProgressDialog extends AppCompatDialog {
    String[] tips;

    public CinematicProgressDialog(@NonNull Context context) {
        super(new ContextThemeWrapper(context, R.style.CinematicDialog));

        setContentView(R.layout.dialog_cinematic_progress);

        tips = context.getResources().getStringArray(R.array.tips);
        initUi();
    }


    TextView txtProgressHint = null;
    TextView txtProgressMessage = null;
    ProgressBar progressTime = null;

    ImageView animView  = null;

    void initUi(){
        txtProgressHint = (TextView)findViewById(R.id.txtProgressHint);
        txtProgressMessage = (TextView)findViewById(R.id.txtProgressMessage);
        progressTime = (ProgressBar)findViewById(R.id.progressTime);
        animView = (ImageView) findViewById(R.id.imgAnim);
        txtProgressHint.setText(tips[(int)(tips.length * Math.random())]);
    }

    Handler hWnd = new Handler();


    public static void setPromptWin(CinematicProgressDialog dia) {
        Window win = dia.getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        win.setGravity(Gravity.LEFT | Gravity.TOP);
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.horizontalMargin=0;
        lp.verticalMargin=0;
        lp.dimAmount=1;
        win.setAttributes(lp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        animView.setBackgroundResource(R.drawable.anim_book);
        hWnd.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((AnimationDrawable)animView.getBackground()).start();
            }
        },300);
        //txtProgressHint.setText("Tips: Test tips");
    }

    public void setMessage(final String str){
        hWnd.post(new Runnable() {
            @Override
            public void run() {
                txtProgressMessage.setText(str);
            }
        });
    }

    public void setProgress(final int now,final int max){
        hWnd.post(new Runnable() {
            @Override
            public void run() {
                progressTime.setMax(max);
                progressTime.setProgress(now);
            }
        });

    }


}
