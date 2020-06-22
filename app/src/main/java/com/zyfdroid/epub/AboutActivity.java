package com.zyfdroid.epub;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView wv = new WebView(this);
        wv.getSettings().setUseWideViewPort(true);
        wv.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if(request.getUrl().getScheme().startsWith("http")){
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(request.getUrl());
                    startActivity(i);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
        String filename = getString(R.string.about_file_name);
        wv.loadUrl("file:///android_asset/"+filename+".html");
        setContentView(wv);
    }
}