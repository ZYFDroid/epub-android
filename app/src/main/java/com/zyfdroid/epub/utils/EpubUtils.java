package com.zyfdroid.epub.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;


import com.zyfdroid.epub.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Created by ZYFDroid on 2020-06-18.
 */

public class EpubUtils {
    public static String cacheImagePath = null;
    public static String cacheBookPath = null;

    public static final String FLAG_EXTRACTED = "extracted.flag";

    public static String bookRoot = null;
    public static void initFolders(Context ctx){
        File coverCache = new File(ctx.getFilesDir(),"cover");
        File bookCache = new File(ctx.getCacheDir(),"book");
        if(!coverCache.exists()){coverCache.mkdirs();}
        if(!bookCache.exists()){bookCache.mkdirs();}
        cacheImagePath = coverCache.getAbsolutePath();
        cacheBookPath = bookCache.getAbsolutePath();
        File bookRootFile = new File(Environment.getExternalStorageDirectory(),"Books");
        if(!bookRootFile.exists()){bookRootFile.mkdirs();}
        bookRoot = bookRootFile.getAbsolutePath();
    }


    public static Bitmap getBookCoverInCache(DBUtils.BookEntry entry){
        File cacheFile = new File(cacheImagePath,entry.getUUID()+".jpg");
        if(cacheFile.exists()){
            try {
                return BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
            }catch (Exception ex){
                Log.w("EPUB","Cannot load cache?",ex);
                try{
                    cacheFile.delete();
                }catch (Exception ex2){
                    ex2.printStackTrace();
                }
            }
        }
        return makeCoverImage(entry.getDisplayName());
    }

    public static Bitmap makeCoverImage(String title) {
        Bitmap bmp = Bitmap.createBitmap(150, 200, Bitmap.Config.RGB_565);
        Canvas g = new Canvas(bmp);
        Paint white = new Paint();
        white.setColor(Color.WHITE);
        white.setStyle(Paint.Style.FILL);
        g.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), white);
        TextPaint tp = new TextPaint();
        tp.setTextSize(20);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setColor(Color.BLACK);
        tp.setAntiAlias(true);
        g.translate(bmp.getWidth() / 2, bmp.getHeight() / 2.5f);
        StaticLayout staticLayout = new StaticLayout(title, tp, bmp.getWidth() - 10, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        g.translate(0, -staticLayout.getHeight() / 2);
        staticLayout.draw(g);
        return bmp;
    }

    public static void ExtractBook(final Context ctx, DBUtils.BookEntry be, String destinationLocation,final BookLoadCallback callback){
        new AsyncTask<String, Void, Boolean>() {
            CinematicProgressDialog pdd;
            void delay(int mill){
                try {
                    Thread.sleep(mill);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                pdd = new CinematicProgressDialog(ctx);
                CinematicProgressDialog.setPromptWin(pdd);
                pdd.show();
                CinematicProgressDialog.setPromptWin(pdd);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                pdd.dismiss();
                callback.onResult(aBoolean);
            }

            @Override
            protected Boolean doInBackground(String... strings) {
                try {
                    ZipFile zf = new ZipFile(new File(strings[0]),ZipFile.OPEN_READ);
                    String target  = strings[1];
                    ArrayList<ZipEntry> zes = new ArrayList<>();
                    Enumeration<?> z = zf.entries();
                    pdd.setMessage(ctx.getString(R.string.load_prepare));
                    pdd.setProgress(0,1);
                    while (z.hasMoreElements()){
                        ZipEntry ze = (ZipEntry) z.nextElement();
                        if(ze.getName().contains("../")){Log.e("What the fuck zip file!",ze.getName());continue;}
                        zes.add(ze);
                    }
                    pdd.setProgress(1,1);
                    delay(200);
                    pdd.setMessage(ctx.getString(R.string.load_loading));
                    for (int i = 0; i < zes.size(); i++) {
                        pdd.setProgress(i+1,zes.size());
                        ZipEntry ze = zes.get(i);
                        File dest = new File(target,ze.getName());
                        if(ze.isDirectory()){
                            dest.mkdirs();
                        }
                        else{
                            if(dest.exists()){dest.delete();}
                            if(!dest.getParentFile().exists()){dest.getParentFile().mkdirs();}
                            dest.createNewFile();
                            FileOutputStream os = new FileOutputStream(dest);
                            InputStream is = zf.getInputStream(ze);

                            byte[] buffer = new byte[1024];
                            int len=0;
                            while ((len = is.read(buffer))>0){
                                os.write(buffer,0,len);
                            }
                            is.close();
                            os.close();
                        }
                    }
                    File flag = new File(target,FLAG_EXTRACTED);
                    if(!flag.exists()){flag.createNewFile();}
                    zf.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    pdd.setMessage(ctx.getString(R.string.load_error)+e.getMessage());
                    pdd.setProgress(0,1);
                    delay(1500);
                    return false;
                }
            }
        }.execute(be.path,destinationLocation);
    }

    public interface BookLoadCallback{
        void onResult(boolean b);
    }
}
