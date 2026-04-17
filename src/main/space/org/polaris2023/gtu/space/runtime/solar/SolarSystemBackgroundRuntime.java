package org.polaris2023.gtu.space.runtime.solar;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public final class SolarSystemBackgroundRuntime implements AutoCloseable {
    private static final long STEP_NANOS = 16_666_667L;
    private static final float TICKS_PER_NANO = 20.0F / 1_000_000_000.0F;

    private final SolarSystemRuntime runtime;
    private final AtomicReference<SolarSystemFrame> latestFrame;
    private final AtomicLong syncedAbsoluteTick = new AtomicLong();
    private final AtomicLong syncedNanoTime = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean();

    private volatile Thread workerThread;

    public SolarSystemBackgroundRuntime(SolarSystemRuntime runtime) {
        this.runtime = runtime;
        this.latestFrame = new AtomicReference<>(runtime.sample(0L, 0.0F));
    }

    public void ensureStarted() {
        if (running.compareAndSet(false, true)) {
            syncedNanoTime.set(System.nanoTime());
            Thread thread = new Thread(this::runLoop, "gtu-space-solar-runtime");
            thread.setDaemon(true);
            thread.start();
            workerThread = thread;
        }
    }

    public void syncTime(long absoluteTick) {
        syncedAbsoluteTick.set(absoluteTick);
        syncedNanoTime.set(System.nanoTime());
    }

    public SolarSystemFrame latestFrame() {
        return latestFrame.get();
    }

    public SolarSystemDefinition definition() {
        return runtime.definition();
    }

    private void runLoop() {
        while (running.get()) {
            long baseTick = syncedAbsoluteTick.get();
            long baseNano = syncedNanoTime.get();
            long now = System.nanoTime();
            long elapsed = Math.max(0L, now - baseNano);

            float tickProgress = elapsed * TICKS_PER_NANO;
            long wholeTicks = (long) tickProgress;
            float partialTick = tickProgress - wholeTicks;

            latestFrame.set(runtime.sample(baseTick + wholeTicks, partialTick));
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
    }
}
