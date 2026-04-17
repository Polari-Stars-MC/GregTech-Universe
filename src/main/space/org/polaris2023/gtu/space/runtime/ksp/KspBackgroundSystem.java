package org.polaris2023.gtu.space.runtime.ksp;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public final class KspBackgroundSystem implements AutoCloseable {
    private static final long STEP_NANOS = 50_000_000L;
    private static final double STEP_SECONDS = 0.05D;

    private final KspSystemRuntime runtime;
    private final AtomicReference<KspSnapshot> latestSnapshot;
    private final AtomicBoolean running = new AtomicBoolean();

    private volatile Thread workerThread;

    public KspBackgroundSystem(KspSystemDefinition definition) {
        this.runtime = new KspSystemRuntime(definition);
        this.latestSnapshot = new AtomicReference<>(runtime.snapshot());
    }

    public void ensureStarted() {
        if (running.compareAndSet(false, true)) {
            Thread thread = new Thread(this::runLoop, "gtu-space-ksp-runtime");
            thread.setDaemon(true);
            thread.start();
            workerThread = thread;
        }
    }

    public KspSnapshot latestSnapshot() {
        return latestSnapshot.get();
    }

    private void runLoop() {
        while (running.get()) {
            runtime.step(STEP_SECONDS);
            latestSnapshot.set(runtime.snapshot());
            LockSupport.parkNanos(STEP_NANOS);
        }
    }

    @Override
    public void close() {
        running.set(false);
        Thread thread = workerThread;
        if (thread != null) {
            LockSupport.unpark(thread);
        }
        runtime.close();
    }
}
