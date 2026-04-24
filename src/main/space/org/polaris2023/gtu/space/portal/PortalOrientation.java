package org.polaris2023.gtu.space.portal;

import org.joml.Vector3f;

/**
 * Represents the orientation of a portal in 3D space.
 * <p>
 * The orientation is defined by a normal vector (perpendicular to the portal surface),
 * an up vector (defining the portal's "up" direction), and an optional rotation angle.
 * </p>
 *
 * @param normal         The normal vector perpendicular to the portal surface
 * @param up             The up vector defining the portal's vertical orientation
 * @param rotationDegrees The rotation angle in degrees around the normal axis
 */
public record PortalOrientation(
        Vector3f normal,
        Vector3f up,
        float rotationDegrees
) {

    /**
     * Default orientation with normal pointing in the positive Y direction.
     */
    public static final PortalOrientation DEFAULT = new PortalOrientation(
            new Vector3f(0, 1, 0),
            new Vector3f(0, 0, -1),
            0.0f
    );
}
