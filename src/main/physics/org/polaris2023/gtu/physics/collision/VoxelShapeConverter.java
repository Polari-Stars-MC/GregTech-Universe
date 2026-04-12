package org.polaris2023.gtu.physics.collision;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * VoxelShape 转换为 Bullet CollisionShape
 * <p>
 * 兼容所有 mod 的方块碰撞箱
 */
public final class VoxelShapeConverter {

    private VoxelShapeConverter() {
    }

    /**
     * 将方块的 VoxelShape 转换为 Bullet CollisionShape
     * <p>
     * 使用标准 API 获取碰撞箱，兼容所有 mod
     *
     * @param level    世界
     * @param pos      方块位置
     * @param state    方块状态
     * @return CollisionShape，如果方块无碰撞返回 null
     */
    public static CollisionShape convert(BlockGetter level, BlockPos pos, BlockState state) {
        // 使用标准 API 获取碰撞箱，兼容所有 mod
        VoxelShape voxelShape = state.getCollisionShape(level, pos);

        // 空碰撞箱
        if (voxelShape.isEmpty()) {
            return null;
        }

        // 完整方块优化
        if (voxelShape.equals(Shapes.block())) {
            return new BoxCollisionShape(0.5f, 0.5f, 0.5f);
        }

        // 复杂形状：提取所有 AABB 并创建复合碰撞形状
        List<AABBInfo> boxes = extractAABBs(voxelShape);

        if (boxes.isEmpty()) {
            return null;
        }

        // 单个 AABB
        if (boxes.size() == 1) {
            AABBInfo box = boxes.get(0);
            return createBoxShape(box);
        }

        // 多个 AABB：创建复合碰撞形状
        return createCompoundShape(boxes);
    }

    /**
     * 从 VoxelShape 提取所有 AABB
     */
    private static List<AABBInfo> extractAABBs(VoxelShape voxelShape) {
        List<AABBInfo> boxes = new ArrayList<>();

        voxelShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> boxes.add(new AABBInfo(minX, minY, minZ, maxX, maxY, maxZ)));

        return boxes;
    }

    /**
     * 创建单个 BoxCollisionShape
     */
    private static BoxCollisionShape createBoxShape(AABBInfo box) {
        float halfWidth = (float) ((box.maxX - box.minX) / 2.0);
        float halfHeight = (float) ((box.maxY - box.minY) / 2.0);
        float halfDepth = (float) ((box.maxZ - box.minZ) / 2.0);
        return new BoxCollisionShape(halfWidth, halfHeight, halfDepth);
    }

    /**
     * 创建指定尺寸的 BoxCollisionShape
     *
     * @param halfWidth  半宽
     * @param halfHeight 半高
     * @param halfDepth  半深
     * @return BoxCollisionShape
     */
    public static BoxCollisionShape createBoxShape(float halfWidth, float halfHeight, float halfDepth) {
        return new BoxCollisionShape(halfWidth, halfHeight, halfDepth);
    }

    /**
     * 创建复合碰撞形状
     */
    private static CollisionShape createCompoundShape(List<AABBInfo> boxes) {
        CompoundCollisionShape compound = new CompoundCollisionShape(boxes.size());

        for (AABBInfo box : boxes) {
            BoxCollisionShape boxShape = createBoxShape(box);

            // 计算中心偏移（相对于方块中心）
            float centerX = (float) ((box.minX + box.maxX) / 2.0 - 0.5);
            float centerY = (float) ((box.minY + box.maxY) / 2.0 - 0.5);
            float centerZ = (float) ((box.minZ + box.maxZ) / 2.0 - 0.5);

            compound.addChildShape(boxShape, new Vector3f(centerX, centerY, centerZ));
        }

        return compound;
    }

    /**
     * AABB 信息
     */
    private record AABBInfo(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }
}
