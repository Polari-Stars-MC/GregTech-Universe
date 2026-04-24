package org.polaris2023.gtu.space.portal.impl;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.space.portal.IPortalManager;
import org.polaris2023.gtu.space.portal.Portal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpacePortalManager implements IPortalManager {
    private final ConcurrentHashMap<UUID, Portal> portals = new ConcurrentHashMap<>();

    @Override
    public Portal getPortalAt(Vec3 position, ResourceKey<Level> dimension) {
        String dimId = dimension.location().toString();
        for (Portal portal : portals.values()) {
            if (portal.sourceDimension().equals(dimId) && portal.contains(position)) {
                return portal;
            }
        }
        return null;
    }

    @Override
    public void registerPortal(Portal portal) {
        if (portals.containsKey(portal.id())) {
            throw new IllegalArgumentException("Portal already registered: " + portal.id());
        }
        portals.put(portal.id(), portal);
    }

    @Override
    public void unregisterPortal(UUID portalId) {
        portals.remove(portalId);
    }

    @Override
    public List<Portal> getPortalsInDimension(ResourceKey<Level> dimension) {
        String dimId = dimension.location().toString();
        List<Portal> result = new ArrayList<>();
        for (Portal portal : portals.values()) {
            if (portal.sourceDimension().equals(dimId)) {
                result.add(portal);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<Portal> getAllPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public void tick() {
        // Portals are ephemeral — created/destroyed by SpaceTransitionManager
    }

    public void clear() {
        portals.clear();
    }
}
