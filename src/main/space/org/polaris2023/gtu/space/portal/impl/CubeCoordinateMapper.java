package org.polaris2023.gtu.space.portal.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.polaris2023.gtu.space.portal.CubeFaceMapping;
import org.polaris2023.gtu.space.portal.FacePosition;
import org.polaris2023.gtu.space.portal.ICoordinateMapper;

public final class CubeCoordinateMapper implements ICoordinateMapper {
    private static final double DEFAULT_PLANET_RADIUS = 5000.0;

    @Override
    public BlockPos mapToSurface(int faceIndex, Vector3d localPosition, double planetRadius) {
        CubeFaceMapping mapping = CubeFaceMapping.getDefault(faceIndex);
        double scale = planetRadius > 0 ? planetRadius : DEFAULT_PLANET_RADIUS;
        double worldX = localPosition.x() * mapping.right().x() * scale
                + localPosition.y() * mapping.up().x() * scale
                + localPosition.z() * mapping.normal().x() * scale;
        double worldY = localPosition.x() * mapping.right().y() * scale
                + localPosition.y() * mapping.up().y() * scale
                + localPosition.z() * mapping.normal().y() * scale;
        double worldZ = localPosition.x() * mapping.right().z() * scale
                + localPosition.y() * mapping.up().z() * scale
                + localPosition.z() * mapping.normal().z() * scale;
        return BlockPos.containing(worldX, worldY, worldZ);
    }

    @Override
    public FacePosition mapToCubeFace(BlockPos surfacePos, String dimension) {
        double x = surfacePos.getX();
        double y = surfacePos.getY();
        double z = surfacePos.getZ();
        double absX = Math.abs(x);
        double absY = Math.abs(y);
        double absZ = Math.abs(z);

        int faceIndex;
        double localU, localV;

        if (absZ >= absX && absZ >= absY) {
            faceIndex = z < 0 ? 0 : 1;
            double sign = z < 0 ? 1.0 : -1.0;
            localU = sign * x / absZ;
            localV = y / absZ;
        } else if (absX >= absY) {
            faceIndex = x < 0 ? 2 : 3;
            double sign = x < 0 ? 1.0 : -1.0;
            localU = sign * z / absX;
            localV = y / absX;
        } else {
            faceIndex = y > 0 ? 4 : 5;
            double sign = y > 0 ? 1.0 : -1.0;
            localU = x / absY;
            localV = sign * z / absY;
        }

        return new FacePosition(
                faceIndex,
                new Vector3d(localU, localV, 0.0),
                new Vector3d(x, y, z)
        );
    }

    @Override
    public BlockPos findSafeSpawnLocation(int faceIndex, BlockPos targetPos, ServerLevel level) {
        BlockPos.MutableBlockPos mutable = targetPos.mutable();
        for (int dy = 0; dy < 32; dy++) {
            mutable.setY(targetPos.getY() + dy);
            BlockState below = level.getBlockState(mutable.below());
            BlockState at = level.getBlockState(mutable);
            BlockState above = level.getBlockState(mutable.above());
            if (!below.isAir() && at.isAir() && above.isAir()) {
                return mutable.immutable();
            }
        }
        return targetPos;
    }

    @Override
    public Vector3f getFaceNormal(int faceIndex) {
        return CubeFaceMapping.getDefault(faceIndex).normal();
    }
}
