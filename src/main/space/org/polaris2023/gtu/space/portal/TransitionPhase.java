package org.polaris2023.gtu.space.portal;

/**
 * Defines the phases of a dimension transition lifecycle.
 *
 * @see TransitionState
 */
public enum TransitionPhase {

    /**
     * Transition has been initiated but not yet started.
     */
    INITIATED,

    /**
     * Transition is preparing resources and validating.
     */
    PREPARING,

    /**
     * Player is being transferred between dimensions.
     */
    TRANSFERRING,

    /**
     * Transition is finalizing and cleaning up.
     */
    COMPLETING,

    /**
     * Transition has completed successfully.
     */
    COMPLETED,

    /**
     * Transition was cancelled before completion.
     */
    CANCELLED
}
