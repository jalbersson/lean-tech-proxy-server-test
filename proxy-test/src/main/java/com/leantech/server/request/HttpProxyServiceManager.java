package com.leantech.server.request;

import com.leantech.server.io.InputStreamConsumer;
import com.leantech.server.io.SocketCloseHandler;
import com.leantech.server.utils.ThreadPoolManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.Semaphore;

public class HttpProxyServiceManager implements ProxyService {

    private Socket client;
    private Semaphore clientSemaphore;

    public HttpProxyServiceManager(Socket client, Semaphore clientSemaphore) {
        super();
        this.client = client;
        this.clientSemaphore = clientSemaphore;
    }

    public void proxy(Header request) {
        Socket to;
        OutputStream toOut;
        Properties properties = new Properties();
        try {
            if (Boolean.valueOf(properties.getProperty(new String("USE_PROXY_SOCKS"), new String("False")))) {
                to = new Socket(new Proxy(Proxy.Type.SOCKS,
                        new InetSocketAddress(properties.getProperty("SOCKS_PROXY_HOST", "")
                                , Integer.valueOf(properties.getProperty("SOCKS_PROXY_PORT", "")))));
            } else {
                to = new Socket();
            }

            to.connect(new InetSocketAddress(request.getHost(), request.getPort()));

            toOut = to.getOutputStream();
            toOut.write(request.toString().getBytes());
            toOut.flush();
        } catch (IOException e) {
            System.out.println(request.toString());
            System.out.println(e.getMessage());
            this.clientSemaphore.release(2);
            return;
        }

        try {
            ThreadPoolManager.getInstance().execute(new InputStreamConsumer(to.getInputStream(),
                    this.client.getOutputStream(), Long.valueOf(properties.getProperty(new String("HTTP_BYTES_WAIT_TIMEOUT"),
                    "2000")), this.clientSemaphore));
            ThreadPoolManager.getInstance().execute(new SocketCloseHandler(to, this.clientSemaphore));
        } catch (IOException e) {
            System.out.println(request.toString());
            System.out.println(e.getMessage());
            this.clientSemaphore.release(2);
        }
    }

}
