package com.leantech.server.utils;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Timer {
    private Long millis;
    private TimeOutListener timeOutListener;
    private Runnable timerTask;
    private boolean timeOut;
    private boolean timerCanceled;

    public Timer() {
        super();
        this.timeOut = false;
    }

    public Timer(Long millis) {
        this();
        this.millis = millis;
        this.timerCanceled = false;
    }

    public Timer(Long millis, TimeOutListener timeOutListener) {
        this(millis);
        this.addTimeOutListener(timeOutListener);
    }

    public void addTimeOutListener(TimeOutListener timeOutListener) {
        if (timeOutListener == null) {
            throw new NullPointerException();
        }

        this.timeOutListener = timeOutListener;
    }

    public void schedule() {
        if (this.millis == null) {
            throw new NullPointerException();
        }

        this.cancel();
        this.timeOut = false;
        this.timerTask = new TimerTask(this.millis, this);
        ExecutorService executorService = Executors.newFixedThreadPool(Integer.valueOf(new Properties().getProperty(new String("MAX_THREADS_ON_POOL"), new String("3000"))));
        executorService.execute(timerTask);
    }

    public void schedule(Long millis) {
        this.millis = millis;
        this.schedule();
    }

    private void timeOut() {
        if (this.isTimerCanceled()) {
            return;
        }

        this.timeOut = true;

        if (this.timeOutListener != null) {
            this.timeOutListener.timeOut();
        }
    }

    public boolean isTimerCanceled() {
        return timerCanceled;
    }

    public void cancel() {
        if (this.timerTask != null) {
            ((TimerTask) this.timerTask).cancel();
            this.timerTask = null;
        }
        //this.timerCanceled = true;
        //this.timeOut = true;
    }

    public boolean isTimeOut() {
        return timeOut;
    }

    private class TimerTask implements Runnable {
        private Long millis;
        private Timer timer;
        private boolean canceled;

        TimerTask(Long millis, Timer timer) {
            super();
            this.millis = millis;
            this.timer = timer;
            this.canceled = false;
        }

        public void cancel() {
            this.canceled = true;
        }

        public void run() {
            long now = new Date().getTime();

            while ((now + this.millis) > new Date().getTime()) {
                if (canceled) {
                    return;
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {}
            }

            this.timer.timeOut();
        }

    }

    public static void main(String[] args) {
        Timer timer = new Timer();
        timer.schedule(5000L);

        while (!timer.isTimeOut()) {
            try { Thread.sleep(1); } catch (Exception e) {}
        }

        System.out.println("-- time-out --");
    }
}
