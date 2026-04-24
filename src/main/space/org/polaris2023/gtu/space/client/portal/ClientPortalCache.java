package org.polaris2023.gtu.space.client.portal;

import org.polaris2023.gtu.space.portal.PortalType;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPortalCache {
    private static final ConcurrentHashMap<UUID, PortalSyncData> PORTALS = new ConcurrentHashMap<>();

    private ClientPortalCache() {
    }

    public static void updatePortal(PortalSyncData data) {
        if (data.active()) {
            PORTALS.put(data.portalId(), data);
        } else {
            PORTALS.remove(data.portalId());
        }
    }

    public static PortalSyncData getPortal(UUID portalId) {
        return PORTALS.get(portalId);
    }

    public static Collection<PortalSyncData> allPortals() {
        return Collections.unmodifiableCollection(PORTALS.values());
    }

    public static PortalSyncData findNearestPortal(double x, double y, double z, String dimension, double maxDistance) {
        PortalSyncData nearest = null;
        double nearestDistSq = maxDistance * maxDistance;
        for (PortalSyncData portal : PORTALS.values()) {
            if (!portal.sourceDimension().equals(dimension)) {
                continue;
            }
            double cx = (portal.minX() + portal.maxX()) * 0.5;
            double cy = (portal.minY() + portal.maxY()) * 0.5;
            double cz = (portal.minZ() + portal.maxZ()) * 0.5;
            double distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = portal;
            }
        }
        return nearest;
    }

    public static void clear() {
        PORTALS.clear();
    }
}
