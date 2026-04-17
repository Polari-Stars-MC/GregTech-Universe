package org.polaris2023.gtu.space.runtime.ksp;

import java.util.LinkedHashMap;
import java.util.Map;

public final class KspSaveData {
    public long simulationTick;
    public double simulationSeconds;
    public Map<String, BodyStateData> bodyStates = new LinkedHashMap<>();
    public Map<String, VesselStateData> vesselStates = new LinkedHashMap<>();
    public Map<String, double[]> bodyThrust = new LinkedHashMap<>();
    public Map<String, PlayerStateData> playerStates = new LinkedHashMap<>();

    public static final class BodyStateData {
        public double[] position;
        public double[] velocity;
        public String referenceBodyId;
    }

    public static final class VesselStateData {
        public String id;
        public String name;
        public String universeId;
        public String galaxyId;
        public String systemId;
        public String[] universeAnchorPosition;
        public String primaryBodyId;
        public double dryMass;
        public double[] absolutePosition;
        public double[] absoluteVelocity;
    }

    public static final class PlayerStateData {
        public String mode;
        public String anchorDimension;
        public String universeId;
        public String galaxyId;
        public String systemId;
        public String[] universeAnchorPosition;
        public double[] coordinate;
        public String vesselId;
    }
}
