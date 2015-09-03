package com.metova.rxjavaexample;

import com.metova.rxjavaexample.event.RxBus;
import com.metova.rxjavaexample.event.StartWorkEvent;
import com.metova.rxjavaexample.event.StopWorkEvent;
import rx.Subscription;

import java.io.IOException;

public class Main {

    private static WorkerThread sWorkerThread = new WorkerThread();

    public static void main(String[] args) throws IOException {
        sWorkerThread.start();
        System.out.println("Press the enter key to start and stop work!");

        for (; ; ) {
            System.in.read();
            if (!sWorkerThread.isRunning()) {
                RxBus.getInstance().post(new StartWorkEvent());
            } else {
                RxBus.getInstance().post(new StopWorkEvent());
            }
        }
    }

    private static class WorkerThread extends Thread {

        private Subscription mStartWorkSubscription;
        private Subscription mStopWorkSubscription;
        private boolean mRunning;

        @Override
        public void run() {
            mStartWorkSubscription = RxBus.getInstance().register(StartWorkEvent.class, event -> {
                mRunning = true;
                synchronized (this) {
                    notify();
                }
            });
            mStopWorkSubscription = RxBus.getInstance().register(StopWorkEvent.class, event -> {
                mRunning = false;
            });

            for (; ; ) {
                if (Thread.interrupted()) {
                    cleanUp();
                    return;
                }

                if (!mRunning) {
                    System.out.println("Stopping work");

                    try {
                        synchronized (this) {
                            wait();
                        }
                    } catch (InterruptedException e) {
                        cleanUp();
                        return;
                    }

                    System.out.println("Starting work");
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    cleanUp();
                    return;
                }

                doWork();
            }
        }

        private void doWork() {
            System.out.print(".");
        }

        private void cleanUp() {
            mStartWorkSubscription.unsubscribe();
            mStopWorkSubscription.unsubscribe();
        }

        public boolean isRunning() {
            return mRunning;
        }
    }
}
