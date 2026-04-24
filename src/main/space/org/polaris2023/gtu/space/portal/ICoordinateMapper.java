package org.polaris2023.gtu.space.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Maps coordinates between cube planet faces and planet surface locations.
 * <p>
 * The CoordinateMapper handles the bidirectional mapping between positions on
 * cube planet faces and their corresponding surface coordinates in planet dimensions.
 * It also provides utilities for finding safe spawn locations.
 * </p>
 *
 * @see FacePosition
 * @see Portal
 */
public interface ICoordinateMapper {

    /**
     * Maps a position on a cube face to surface coordinates.
     *
     * @param faceIndex    The cube face index (0-5)
     * @param localPosition The position relative to the face center
     * @param planetRadius The radius of the planet in meters
     * @return The corresponding surface position in the planet dimension
     */
    BlockPos mapToSurface(int faceIndex, Vector3d localPosition, double planetRadius);

    /**
     * Maps a surface position back to cube face coordinates.
     *
     * @param surfacePos The position in the planet dimension
     * @param dimension  The planet dimension key
     * @return The face position containing face index and local coordinates
     */
    FacePosition mapToCubeFace(BlockPos surfacePos, String dimension);

    /**
     * Finds a safe spawn location near the target position.
     * <p>
     * If the target position is blocked or unsafe, this method searches
     * nearby positions to find a suitable spawn location.
     * </p>
     *
     * @param faceIndex The target face index
     * @param targetPos The desired position
     * @param level     The server level to search in
     * @return A safe spawn position, or the original if already safe
     */
    BlockPos findSafeSpawnLocation(int faceIndex, BlockPos targetPos, ServerLevel level);

    /**
     * Returns the face normal for the given face index.
     * <p>
     * Face indices:
     * <ul>
     *   <li>0 = Front  (Z-)</li>
     *   <li>1 = Back   (Z+)</li>
     *   <li>2 = Left   (X-)</li>
     *   <li>3 = Right  (X+)</li>
     *   <li>4 = Top    (Y+)</li>
     *   <li>5 = Bottom (Y-)</li>
     * </ul>
     * </p>
     *
     * @param faceIndex The face index (0-5)
     * @return The normal vector for that face
     */
    Vector3f getFaceNormal(int faceIndex);
}
