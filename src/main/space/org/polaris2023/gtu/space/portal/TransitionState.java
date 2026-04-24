package org.polaris2023.gtu.space.portal;

import java.util.UUID;

/**
 * Represents the state of an in-progress dimension transition.
 * <p>
 * Tracks the transition lifecycle from initiation through completion or cancellation,
 * including the associated player, portal, and timing information.
 * </p>
 *
 * @see ITransitionManager
 * @see TransitionPhase
 */
public final class TransitionState {

    private final UUID transitionId;
    private final UUID playerId;
    private final UUID portalId;
    private final TransitionPhase phase;
    private final long startTick;
    private final long completeTick;
    private final double progress;

    public TransitionState(
            UUID transitionId,
            UUID playerId,
            UUID portalId,
            TransitionPhase phase,
            long startTick,
            long completeTick,
            double progress
    ) {
        this.transitionId = transitionId;
        this.playerId = playerId;
        this.portalId = portalId;
        this.phase = phase;
        this.startTick = startTick;
        this.completeTick = completeTick;
        this.progress = progress;
    }

    public UUID transitionId() {
        return transitionId;
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID portalId() {
        return portalId;
    }

    public TransitionPhase phase() {
        return phase;
    }

    public long startTick() {
        return startTick;
    }

    public long completeTick() {
        return completeTick;
    }

    public double progress() {
        return progress;
    }

    /**
     * Creates a new transition state with an updated phase.
     *
     * @param newPhase The new phase
     * @return A new TransitionState with the updated phase
     */
    public TransitionState withPhase(TransitionPhase newPhase) {
        return new TransitionState(
                transitionId, playerId, portalId, newPhase,
                startTick, completeTick, progress
        );
    }

    /**
     * Creates a new transition state with updated progress.
     *
     * @param newProgress The new progress value (0.0 to 1.0)
     * @return A new TransitionState with the updated progress
     */
    public TransitionState withProgress(double newProgress) {
        return new TransitionState(
                transitionId, playerId, portalId, phase,
                startTick, completeTick, Math.clamp(newProgress, 0.0, 1.0)
        );
    }

    /**
     * Checks if the transition is complete.
     *
     * @return true if the phase is COMPLETED
     */
    public boolean isComplete() {
        return phase == TransitionPhase.COMPLETED;
    }

    /**
     * Checks if the transition was cancelled.
     *
     * @return true if the phase is CANCELLED
     */
    public boolean isCancelled() {
        return phase == TransitionPhase.CANCELLED;
    }
}
