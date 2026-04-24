package org.polaris2023.gtu.space.portal;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Represents a single portal instance with all its properties.
 * <p>
 * A portal defines a bidirectional or unidirectional link between two dimensions,
 * with an associated cube face index for coordinate mapping. The portal area
 * defines the 3D region where the portal is active.
 * </p>
 *
 * @see IPortalManager
 * @see ITransitionManager
 */
public final class Portal {

    private final UUID id;
    private final String sourceDimension;
    private final String targetDimension;
    private final int faceIndex;
    private final AABB area;
    private final boolean bidirectional;
    private final PortalOrientation orientation;
    private final PortalType type;

    public Portal(
            UUID id,
            String sourceDimension,
            String targetDimension,
            int faceIndex,
            AABB area,
            boolean bidirectional,
            PortalOrientation orientation,
            PortalType type
    ) {
        this.id = id;
        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;
        this.faceIndex = faceIndex;
        this.area = area;
        this.bidirectional = bidirectional;
        this.orientation = orientation;
        this.type = type;
    }

    public UUID id() {
        return id;
    }

    public String sourceDimension() {
        return sourceDimension;
    }

    public String targetDimension() {
        return targetDimension;
    }

    public int faceIndex() {
        return faceIndex;
    }

    public AABB area() {
        return area;
    }

    public boolean bidirectional() {
        return bidirectional;
    }

    public PortalOrientation orientation() {
        return orientation;
    }

    public PortalType type() {
        return type;
    }

    /**
     * Checks if a position is within the portal area.
     *
     * @param position The position to check
     * @return true if the position is inside the portal area
     */
    public boolean contains(Vec3 position) {
        return area.contains(position);
    }

    /**
     * Returns the normal vector of the portal surface.
     *
     * @return The portal normal vector
     */
    public Vector3f getNormal() {
        return orientation.normal();
    }

    /**
     * Returns the width of the portal in blocks.
     *
     * @return The portal width
     */
    public double getWidth() {
        return area.getXsize();
    }

    /**
     * Returns the height of the portal in blocks.
     *
     * @return The portal height
     */
    public double getHeight() {
        return area.getYsize();
    }
}
