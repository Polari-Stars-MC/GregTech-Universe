package org.polaris2023.gtu.space.portal;

import org.joml.Vector3d;

/**
 * Represents a position on a cube planet face.
 * <p>
 * Contains the face index (0-5), the local position relative to the face center,
 * and the world position in the planet dimension.
 * </p>
 *
 * @param faceIndex    The cube face index (0-5):
 *                     <ul>
 *                       <li>0 = Front  (Z-)</li>
 *                       <li>1 = Back   (Z+)</li>
 *                       <li>2 = Left   (X-)</li>
 *                       <li>3 = Right  (X+)</li>
 *                       <li>4 = Top    (Y+)</li>
 *                       <li>5 = Bottom (Y-)</li>
 *                     </ul>
 * @param localPosition  The position relative to the face center
 * @param worldPosition  The absolute world position in the planet dimension
 */
public record FacePosition(
        int faceIndex,
        Vector3d localPosition,
        Vector3d worldPosition
) {
}
