package org.polaris2023.gtu.space.portal;

import org.joml.Vector3f;

/**
 * Defines the mapping between a cube planet face and its orientation in 3D space.
 * <p>
 * Each cube face has a normal vector (pointing outward from the cube center),
 * an up vector (defining the "up" direction on that face), and a right vector
 * (defining the "right" direction on that face). These vectors form an orthonormal
 * basis for coordinate transformations.
 * </p>
 *
 * <h2>Face Indices</h2>
 * <ul>
 *   <li>0 = Front  (Z-) - normal: (0, 0, -1)</li>
 *   <li>1 = Back   (Z+) - normal: (0, 0, 1)</li>
 *   <li>2 = Left   (X-) - normal: (-1, 0, 0)</li>
 *   <li>3 = Right  (X+) - normal: (1, 0, 0)</li>
 *   <li>4 = Top    (Y+) - normal: (0, 1, 0)</li>
 *   <li>5 = Bottom (Y-) - normal: (0, -1, 0)</li>
 * </ul>
 *
 * @param faceIndex The cube face index (0-5)
 * @param normal    The normal vector perpendicular to the face, pointing outward
 * @param up        The up vector defining the vertical direction on the face
 * @param right     The right vector defining the horizontal direction on the face
 *
 * @see ICoordinateMapper
 * @see FacePosition
 */
public record CubeFaceMapping(
        int faceIndex,
        Vector3f normal,
        Vector3f up,
        Vector3f right
) {

    /**
     * Default mappings for all 6 cube faces.
     * <p>
     * The array index corresponds to the face index. Each mapping defines
     * an orthonormal basis where:
     * </p>
     * <ul>
     *   <li>normal × up = right (right-handed coordinate system)</li>
     *   <li>All vectors are unit length</li>
     *   <li>Adjacent faces have orthogonal normals</li>
     *   <li>Opposite faces have anti-parallel normals</li>
     * </ul>
     */
    public static final CubeFaceMapping[] DEFAULT_MAPPINGS = {
            // Face 0: Front (Z-)
            new CubeFaceMapping(0, new Vector3f(0, 0, -1), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0)),
            // Face 1: Back (Z+)
            new CubeFaceMapping(1, new Vector3f(0, 0, 1), new Vector3f(0, 1, 0), new Vector3f(-1, 0, 0)),
            // Face 2: Left (X-)
            new CubeFaceMapping(2, new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)),
            // Face 3: Right (X+)
            new CubeFaceMapping(3, new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, -1)),
            // Face 4: Top (Y+)
            new CubeFaceMapping(4, new Vector3f(0, 1, 0), new Vector3f(0, 0, -1), new Vector3f(1, 0, 0)),
            // Face 5: Bottom (Y-)
            new CubeFaceMapping(5, new Vector3f(0, -1, 0), new Vector3f(0, 0, 1), new Vector3f(1, 0, 0))
    };

    /**
     * Gets the default mapping for a given face index.
     *
     * @param faceIndex The face index (0-5)
     * @return The default mapping for that face
     * @throws IllegalArgumentException if faceIndex is not in range [0, 5]
     */
    public static CubeFaceMapping getDefault(int faceIndex) {
        if (faceIndex < 0 || faceIndex > 5) {
            throw new IllegalArgumentException("Face index must be between 0 and 5, got: " + faceIndex);
        }
        return DEFAULT_MAPPINGS[faceIndex];
    }

    /**
     * Gets the face name for this mapping.
     *
     * @return The face name (front, back, left, right, top, or bottom)
     */
    public String faceName() {
        return switch (faceIndex) {
            case 0 -> "front";
            case 1 -> "back";
            case 2 -> "left";
            case 3 -> "right";
            case 4 -> "top";
            case 5 -> "bottom";
            default -> "unknown";
        };
    }

    /**
     * Checks if this face is a side face (front, back, left, or right).
     *
     * @return true if this is a side face (indices 0-3)
     */
    public boolean isSideFace() {
        return faceIndex >= 0 && faceIndex <= 3;
    }

    /**
     * Checks if this face is a horizontal face (top or bottom).
     *
     * @return true if this is a horizontal face (indices 4-5)
     */
    public boolean isHorizontalFace() {
        return faceIndex == 4 || faceIndex == 5;
    }

    /**
     * Gets the opposite face index.
     *
     * @return The face index of the opposite face
     */
    public int oppositeFaceIndex() {
        return switch (faceIndex) {
            case 0 -> 1; // front -> back
            case 1 -> 0; // back -> front
            case 2 -> 3; // left -> right
            case 3 -> 2; // right -> left
            case 4 -> 5; // top -> bottom
            case 5 -> 4; // bottom -> top
            default -> throw new IllegalStateException("Invalid face index: " + faceIndex);
        };
    }
}
