package com.zyfdroid.epub.utils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.zy.myapplication.epub.BookModel;
import com.zy.myapplication.epub.ReadEpubHeadInfo;
import com.zyfdroid.epub.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by ZYFDroid on 2020-06-18.
 */

public class BookScanner {

    public static final String TAG="ScanBook";

    private static class TempBookInfo{
        String title;
        Bitmap cover;

        public TempBookInfo(String title, Bitmap cover) {
            this.title = title;
            this.cover = cover;
        }
    }

    public interface OnBookFinished{
        void onFinish();
    }

    private static TempBookInfo readBookInfo(File file){
        Log.i("BookScanner",file.getAbsolutePath());
        String name="";
        String title="";
        try {
            BookModel model = ReadEpubHeadInfo.getePubBook(file.getAbsolutePath());
            name=model.getName();
            title=name+" - "+model.getAuthor();
            Bitmap bmp;
            try {

                bmp = BitmapFactory.decodeFile(model.getCover());
                if(null==bmp){
                    bmp =EpubUtils.makeCoverImage(name+"\r\n"+model.getAuthor());
                }
            }catch (Exception ex){
                bmp =EpubUtils.makeCoverImage(name+"\r\n"+model.getAuthor());
            }
            return new TempBookInfo(title,bmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(name.length()>0){return new TempBookInfo(title,EpubUtils.makeCoverImage(name));}
        return new TempBookInfo(file.getName(),EpubUtils.makeCoverImage(file.getName()));
    }

    public static void scanBooks(final Context ctx,String root,final OnBookFinished finishListener,final boolean fullscan){
        ReadEpubHeadInfo.setCachePath(ctx);
        AsyncTask<String,String,String> scanTask = new AsyncTask<String, String, String>() {

            HashMap<String,String> pathToUUID = new HashMap<String, String>();
            List<DBUtils.BookEntry> bookEntries = new ArrayList<DBUtils.BookEntry>();
            List<DBUtils.BookEntry> folderEntries = new ArrayList<DBUtils.BookEntry>();

            List<String> path = new ArrayList<String>();
            List<String> bookPath = new ArrayList<String>();
            void rescurePath(String root){
                File f = new File(root);
                if(f==null || f.getName().startsWith(".")){
                    return;
                }
                File[] listfiles = f.listFiles();
                if(listfiles==null){return;}
                for(File subf : listfiles){
                    if(subf.isDirectory()){
                        rescurePath(subf.getAbsolutePath());
                        path.add(subf.getAbsolutePath());
                        Log.d(TAG, "rescurePath: "+subf.getAbsolutePath());
                    }else{
                        if(subf.getName().toLowerCase().endsWith(".epub")){
                            Log.d(TAG, "rescureFile: "+subf.getAbsolutePath());
                            bookPath.add(subf.getAbsolutePath());
                        }
                    }
                }


            }

            void delay(int mill){
                try {
                    Thread.sleep(mill);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @SuppressLint("DefaultLocale")
            @Override
            protected String doInBackground(String... params) {
                try {
                    pdd.setProgress(0,100);
                    publishProgress(ctx.getString(R.string.scan_rescure_dictionary));
                    delay(300);
                    rescurePath(params[0]);
                    publishProgress(String.format(ctx.getString(R.string.scan_found), bookPath.size(), path.size()));

                    pdd.setProgress(100,100);
                    delay(300);
                    publishProgress(ctx.getString(R.string.scan_reading_database));

                    pdd.setProgress(0,100);

                    if(fullscan){
                        DBUtils.execSql("delete from library");
                    }

                    List<DBUtils.BookEntry> pathInDb = DBUtils.queryBooks("type=?", String.valueOf(DBUtils.BookEntry.TYPE_FOLDER));
                    for (DBUtils.BookEntry pid : pathInDb) {
                        pathToUUID.put(pid.getPath(), pid.getUUID());
                    }
                    List<DBUtils.BookEntry> newPaths = new ArrayList<DBUtils.BookEntry>();
                    for (String p : path) {
                        if (!pathToUUID.containsKey(p)) {
                            newPaths.add(DBUtils.BookEntry.createFolder("", p));
                        }
                    }
                    for (DBUtils.BookEntry pid : newPaths) {
                        pathToUUID.put(pid.getPath(), pid.getUUID());
                    }
                    for (DBUtils.BookEntry pid : newPaths) {
                        if (TextUtils.isEmpty(pid.getParentUUID())) {
                            String parentPath = new File(pid.getPath()).getParentFile().getAbsolutePath();
                            String path2uuid = pathToUUID.get(parentPath);
                            pid.setParentUUID(path2uuid != null ? path2uuid : DBUtils.BookEntry.ROOT_UUID);
                        }
                    }
                    folderEntries.addAll(newPaths);

                    pdd.setProgress(3,4);
                    List<DBUtils.BookEntry> bookInDb = DBUtils.queryBooks("type=? or type=?", String.valueOf(DBUtils.BookEntry.TYPE_BOOK),String.valueOf(DBUtils.BookEntry.TYPE_BOOK_COMPLETE));

                    List<String> bookPathInDb = new ArrayList<String>();
                    for (DBUtils.BookEntry bk :
                            bookInDb) {
                        bookPathInDb.add(bk.getPath());
                    }

                    List<String> newBookPathList = new ArrayList<String>();
                    for (String newBookPath : bookPath) {
                        if (!bookPathInDb.contains(newBookPath)) {
                            newBookPathList.add(newBookPath);
                        }
                    }
                    publishProgress(String.format(ctx.getString(R.string.scan_going_to_add), newBookPathList.size()));

                    pdd.setProgress(4,4);
                    delay(200);
                    publishProgress(ctx.getString(R.string.scan_begin_adding));

                    pdd.setProgress(0,4);
                    delay(200);

                    int success = 0, deleted = 0;

                    for (int i = 0; i < newBookPathList.size(); i++) {

                        pdd.setProgress(i,newBookPathList.size());
                        publishProgress(String.format(ctx.getString(R.string.scan_add_progress), i + 1, newBookPathList.size()));
                        try {
                            File bf = new File(newBookPathList.get(i));
                            String parentPath = bf.getParentFile().getAbsolutePath();
                            TempBookInfo readinfo = readBookInfo(bf);
                            String path2uuid = pathToUUID.get(parentPath);
                            DBUtils.BookEntry tmpEntry = DBUtils.BookEntry.createBook(path2uuid != null ? path2uuid : DBUtils.BookEntry.ROOT_UUID, readinfo.title, bf.getAbsolutePath());
                            tmpEntry.lastOpenTime = bf.lastModified();
                            String coverPathName = EpubUtils.cacheImagePath + "/" + tmpEntry.getUUID() + ".jpg";
                            File coverFile = new File(coverPathName);
                            if(coverFile.exists()){coverFile.delete();}
                            coverFile.createNewFile();
                            readinfo.cover.compress(Bitmap.CompressFormat.JPEG, 95, new FileOutputStream(coverPathName));
                            bookEntries.add(tmpEntry);
                            readinfo.cover.recycle();
                            success++;
                            if(i % 30 == 0){
                                System.gc();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    pdd.setProgress(4,4);
                    publishProgress(ctx.getString(R.string.scan_save_to_database));
                    delay(300);

                    pdd.setProgress(0,4);
                    DBUtils.InsertBooks(folderEntries);
                    DBUtils.InsertBooks(bookEntries);
                    publishProgress(ctx.getString(R.string.scan_removed));
                    delay(300);

                    pdd.setProgress(3,4);
                    deleted = cleanDB();
                    int removeEmptyPath = cleanEmptyDir();
                    while (removeEmptyPath>0){
                        removeEmptyPath = cleanEmptyDir();
                    }
                    pdd.setProgress(4,4);
                    publishProgress(String.format(ctx.getString(R.string.scan_report), success, deleted));
                    delay(1500);
                    return "";
                }catch (Exception ex){
                    ex.printStackTrace();
                    publishProgress(ctx.getString(R.string.scan_error) + ex.getMessage());
                    delay(1500);
                }
                return "";
            }

            int cleanDB(){
                int deleted = 0;
                List<String> deletionUUIDs = new ArrayList<String>();
                List<String> deletionIDs = new ArrayList<String>();
                List<DBUtils.BookEntry> bookInDb = DBUtils.queryBooks("1=1");
                List<DBUtils.BookEntry> completedBook = DBUtils.queryBooks("type=2");
                HashSet<String> hs = new HashSet<String>();
                for (DBUtils.BookEntry bk : completedBook){
                    hs.add(bk.getPath());
                }
                for (DBUtils.BookEntry bk : bookInDb) {
                    if(bk.getType()== DBUtils.BookEntry.TYPE_BOOK) {
                        if (!new File(bk.getPath()).exists()){
                            deletionUUIDs.add(bk.getUUID());
                            deleted++;
                        }
                        if(bk.getType() == 0 && hs.contains(bk.path)){
                            deletionIDs.add(bk.getUUID());
                        }
                    }
                }
                for (String delete : deletionUUIDs) {
                    DBUtils.execSql("delete from library where uuid=?",delete);
                    File imgCover = new File(EpubUtils.cacheImagePath+"/"+delete+".png");
                    if(imgCover.exists()){
                        imgCover.delete();
                    }
                }
                for (String delete : deletionIDs) {
                    String sql = "delete from library where type = 0 and uuid = ?";
                    DBUtils.execSql(sql,delete);
                    deleted++;
                }
                return deleted;
            }

            int cleanEmptyDir(){
                int deleted = 0;
                List<String> deletionUUIDs = new ArrayList<String>();
                List<DBUtils.BookEntry> bookInDb = DBUtils.queryBooks("type=?", String.valueOf(DBUtils.BookEntry.TYPE_FOLDER));
                for (DBUtils.BookEntry bk : bookInDb) {
                    if(bk.getUUID().equals(DBUtils.BookEntry.ROOT_UUID)){continue;}
                    if(DBUtils.getCount("parent_uuid=?",bk.getUUID())==0){
                        deletionUUIDs.add(bk.getUUID());
                    }
                }
                for (String delete : deletionUUIDs) {
                    DBUtils.execSql("delete from library where uuid=?",delete);
                    deleted++;
                }
                return deleted;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                if(null!=pdd){
                    pdd.setMessage(values[0]);
                }
            }
            CinematicProgressDialog pdd;
            @Override
            protected void onPreExecute() {
                pdd = new CinematicProgressDialog(ctx);
                CinematicProgressDialog.setPromptWin(pdd);
                pdd.show();
                pdd.setCanceledOnTouchOutside(false);
                CinematicProgressDialog.setPromptWin(pdd);
            }

            @Override
            protected void onPostExecute(String s) {
                pdd.dismiss();
                if(null!=finishListener){finishListener.onFinish();}
            }
        }.execute(root);

    }

}
