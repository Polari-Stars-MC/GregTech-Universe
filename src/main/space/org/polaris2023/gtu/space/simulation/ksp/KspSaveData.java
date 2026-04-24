package org.polaris2023.gtu.space.simulation.ksp;

import java.util.LinkedHashMap;
import java.util.Map;

public final class KspSaveData {
    public long simulationTick;
    public double simulationSeconds;
    public long spaceServerTick;
    public Map<String, BodyStateData> bodyStates = new LinkedHashMap<>();
    public Map<String, VesselStateData> vesselStates = new LinkedHashMap<>();
    public Map<String, double[]> bodyThrust = new LinkedHashMap<>();
    public Map<String, PlayerStateData> playerStates = new LinkedHashMap<>();
    public Map<String, SpaceVesselData> spaceVessels = new LinkedHashMap<>();
    public Map<String, SpaceTransitionData> spaceTransitions = new LinkedHashMap<>();

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
        public String bodyId;
        public String planetDimension;
        public String stablePlanetDimension;
        public double stableX;
        public double stableY;
        public double stableZ;
        public float stableYRot;
        public float stableXRot;
        public String sdSlotDimension;
        public String universeId;
        public String galaxyId;
        public String systemId;
        public String[] universeAnchorPosition;
        public double[] coordinate;
        public String vesselId;
        public String transitionId;
    }

    public static final class SpaceVesselData {
        public String vesselId;
        public String authorityVesselId;
        public String bodyId;
        public String universeId;
        public String galaxyId;
        public String systemId;
        public String[] universeAnchorPosition;
        public String sdSlotDimension;
        public double[] shellCoordinate;
        public double shellX;
        public double shellY;
        public double shellZ;
        public double altitudeMeters;
        public String landingBodyId;
        public String[] crew;
    }

    public static final class SpaceTransitionData {
        public String transitionId;
        public String direction;
        public String sourceDomain;
        public String targetDomain;
        public String bodyId;
        public String sourceDimension;
        public String targetDimension;
        public long startTick;
        public long cutoverTick;
        public long completeTick;
        public double startAltitudeMeters;
        public double cutoverAltitudeMeters;
        public double completeAltitudeMeters;
        public boolean cutoverApplied;
    }
}

