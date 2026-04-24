package org.polaris2023.gtu.space.portal;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Handles seamless dimension transitions triggered by portal entry.
 * <p>
 * The TransitionManager manages the complete lifecycle of dimension transitions,
 * from initiation through completion or cancellation. It ensures atomic transitions
 * where either the transition completes fully or fails fully, preserving player state.
 * </p>
 *
 * @see TransitionState
 * @see Portal
 * @see ISyncManager
 */
public interface ITransitionManager {

    /**
     * Begins a dimension transition for the player.
     *
     * @param player The player to transition
     * @param portal The portal triggering the transition
     * @return The transition state
     */
    TransitionState beginTransition(ServerPlayer player, Portal portal);

    /**
     * Completes an in-progress transition.
     *
     * @param player The player to complete transition for
     */
    void completeTransition(ServerPlayer player);

    /**
     * Cancels an in-progress transition.
     *
     * @param player The player to cancel transition for
     * @param reason The reason for cancellation
     */
    void cancelTransition(ServerPlayer player, String reason);

    /**
     * Gets the current transition state for a player.
     *
     * @param playerId The player's UUID
     * @return The transition state, or null if not transitioning
     */
    TransitionState getTransitionState(UUID playerId);

    /**
     * Checks if a transition is valid.
     *
     * @param player The player to check
     * @param portal The target portal
     * @return ValidationResult indicating success or failure reason
     */
    ValidationResult validateTransition(ServerPlayer player, Portal portal);
}
