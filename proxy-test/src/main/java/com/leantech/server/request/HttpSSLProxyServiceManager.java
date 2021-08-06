package com.leantech.server.request;

import com.leantech.server.io.InputStreamConsumer;
import com.leantech.server.io.SocketCloseHandler;
import com.leantech.server.utils.ThreadPoolManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.Semaphore;

public class HttpSSLProxyServiceManager implements ProxyService {

    private Socket client;
    private Semaphore clientSemaphore;

    public HttpSSLProxyServiceManager(Socket client, Semaphore clientSemaphore) {
        super();
        this.client = client;
        this.clientSemaphore = clientSemaphore;
    }

    public void proxy(Header request) {
        Socket to;
        Properties properties = new Properties();
        try {
            if (Boolean.valueOf(properties.getProperty("USE_PROXY_SOCKS", "False"))) {
                to = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(properties.getProperty("SOCKS_PROXY_HOST",""),
                        Integer.valueOf(properties.getProperty("SOCKS_PROXY_PORT", "0")))));
            } else {
                to = new Socket();
            }

            to.connect(new InetSocketAddress(request.getHost(), request.getPort()));

            this.client.getOutputStream().write("HTTP/1.1 200 Connection established\r\nProxy-connection: Keep-alive\r\n\r\n".getBytes());
            this.client.getOutputStream().flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            this.clientSemaphore.release(2);
            return;
        }

        Semaphore clientSemaphore = new Semaphore(0, true);
        Semaphore toSemaphore = new Semaphore(0, true);

        SocketCloseHandler toSocketCloseHandler = new SocketCloseHandler(to);
        toSocketCloseHandler.addSemaphore(clientSemaphore);
        toSocketCloseHandler.addSemaphore(toSemaphore);

        try {
            ThreadPoolManager.getInstance().execute(new InputStreamConsumer(this.client.getInputStream(), to.getOutputStream(), null, clientSemaphore));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        try {
            ThreadPoolManager.getInstance().execute(new InputStreamConsumer(to.getInputStream(), this.client.getOutputStream(), null, toSemaphore));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        ThreadPoolManager.getInstance().execute(toSocketCloseHandler);

        try {
            clientSemaphore.acquire();
            toSemaphore.acquire();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

        this.clientSemaphore.release(2);
    }
}
