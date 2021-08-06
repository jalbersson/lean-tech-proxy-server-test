package com.leantech.server.io;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class SocketCloseHandler implements Runnable {

    private Socket socket;
    private List<Semaphore> semaphores;

    public SocketCloseHandler(Socket socket) {
        super();
        this.socket = socket;
    }

    public SocketCloseHandler(Socket socket, Semaphore semaphore) {
        super();
        this.socket = socket;
        this.addSemaphore(semaphore);
    }

    public boolean addSemaphore(Semaphore semaphore) {
        if (this.semaphores == null) {
            this.semaphores = new ArrayList<Semaphore>();
        }

        return this.semaphores.add(semaphore);
    }

    public void run() {
        for (Semaphore semaphore : this.semaphores) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        try {
            this.socket.getInputStream().close();
        } catch(Exception e) {}

        try {
            this.socket.getOutputStream().close();
        } catch(Exception e) {}

        try {
            this.socket.close();
        } catch(Exception e) {}
    }

}
