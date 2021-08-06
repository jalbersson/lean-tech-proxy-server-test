package com.leantech.server.io;

import com.leantech.server.utils.ContentType;
import com.leantech.server.utils.Timer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.Semaphore;

public class InputStreamConsumer implements Runnable {

    private InputStream input;
    private OutputStream output;
    private Semaphore semaphore;
    private Long readTimeOut;
    private Timer timerRead;

    public InputStreamConsumer(InputStream input, OutputStream output) {
        super();
        this.input = input;
        this.output = output;
    }

    public InputStreamConsumer(InputStream input, OutputStream output, Long readTimeOut) {
        this(input, output);
        this.readTimeOut = readTimeOut;
        this.timerRead = new Timer();
    }

    public InputStreamConsumer(InputStream input, OutputStream output, Long readTimeOut, Semaphore semaphore) {
        this(input, output, readTimeOut);
        this.semaphore = semaphore;
    }

    public void consume() {
        try {
            boolean availableBytes = false;
            int bytesReaded = 0;
            Properties properties = new Properties();
            byte[] bytesBuffer = new byte[Integer.valueOf(properties.getProperty(new String("PROXY_READ_BUFFER_SIZE"), new String("1024")))];

            do {
                try {
                    bytesReaded = this.input.read(bytesBuffer);
                } catch(IOException e) {}

                try {
                    this.output.write(bytesBuffer, 0, bytesReaded);
                    this.output.flush();
                } catch (Exception e) {}

                try {
                    String response = new String(bytesBuffer, 0, bytesReaded);
                    String key = new String("\nContent-Type:");
                    String newLine = new String("\n");
                    String dotCome = new String(";");

                    int indexOfContentType = response.toLowerCase().indexOf(key.toLowerCase());

                    if (indexOfContentType != -1) {
                        String contentType = response.substring(indexOfContentType + key.length());
                        int indexOfFinal = contentType.indexOf(newLine);
                        contentType = contentType.substring(0, indexOfFinal).trim();
                        int indexOfDotCome = contentType.indexOf(dotCome);

                        if (indexOfDotCome != -1) {
                            contentType = contentType.substring(0, indexOfDotCome).trim();
                        }

                        System.out.println(new String("Content-Type: ") + contentType);

                        // if content-type is application/octet-stream change read timeout
                        if (contentType.toLowerCase().contains(ContentType.APPLICATION_OCTETSTREAM.toString()) ||
                                contentType.toLowerCase().contains(ContentType.APPLICATION_ZIP.toString())) {
                            System.out.println(new String("application download detected."));
                            System.out.println(new String("nulling download timeout."));
                            this.readTimeOut = null;
                        }
                    }
                } catch(Exception e) {}

                if (this.readTimeOut != null) {
                    if (this.timerRead == null)
                        this.timerRead = new Timer();
                    this.timerRead.schedule(this.readTimeOut);

                    try {
                        while (this.input.available() < 1) {
                            if (this.timerRead.isTimeOut())
                                break;
                        }
                    } catch(IOException e) {}
                } else {
                    try {
                        while (this.input.available() < 1) {
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {};
                        }
                    } catch(IOException e) {}
                }

                try {
                    availableBytes = this.input.available() > 0;
                } catch (IOException e) {}
            } while (availableBytes);
        } finally {
            if (this.semaphore != null) {
                this.semaphore.release(2);
            }
        }
    }

    public void run() {
        this.consume();
    }
}
