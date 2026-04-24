package org.polaris2023.gtu.space.portal;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Synchronizes transition states across all clients in multiplayer.
 * <p>
 * The SyncManager handles network synchronization of dimension transitions,
 * ensuring all clients see consistent player positions and transition states.
 * It broadcasts transition events and handles position interpolation for
 * smooth visual transitions.
 * </p>
 *
 * @see TransitionState
 * @see ITransitionManager
 */
public interface ISyncManager {

    /**
     * Broadcasts a transition start event to all relevant clients.
     *
     * @param transition The transition state
     */
    void broadcastTransitionStart(TransitionState transition);

    /**
     * Broadcasts a transition progress update.
     *
     * @param transition The updated transition state
     */
    void broadcastTransitionProgress(TransitionState transition);

    /**
     * Broadcasts a transition completion event.
     *
     * @param transition The completed transition
     */
    void broadcastTransitionComplete(TransitionState transition);

    /**
     * Broadcasts a transition cancellation.
     *
     * @param playerId The player whose transition was cancelled
     * @param reason   The cancellation reason
     */
    void broadcastTransitionCancellation(UUID playerId, String reason);

    /**
     * Interpolates a player's visual position during transition.
     *
     * @param playerId    The player to interpolate
     * @param partialTick The partial tick time
     * @return The interpolated position
     */
    Vec3 interpolatePlayerPosition(UUID playerId, float partialTick);
}
