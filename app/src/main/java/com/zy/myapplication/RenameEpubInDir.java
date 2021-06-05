package com.zy.myapplication;

import com.zy.myapplication.epub.BookModel;
import com.zy.myapplication.epub.ReadEpubHeadInfo;
import com.zy.myapplication.utils.LogUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class RenameEpubInDir {
    public static void main(String[] args) {
        final String path = args[0];
        LogUtils.stdout=true;
        ReadEpubHeadInfo reader = new ReadEpubHeadInfo();
        reader.setSaveInfoPath(new File(path,"cache").getAbsolutePath());
        File[] epubs = new File(path).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getAbsolutePath().toLowerCase().endsWith(".epub");
            }
        });
        for (int i = 0; i < epubs.length; i++) {
            File epub = epubs[i];
            System.out.print("Rename "+epub.getName());
            BookModel bm = ReadEpubHeadInfo.getePubBook(epub.getAbsolutePath());
            String name = bm.getName();
            String author = bm.getAuthor();
            String generatedName = mkFilename(name,author);
            epub.renameTo(new File(epub.getParent(),generatedName));
            System.out.println(" To "+generatedName);
        }
    }

    private static String mkFilename(String name, String author) {
        String readyName = name+" - "+author;
        String gbkSafeString = gbkSafe(readyName);
        gbkSafeString = gbkSafeString.replace('?','_');
        gbkSafeString = gbkSafeString.replace('/','_');
        gbkSafeString = gbkSafeString.replace('\\','_');
        gbkSafeString = gbkSafeString.replace('|','_');
        gbkSafeString = gbkSafeString.replace('\'','_');
        gbkSafeString = gbkSafeString.replace('\"','_');
        gbkSafeString = gbkSafeString.replace(':','_');
        gbkSafeString = gbkSafeString.replace(';','_');
        gbkSafeString = gbkSafeString.replace('<','_');
        gbkSafeString = gbkSafeString.replace('>','_');
        gbkSafeString = gbkSafeString.replace('*','_');
        if(gbkSafeString.length()>32){
            gbkSafeString = gbkSafeString.substring(0,32)+"…";
        }
        return gbkSafeString+".epub";
    }

    public static String gbkSafe(String in){
        try {
            String gbkSafeName = new String(in.getBytes("GBK"),"GBK");
            return gbkSafeName.replace("?","_");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return in;
    }

}
