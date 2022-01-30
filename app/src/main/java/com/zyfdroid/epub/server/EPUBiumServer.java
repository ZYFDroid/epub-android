package com.zyfdroid.epub.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;
import com.zyfdroid.epub.R;
import com.zyfdroid.epub.utils.DBUtils;
import com.zyfdroid.epub.utils.EpubUtils;
import com.zyfdroid.epub.utils.SpUtils;
import com.zyfdroid.epub.utils.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import fi.iki.elonen.NanoHTTPD;

public class EPUBiumServer extends NanoHTTPD {
    Context appContext;
    DBUtils dbUtils;
    SpUtils spUtils;
    AssetManager asset;
    Gson gson;



    public EPUBiumServer(Context ctx) {
        super(2480);
        appContext = ctx;
        spUtils = SpUtils.getInstance(ctx);
        asset = appContext.getResources().getAssets();
        gson = new Gson();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String url = session.getUri();
        try {
            if(url.equals("/favicon.ico")){
                return handleAssetResponse(url, "/", "httpserver/");
            }
            if (url.equals("/")) {
                return newFixedLengthResponse("<script>window.location.replace('bookshelf/index.html');</script>");
            }
            if (url.startsWith("/bookshelf/")) {
                return handleAssetResponse(url, "/bookshelf", "httpserver");
            }
            if (url.startsWith("/read/")) {
                String bookurl = replaceFirst(url,"/read/");
                String[] urlstruct = bookurl.split(Pattern.quote("/"),2);
                if(urlstruct.length>1){
                    String bookuuid = urlstruct[0];
                    String suburl = urlstruct[1];
                    return serveRead(bookuuid,suburl,session);
                }
            }
            if (url.startsWith("/api/")) {
                return serveApi(replaceFirst(url, "/api/"));
            }
            if(url.startsWith("/common/")){
                return handleAssetResponse(url,"/common/","common/");
            }
            if(url.startsWith("/static/defaultFont.ttf")){
                String font = SpUtils.getInstance(appContext).getCustomFont();
                if(TextUtils.isEmpty(font)){
                    return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/html","404 Not Found");
                }
                FileInputStream fontStream = new FileInputStream(font);
                return newFixedLengthResponse(Response.Status.OK,mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(font)),fontStream,fontStream.available());
            }
            if(url.startsWith("/static/")){
                return handleAssetResponse(url,"/static/","epubjs/static/");
            }
        }catch (Exception ex){
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,"text/html",ex.toString());
        }
        return super.serve(session);
    }

    private Response serveRead(String bookuuid, String suburl,IHTTPSession session) {
        if(suburl.startsWith("book/")){
            String cachePath = new File(EpubUtils.cacheBookPath,bookuuid).getAbsolutePath();
            return handleFsResponse(suburl,"book/",cachePath+"/");
        }
        if(suburl.startsWith("api/")){
            return serveReadingApi(bookuuid,replaceFirst(suburl,"api/"),session);
        }
        return handleAssetResponse(suburl,"","epubjs/");
    }

    HashMap<String, DBUtils.BookMark> cachedBookmarks = new HashMap<>();

    public Response serveReadingApi(String bookuuid,String api,IHTTPSession session){
        if(api.startsWith("bmload/")){
            int requestId = Integer.parseInt(replaceFirst(api,"bmload/"));
            return newFixedLengthResponse(gson.toJson(DBUtils.queryBookmarks(appContext,bookuuid).get(requestId)));
        }
        if(api.equals("bmloadall")){
            return newFixedLengthResponse(gson.toJson(DBUtils.queryBookmarks(appContext,bookuuid)));
        }
        if(api.startsWith("bmsave/")){
            int requestId = Integer.parseInt(replaceFirst(api,"bmsave/"));
            Map<String,List<String>> queryparam = session.getParameters();
            String name = queryparam.get("name").get(0);
            String cfi = queryparam.get("cfi").get(0);
            DBUtils.setBookmark(bookuuid,requestId,name,cfi);
            return newFixedLengthResponse("OK");
        }
        if(api.equals("bookname")){
            return newFixedLengthResponse(DBUtils.queryBooks("uuid = ?",bookuuid).get(0).getDisplayName());
        }
        return notFound();
    }

    List<DBUtils.BookEntry> bookEntries;

    public Response serveApi(String apipath) throws IOException {
        if(apipath.equals("reportmode.js")){
            return newFixedLengthResponse(Response.Status.OK,"text/javascript","var reportMessage=function(a,b){parent.reportMessage(a,b);}");
        }
        if(apipath.equals("devname")){
            return newFixedLengthResponse(Build.MANUFACTURER+" "+Build.MODEL);
        }
        if(apipath.equals("library")){
            bookEntries = DBUtils.queryBooks("type=0 order by lastopen desc");
            return newFixedLengthResponse(gson.toJson(bookEntries));
        }
        if(apipath.equals("folders")){
            bookEntries = DBUtils.queryFoldersNotEmpty();
            return newFixedLengthResponse(gson.toJson(bookEntries));
        }
        if(apipath.startsWith("folder/")){
            String targetFolder = replaceFirst(apipath,"folder/");
            bookEntries = DBUtils.queryBooks("type=0 and parent_uuid = ? order by lastopen desc",targetFolder);
            return newFixedLengthResponse(gson.toJson(bookEntries));
        }
        if(apipath.startsWith("cover/")){
            String uuid = replaceFirst(apipath,"cover/");
            bookEntries = DBUtils.queryBooks("uuid = ?",uuid);
            Bitmap bmp = EpubUtils.getBookCoverInCache(bookEntries.get(0));
            Bitmap bmp2 = Bitmap.createScaledBitmap(bmp,120,200,false);
            bmp.recycle();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp2.compress(Bitmap.CompressFormat.JPEG,80,baos);
            ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
            safeClose(baos);
            bmp2.recycle();
            return newFixedLengthResponse(Response.Status.OK,"image/jpeg",bis,bis.available());
        }
        if(apipath.startsWith("open/")){
            String uuid = replaceFirst(apipath,"open/");
            bookEntries = DBUtils.queryBooks("uuid = ?",uuid);
            DBUtils.BookEntry b = bookEntries.get(0);
            ensureBookExtracted(b);
            DBUtils.execSql("update library set lastopen=? where uuid=?",System.currentTimeMillis(),b.getUUID());
            return newFixedLengthResponse("<script>window.location.replace('/read/"+b.getUUID()+"/read.html');</script>");
        }
        return notFound();
    }

    void ensureBookExtracted(DBUtils.BookEntry be) throws IOException {
        File dir =new File(EpubUtils.cacheBookPath,be.getUUID());
        File flag = new File(dir,EpubUtils.FLAG_EXTRACTED);
        if(!flag.exists()){
            extractBook(be.getPath(),dir.getAbsolutePath());
        }
    }

    void extractBook(String... strings) throws IOException {
        ZipFile zf = new ZipFile(new File(strings[0]),ZipFile.OPEN_READ);
        String target  = strings[1];
        ArrayList<ZipEntry> zes = new ArrayList<>();
        Enumeration<?> z = zf.entries();
        while (z.hasMoreElements()){
            ZipEntry ze = (ZipEntry) z.nextElement();
            if(ze.getName().contains("../")){
                Log.e("What the fuck zip file!",ze.getName());continue;}
            zes.add(ze);
        }
        for (int i = 0; i < zes.size(); i++) {
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
        File flag = new File(target,EpubUtils.FLAG_EXTRACTED);
        if(!flag.exists()){flag.createNewFile();}
        zf.close();
    }

    void safeClose(AutoCloseable c){
        if(c!=null){
            try {
                c.close();
            }catch (Exception ex){}
        }
    }

    static String replaceFirst(String txt,String prefix){return txt.substring(prefix.length());}
    MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    public Response handleAssetResponse(String path,String prefix,String assetprefix){
        String assetPath =assetprefix+ path.substring(prefix.length(),path.length());
        try{
            InputStream is = asset.open(assetPath);
            return newFixedLengthResponse(Response.Status.OK,mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path)),is,is.available());
        }catch (Exception ex){
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,"text/html",ex.toString());
        }
    };
    public Response handleFsResponse(String path,String prefix,String fsprefix){
        String assetPath =fsprefix+ path.substring(prefix.length(),path.length());
        try{
            InputStream is = new FileInputStream(assetPath);
            return newFixedLengthResponse(Response.Status.OK,mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path)),is,is.available());
        }catch (Exception ex){
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,"text/html",ex.toString());
        }
    };
    Response notFound(){return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/html","404 Not Found");}
}

