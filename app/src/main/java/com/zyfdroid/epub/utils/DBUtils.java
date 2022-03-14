package com.zyfdroid.epub.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.AndroidRuntimeException;
import android.util.Log;

import com.zyfdroid.epub.R;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZYFDroid on 2020-06-16.
 */


public class DBUtils extends SQLiteOpenHelper {

    public String getBookRoot(){
        return new File(Environment.getExternalStorageDirectory(),"Books").getAbsolutePath();
    }

    private static final int SQL_VERSION = 1;
    private static final String TAG  = "DBUtils";
    private DBUtils(Context context) {
        super(context.getApplicationContext(), "bookdata0", null, SQL_VERSION);
    }

    private static DBUtils mInstance = null;
    public static void init(Context ctx){
        if(null!=mInstance){mInstance.close();}
        mInstance = new DBUtils(ctx);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db,-1,SQL_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int curversion = oldVersion+1;
        while (curversion<=newVersion){
            if(curversion==1){
                // type=0: this is a book;
                // type=1: this is a folder/bookshelf query sub uuid for more books. the Root uuid is 0;
                // type=2: the book has complete reading.
                db.execSQL("create table library(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,uuid text,type integer,parent_uuid text,display_name text,path text,lastopen bigint default 0)");
                db.execSQL("create table bookmarks(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,bookid text,epubcfi text,slot integer default 0,name text,savetime bigint default 0)");
                db.execSQL("insert into library(uuid,type,display_name,path) values(?,?,?,?)",new Object[]{"0",1,"图书馆",getBookRoot()});
                Log.d(TAG, "onUpgrade: now execute version "+curversion);
            }
            curversion++;
        }
    }

    public static void execSql(String sql,Object... args){
        SQLiteDatabase db = mInstance.getWritableDatabase();
        db.execSQL(sql,args);
    }

    public static int getCount(String sql,String... args){
        SQLiteDatabase db = mInstance.getWritableDatabase();
        Cursor c = db.rawQuery("select * from library where "+sql,args);
        int count = c.getCount();
        c.close();
        return count;
    }
    public static int getCount2(String sql,String... args){
        SQLiteDatabase db = mInstance.getWritableDatabase();
        Cursor c = db.rawQuery("select * from bookmarks where "+sql,args);
        int count = c.getCount();
        c.close();
        return count;
    }


    public static void InsertBooks(List<BookEntry> books){
        for (BookEntry b :books) {
            mInstance.execSql("insert into library(uuid,type,parent_uuid,display_name,path,lastopen) values(?,?,?,?,?,?)",
                    b.getUUID(),
                    b.getType(),
                    b.getParentUUID(),
                    b.getDisplayName(),
                    b.getPath(),
                    b.getLastOpenTime()
            );
        }
    }

    public static Cursor rawQuery(String sql,String... args){
        SQLiteDatabase db = mInstance.getWritableDatabase();
        return db.rawQuery(sql,args);
    }

    public static List<BookEntry> queryBooks(String sql,String... args){
        Cursor c = rawQuery("select id,uuid,type,parent_uuid,display_name,path,lastopen from library where "+sql+"",args);
        ArrayList<BookEntry> books  = new ArrayList<>();
        if(c.moveToFirst()){
            do{
                books.add(BookEntry.readFromDB(c.getInt(0),c.getString(1),c.getInt(2),c.getString(3),c.getString(4),c.getString(5),c.getLong(6)));
            }while (c.moveToNext());
        }
        c.close();
        return books;
    }

    public static List<BookEntry> queryFoldersNotEmpty(){
        Cursor c = rawQuery("select id,uuid,type,parent_uuid,display_name,path,lastopen from library as lib where 0 < (select count(*) from library where parent_uuid=lib.uuid and type=0)  order by display_name");
        ArrayList<BookEntry> books  = new ArrayList<>();
        if(c.moveToFirst()){
            do{
                books.add(BookEntry.readFromDB(c.getInt(0),c.getString(1),c.getInt(2),c.getString(3),c.getString(4),c.getString(5),c.getLong(6)));
            }while (c.moveToNext());
        }
        c.close();
        return books;
    }


    public static List<BookMark> queryBookmarks(Context ctx,String bookid){
        Cursor c = rawQuery("select id,bookid,slot,epubcfi,name,savetime from bookmarks where bookid=? order by slot",bookid);
        List<BookMark> bookmarks = new ArrayList<>();
        for (int i = 0; i <= 11; i++) {
            bookmarks.add(new BookMark(-1,bookid,i,"",ctx.getString(R.string.save_empty),-1));
        }
        if(c.moveToFirst()){
            do{
                BookMark bm = new BookMark(c.getInt(0),c.getString(1),c.getInt(2),c.getString(3),c.getString(4),c.getLong(5));
                bookmarks.set(bm.slot,bm);
            }while (c.moveToNext());
        }
        return bookmarks;
    }


    public static void autoSave(String bookId,String cfi,String title){
        setBookmark(bookId,0,""+title,cfi);
    }

    public static BookMark autoLoad(Context ctx,String bookId){
        return queryBookmarks(ctx,bookId).get(0);
    }

    public static void quickSave(String bookId,String cfi,String title){
        setBookmark(bookId,1,""+title,cfi);
    }

    public static BookMark quickLoad(Context ctx,String bookId){
        return queryBookmarks(ctx,bookId).get(1);
    }

    public static void setBookmark(String bookId,int slot,String name,String epubCfi){
        if(getCount2("bookid=? and slot=?",""+bookId,""+slot) > 0){
            execSql("update bookmarks set name=? where bookid=? and slot=?",name,""+bookId,""+slot);
            execSql("update bookmarks set epubcfi=? where bookid=? and slot=?",epubCfi,""+bookId,""+slot);
            execSql("update bookmarks set savetime=? where bookid=? and slot=?",System.currentTimeMillis()+"",""+bookId,""+slot);
        }
        else{
            execSql("insert into bookmarks(bookid,slot,name,epubcfi,savetime) values(?,?,?,?,?)",bookId+"",slot+"",name,epubCfi,System.currentTimeMillis());
        }
    }

    public static void deleteBookmark(String bookId,int slot){
        execSql("delete from bookmarks where bookid=? and slot=?",""+bookId,""+slot);
    }

    public static class BookMark{
        int id;
        String bookid;
        int slot;
        String epubcft, name;
        long saveTime;

        public long getSaveTime() {
            return saveTime;
        }

        public void setSaveTime(long saveTime) {
            this.saveTime = saveTime;
        }

        public BookMark(int id, String bookid, int slot, String epubcft, String name, long savetime) {
            this.id = id;
            this.bookid = bookid;
            this.slot = slot;
            this.epubcft = epubcft;
            this.name = name;
            this.saveTime = savetime;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getBookid() {
            return bookid;
        }

        public void setBookid(String bookid) {
            this.bookid = bookid;
        }

        public int getSlot() {
            return slot;
        }

        public void setSlot(int slot) {
            this.slot = slot;
        }

        public String getEpubcft() {
            return epubcft;
        }

        public void setEpubcft(String epubcft) {
            this.epubcft = epubcft;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class BookEntry{
        public static final String ROOT_UUID="0";
        public static final int TYPE_BOOK = 0;
        public static final int TYPE_BOOK_COMPLETE = 2;
        public static final int TYPE_FOLDER = 1;
        int id=-1;
        String UUID;
        int type;
        String parentUUID;
        String displayName;
        String path;
        long lastOpenTime;

        private BookEntry(int id,String UUID, int type, String parentUUID, String displayName, String path, long lastOpenTime) {
            this.UUID = UUID;
            this.type = type;
            this.parentUUID = parentUUID;
            this.displayName = displayName;
            this.path = path;
            this.lastOpenTime = lastOpenTime;
        }

        public static BookEntry createBook(String parentUUID,String title,String path){
            return new BookEntry(-1, md5(path),TYPE_BOOK,parentUUID,title,path,System.currentTimeMillis());
        }

        public static BookEntry createFolder(String parentUUID,String path){
            File f = new File(path);
            return new BookEntry(-1, md5(path),TYPE_FOLDER,parentUUID,f.getName(),path,System.currentTimeMillis());
        }

        public static BookEntry readFromDB(int id,String UUID, int type, String parentUUID, String displayName, String path, long lastOpenTime){
            return new BookEntry(id,UUID, type, parentUUID,displayName, path, lastOpenTime);
        }

        //region Getter and setter

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUUID() {
            return UUID;
        }

        public void setUUID(String UUID) {
            this.UUID = UUID;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getParentUUID() {
            return parentUUID;
        }

        public void setParentUUID(String parentUUID) {
            this.parentUUID = parentUUID;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getLastOpenTime() {
            return lastOpenTime;
        }

        public void setLastOpenTime(long lastOpenTime) {
            this.lastOpenTime = lastOpenTime;
        }

        //endregion
    }


    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AndroidRuntimeException(e);
        }
    }
}
