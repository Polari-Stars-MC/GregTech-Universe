package org.polaris2023.gtu.space.client.portal;

import org.polaris2023.gtu.space.portal.PortalType;

import java.util.UUID;

public record PortalSyncData(
        UUID portalId,
        String sourceDimension,
        String targetDimension,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        PortalType type,
        int faceIndex,
        boolean active
) {
}
