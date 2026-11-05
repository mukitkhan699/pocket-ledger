package com.pocketledger.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {

    private static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final ExecutorService SINGLE = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static ExecutorService io() {
        return IO;
    }

    public static ExecutorService single() {
        return SINGLE;
    }

    public static void main(Runnable runnable) {
        MAIN.post(runnable);
    }

    public static void runOffMain(Runnable runnable) {
        IO.execute(runnable);
    }
}
