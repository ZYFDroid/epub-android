package com.zy.myapplication.epub;

/**
 * ePub基本信息
 */
public class BookModel {

    private String name;

    private String author;

    /**
     * 书封面图片路径
     */
    private String cover;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
    /**
     * 书封面图片路径
     */
    public String getCover() {
        return cover;
    }
    /**
     * 书封面图片路径
     */
    public void setCover(String cover) {
        this.cover = cover;
    }
}