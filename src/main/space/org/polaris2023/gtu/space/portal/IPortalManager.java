package org.polaris2023.gtu.space.portal;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Manages the lifecycle and lookup of all portal instances.
 * <p>
 * The PortalManager is responsible for registering, unregistering, and querying
 * portals across all dimensions. It maintains a registry of active portals and
 * provides efficient lookup by position and dimension.
 * </p>
 *
 * @see Portal
 * @see ITransitionManager
 */
public interface IPortalManager {

    /**
     * Finds a portal at the given position in the specified dimension.
     *
     * @param position  The world position to check
     * @param dimension The dimension key
     * @return The portal if one exists at the position, null otherwise
     */
    Portal getPortalAt(Vec3 position, ResourceKey<Level> dimension);

    /**
     * Registers a new portal with the manager.
     *
     * @param portal The portal to register
     * @throws IllegalArgumentException if a portal with the same ID already exists
     */
    void registerPortal(Portal portal);

    /**
     * Removes a portal from the registry.
     *
     * @param portalId The UUID of the portal to remove
     */
    void unregisterPortal(UUID portalId);

    /**
     * Returns all active portals in the specified dimension.
     *
     * @param dimension The dimension key
     * @return Unmodifiable list of portals in the dimension
     */
    List<Portal> getPortalsInDimension(ResourceKey<Level> dimension);

    /**
     * Returns all registered portals.
     *
     * @return Unmodifiable collection of all portals
     */
    Collection<Portal> getAllPortals();
}
