package org.polaris2023.gtu.space.simulation.ksp;

import org.polaris2023.gtu.space.simulation.math.SpaceVector;
import org.polaris2023.gtu.space.simulation.math.UniverseVector;

public record KspVesselState(
        String id,
        String name,
        String universeId,
        String galaxyId,
        String systemId,
        UniverseVector universeAnchorPosition,
        String primaryBodyId,
        double dryMass,
        SpaceVector absolutePosition,
        SpaceVector absoluteVelocity
) {
    public KspVesselState withPrimaryBodyId(String nextPrimaryBodyId) {
        return new KspVesselState(id, name, universeId, galaxyId, systemId, universeAnchorPosition, nextPrimaryBodyId, dryMass, absolutePosition, absoluteVelocity);
    }

    public KspVesselState withState(String nextPrimaryBodyId, SpaceVector nextPosition, SpaceVector nextVelocity) {
        return new KspVesselState(id, name, universeId, galaxyId, systemId, universeAnchorPosition, nextPrimaryBodyId, dryMass, nextPosition, nextVelocity);
    }

    public KspVesselState withUniverseAnchor(String nextUniverseId, String nextGalaxyId, String nextSystemId, UniverseVector nextUniverseAnchorPosition) {
        return new KspVesselState(id, name, nextUniverseId, nextGalaxyId, nextSystemId, nextUniverseAnchorPosition, primaryBodyId, dryMass, absolutePosition, absoluteVelocity);
    }
}

