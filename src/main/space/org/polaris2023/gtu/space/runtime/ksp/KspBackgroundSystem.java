package org.polaris2023.gtu.space.runtime.ksp;

import org.polaris2023.gtu.space.runtime.math.SpaceVector;

import java.util.concurrent.atomic.AtomicReference;

public final class KspBackgroundSystem implements AutoCloseable {
    /**
     * 1 MC day = 24000 ticks = 86400 simulation seconds.
     * Each MC tick advances the simulation by 3.6 seconds.
     */
    private static final double SECONDS_PER_TICK = 86400.0 / 24000.0;

    private final KspSystemRuntime runtime;
    private final AtomicReference<KspSnapshot> latestSnapshot;

    public KspBackgroundSystem(KspSystemDefinition definition) {
        this.runtime = new KspSystemRuntime(definition);
        this.latestSnapshot = new AtomicReference<>(runtime.snapshot());
    }

    public void tick() {
        runtime.step(SECONDS_PER_TICK);
        latestSnapshot.set(runtime.snapshot());
    }

    public KspSnapshot latestSnapshot() {
        return latestSnapshot.get();
    }

    public void applyBodyThrust(String bodyId, SpaceVector thrustAcceleration) {
        runtime.applyBodyThrust(bodyId, thrustAcceleration);
    }

    public void clearBodyThrust(String bodyId) {
        runtime.clearBodyThrust(bodyId);
    }

    public KspSaveData exportState() {
        return runtime.exportState();
    }

    public void importState(KspSaveData data) {
        runtime.importState(data);
        latestSnapshot.set(runtime.snapshot());
    }

    @Override
    public void close() {
        runtime.close();
    }
}
