package com.leantech.server;

import com.leantech.server.config.Configuration;
import com.leantech.server.config.ConfigurationManager;
import com.leantech.server.io.InputStreamConsumer;
import com.leantech.server.request.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class Server {
    public static void main(String[] args) {
        System.out.println("Starting server...");

        ConfigurationManager.getInstance().loadConfigurationFile("src/main/resources/http.json");
        Configuration configuration = ConfigurationManager.getInstance().getCurrentConfiguration();

        System.out.println("Local Port in use: " + configuration.getLocalPort());

        try {
            String host = "Proxy Server";

            // Start running the server
            runServer(host, configuration.getRemotePort(), configuration.getLocalPort());
        }
        catch (Exception exception)
        {
            System.out.println("Error while running the proxy server: " + exception.getMessage());
        }
    }

    public static void runServer(String host, int remoteport, int localport) throws IOException {
        // Creating a ServerSocket to listen for connections with
        ServerSocket s = new ServerSocket(localport);
        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];
        Semaphore clientSemaphore = new Semaphore(0, true);
        while (true) {
            Socket client = null, server = null;
            try{
                client = s.accept();
                final InputStream streamFromClient = client.getInputStream();

                ByteArrayOutputStream httpBytesHeaders = new ByteArrayOutputStream();

                try {
                    new InputStreamConsumer(client.getInputStream(), httpBytesHeaders, 1L).consume();
                } catch (IOException e) {
                    System.out.println("Error While consuming client input stream: " + e.getMessage());
                }

                Header requestHeaders = new HttpRequestHeaders(new String(httpBytesHeaders.toByteArray()));

                System.out.println("Request Headers:");
                System.out.println("Request Method: " + requestHeaders.getRequestMethodEnum());
                System.out.println("Host: " + requestHeaders.getHost());
                System.out.println("Port: " + requestHeaders.getPort());

                ProxyService proxy = null;

                if (requestHeaders.getRequestMethodEnum() == HttpRequestMethodType.GET
                        || requestHeaders.getRequestMethodEnum() == HttpRequestMethodType.POST) {
                    proxy = new HttpProxyServiceManager(client, clientSemaphore);
                } else if(requestHeaders.getRequestMethodEnum() == HttpRequestMethodType.CONNECT) {
                    proxy = new HttpSSLProxyServiceManager(client, clientSemaphore);
                } else {
                    System.out.println(requestHeaders);
                }

                if (proxy != null) {
                    proxy.proxy(requestHeaders);
                }
            } catch (Exception exception){

            }
        }
    }
}
