package org.polaris2023.gtu.core.api.multiblock.runtime.execution;

import java.util.concurrent.atomic.AtomicBoolean;

public class StructureRuntimeThread implements Runnable {
    private final StructureRuntime runtime;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final long idleSleepMillis;
    private Thread worker;

    public StructureRuntimeThread(StructureRuntime runtime, long idleSleepMillis) {
        this.runtime = runtime;
        this.idleSleepMillis = idleSleepMillis;
    }

    public synchronized void start(String threadName) {
        if (running.get()) {
            return;
        }
        shutdown.set(false);
        running.set(true);
        worker = new Thread(this, threadName);
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        shutdown.set(true);
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    public boolean isRunning() {
        return running.get() && !shutdown.get();
    }

    @Override
    public void run() {
        while (!shutdown.get()) {
            boolean processed = runtime.processInputBatch();
            if (!processed) {
                try {
                    Thread.sleep(idleSleepMillis);
                } catch (InterruptedException ignored) {
                    if (shutdown.get()) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}
