package org.polaris2023.gtu.space.simulation;

import org.polaris2023.gtu.space.simulation.math.SpaceCoordinate;
import org.polaris2023.gtu.space.simulation.math.UniverseVector;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class SpaceVesselState {
    private final UUID vesselId;
    private final Set<UUID> crew = new LinkedHashSet<>();
    private String authorityVesselId;
    private String bodyId;
    private String universeId;
    private String galaxyId;
    private String systemId;
    private UniverseVector universeAnchorPosition;
    private String sdSlotDimension;
    private SpaceCoordinate shellCoordinate;
    private double shellX;
    private double shellY;
    private double shellZ;
    private double altitudeMeters;
    private String landingBodyId;
    private boolean landingDescentActive;

    public SpaceVesselState(
            UUID vesselId,
            String authorityVesselId,
            String bodyId,
            String universeId,
            String galaxyId,
            String systemId,
            UniverseVector universeAnchorPosition,
            String sdSlotDimension,
            SpaceCoordinate shellCoordinate,
            double shellX,
            double shellY,
            double shellZ,
            double altitudeMeters,
            String landingBodyId
    ) {
        this.vesselId = vesselId;
        this.authorityVesselId = authorityVesselId;
        this.bodyId = bodyId;
        this.universeId = universeId;
        this.galaxyId = galaxyId;
        this.systemId = systemId;
        this.universeAnchorPosition = universeAnchorPosition;
        this.sdSlotDimension = sdSlotDimension;
        this.shellCoordinate = shellCoordinate;
        this.shellX = shellX;
        this.shellY = shellY;
        this.shellZ = shellZ;
        this.altitudeMeters = altitudeMeters;
        this.landingBodyId = landingBodyId;
        this.landingDescentActive = false;
    }

    public UUID vesselId() {
        return vesselId;
    }

    public Set<UUID> crew() {
        return Set.copyOf(crew);
    }

    public void addCrew(UUID playerId) {
        crew.add(playerId);
    }

    public void removeCrew(UUID playerId) {
        crew.remove(playerId);
    }

    public boolean isEmpty() {
        return crew.isEmpty();
    }

    public String authorityVesselId() {
        return authorityVesselId;
    }

    public void setAuthorityVesselId(String authorityVesselId) {
        this.authorityVesselId = authorityVesselId;
    }

    public String bodyId() {
        return bodyId;
    }

    public void setBodyId(String bodyId) {
        this.bodyId = bodyId;
    }

    public String universeId() {
        return universeId;
    }

    public String galaxyId() {
        return galaxyId;
    }

    public String systemId() {
        return systemId;
    }

    public UniverseVector universeAnchorPosition() {
        return universeAnchorPosition;
    }

    public void setUniverseAnchorPosition(String universeId, String galaxyId, String systemId, UniverseVector universeAnchorPosition) {
        this.universeId = universeId;
        this.galaxyId = galaxyId;
        this.systemId = systemId;
        this.universeAnchorPosition = universeAnchorPosition;
    }

    public String sdSlotDimension() {
        return sdSlotDimension;
    }

    public void setSdSlotDimension(String sdSlotDimension) {
        this.sdSlotDimension = sdSlotDimension;
    }

    public SpaceCoordinate shellCoordinate() {
        return shellCoordinate;
    }

    public void setShellCoordinate(SpaceCoordinate shellCoordinate) {
        this.shellCoordinate = shellCoordinate;
    }

    public double shellX() {
        return shellX;
    }

    public double shellY() {
        return shellY;
    }

    public double shellZ() {
        return shellZ;
    }

    public void setShellPosition(double shellX, double shellY, double shellZ) {
        this.shellX = shellX;
        this.shellY = shellY;
        this.shellZ = shellZ;
    }

    public double altitudeMeters() {
        return altitudeMeters;
    }

    public void setAltitudeMeters(double altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    public String landingBodyId() {
        return landingBodyId;
    }

    public void setLandingBodyId(String landingBodyId) {
        this.landingBodyId = landingBodyId;
    }

    public boolean landingDescentActive() {
        return landingDescentActive;
    }

    public void setLandingDescentActive(boolean landingDescentActive) {
        this.landingDescentActive = landingDescentActive;
    }
}
