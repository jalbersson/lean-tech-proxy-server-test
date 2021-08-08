package com.leantech.server.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HeadersPrinter {
    private int algo;

    public StringBuilder getSite(String site){
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();

        try{
            URL url = new URL(site);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            printHeader(site, httpURLConnection.getHeaderFields());

        } catch (Exception exception){
            System.out.println("Error on get site method: " + exception.getMessage());
        }
        return stringBuilder;
    }

    public void printHeader(String site, Map<String, List<String>> headers){
        System.out.println("Headers for website " + site);
        headers.entrySet().forEach(entry -> System.out.println("entry: " + entry));
        System.out.println("End of headers");
    }
}
