package org.polaris2023.gtu.space.portal;

/**
 * Defines the type of portal based on the dimensions it connects.
 *
 * @see Portal
 */
public enum PortalType {

    /**
     * Portal from a space dimension (SD) to a planet surface dimension (PD).
     */
    SPACE_TO_SURFACE,

    /**
     * Portal from a planet surface dimension (PD) to a space dimension (SD).
     */
    SURFACE_TO_SPACE,

    /**
     * Portal between two arbitrary dimensions.
     */
    INTER_DIMENSIONAL
}
