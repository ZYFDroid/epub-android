package com.zyfdroid.epub.utils;

public class TextUtils {

    public static String escapeText(String src)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (char chr : src.toCharArray())
        {
            switch (chr) {
                case '\0':     sb.append("\\0");break;
                case '\b':     sb.append("\\b");break;
                case '\t':     sb.append("\\t");break;
                case '\n':     sb.append("\\n");break;
                case '\u000B': sb.append("\\v");break;
                case '\u000C': sb.append("\\f");break;
                case '\r':     sb.append("\\f");break;
                case '\"':     sb.append("\\\"");break;
                case '\'':     sb.append("\\\'");break;
                case '\\':     sb.append("\\\\");break;
                default: sb.append(chr);break;
            }
        }
        return sb.append("\"").toString();
    }

    public static boolean isEmpty(String string) {
        return android.text.TextUtils.isEmpty(string);
    }

    public static String stripText(String in,int desiredLength){
        StringBuilder sb = new StringBuilder();
        int len = 0;
        for (int i = 0; i < in.length(); i++) {
            char chr = in.charAt(i);
            if(chr > 256){
                len += 2;
            }
            else{
                len += 1;
            }

            sb.append(chr);
            if(len > desiredLength){
                break;
            }
        }
        sb.append("...");
        return sb.toString();
    }
}
