package com.leantech.server.request;

public interface Header {
    public String getHost();
    public HttpRequestMethodType getRequestMethodEnum();
    public Integer getPort();
}
