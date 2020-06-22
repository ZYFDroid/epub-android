package com.zyfdroid.epub;

import android.app.Application;

import com.zy.myapplication.epub.ReadEpubHeadInfo;
import com.zyfdroid.epub.utils.DBUtils;
import com.zyfdroid.epub.utils.EpubUtils;

public class Program extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DBUtils.init(this);
        ReadEpubHeadInfo.setCachePath(this);
        EpubUtils.initFolders(this);

    }
}
