package com.example.servicedemo.model;

import com.google.gson.annotations.SerializedName;

public class Image {
    private int id;
    private String author;
    private String width;
    private String height;
    private String url;
    private String download_url;

    public int getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    public String getUrl() {
        return url;
    }

    public String getDownload_url() {
        return download_url;
    }

    @Override
    public String toString() {
        return "Image{" +
                "id=" + id +
                ", author='" + author + '\'' +
                ", width='" + width + '\'' +
                ", height='" + height + '\'' +
                ", url='" + url + '\'' +
                ", download_url='" + download_url + '\'' +
                '}';
    }
}
