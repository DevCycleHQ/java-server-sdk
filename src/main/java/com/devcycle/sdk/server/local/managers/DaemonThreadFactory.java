package com.devcycle.sdk.server.local.managers;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executors;

public class DaemonThreadFactory implements ThreadFactory {
    private ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

    public Thread newThread(Runnable r) {
        Thread thread = defaultThreadFactory.newThread(r);
        thread.setDaemon(true);
        return thread;
    }
}
