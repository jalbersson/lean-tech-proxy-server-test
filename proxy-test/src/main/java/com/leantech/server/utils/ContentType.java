package com.leantech.server.utils;

public enum ContentType {
    TEXT_PLAIN(new String("text/plain")),
    TEXT_HTML(new String("text/html")),
    TEXT_CSS(new String("text/css")),
    IMAGE_GIF(new String("image/gif")),
    IMAGE_PNG(new String("image/png")),
    IMAGE_JPEG(new String("image/jpeg")),
    APPLICATION_JAVASCRIPT(new String("application/javascript")),
    APPLICATION_XJAVASCRIPT(new String("application/x-javascript")),
    APPLICATION_OCTETSTREAM(new String("application/octet-stream")),
    APPLICATION_ZIP(new String("application/zip"));

    private String contentType;

    ContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String toString() {
        return this.contentType;
    }
}
