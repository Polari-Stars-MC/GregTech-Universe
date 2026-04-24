package org.polaris2023.gtu.space.simulation;

import com.jme3.bullet.PhysicsSpace;

public final class SpacePhysicsEngine implements AutoCloseable {
    private final PhysicsSpace physicsSpace;

    public SpacePhysicsEngine() {
        this.physicsSpace = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
        this.physicsSpace.setAccuracy(1.0f / 20.0f);
    }

    public void stepSimulation(float deltaSeconds) {
        physicsSpace.update(deltaSeconds);
    }

    public PhysicsSpace physicsSpace() {
        return physicsSpace;
    }

    @Override
    public void close() {
        physicsSpace.destroy();
    }
}

