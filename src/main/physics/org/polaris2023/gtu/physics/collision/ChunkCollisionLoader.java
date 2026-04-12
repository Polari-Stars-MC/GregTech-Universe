package org.polaris2023.gtu.physics.collision;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.polaris2023.gtu.physics.world.PhysicsWorld;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块碰撞加载器
 * <p>
 * 负责将区块内所有方块的碰撞箱加载到 Bullet 物理世界
 */
public class ChunkCollisionLoader {

    /**
     * 加载整个区块的碰撞到物理世界
     *
     * @param physicsWorld 物理世界
     * @param chunk        区块
     */
    public static void loadChunkCollisions(PhysicsWorld physicsWorld, LevelChunk chunk) {
        Level level = chunk.getLevel();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        // 遍历所有区块段（垂直方向）
        for (int sectionY = level.getMinSection(); sectionY < level.getMaxSection(); sectionY++) {
            LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sectionY));
            if (section == null || section.hasOnlyAir()) {
                continue;
            }

            loadSectionCollisions(physicsWorld, level, section, chunkX, sectionY, chunkZ);
        }
    }

    /**
     * 加载单个区块段的碰撞
     */
    private static void loadSectionCollisions(PhysicsWorld physicsWorld, Level level,
                                              LevelChunkSection section, int chunkX, int sectionY, int chunkZ) {
        int worldY = SectionPos.sectionToBlockCoord(sectionY);

        // 遍历区块段内所有方块
        for (int localX = 0; localX < 16; localX++) {
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    BlockState state = section.getBlockState(localX, localY, localZ);

                    // 跳过空气和无碰撞方块
                    if (state.isAir() || state.getCollisionShape(level, BlockPos.ZERO).isEmpty()) {
                        continue;
                    }

                    // 计算世界坐标
                    int worldX = SectionPos.sectionToBlockCoord(chunkX, localX);
                    int worldZ = SectionPos.sectionToBlockCoord(chunkZ, localZ);
                    int blockY = worldY + localY;

                    BlockPos pos = new BlockPos(worldX, blockY, worldZ);

                    // 加载单个方块的碰撞
                    loadBlockCollision(physicsWorld, level, pos, state);
                }
            }
        }
    }

    /**
     * 加载单个方块的碰撞
     */
    public static void loadBlockCollision(PhysicsWorld physicsWorld, Level level, BlockPos pos, BlockState state) {
        // 使用转换器获取 Bullet 碰撞形状
        CollisionShape shape = VoxelShapeConverter.convert(level, pos, state);
        if (shape == null) {
            return;
        }

        // 计算方块中心位置
        Vector3f position = new Vector3f(
                pos.getX() + 0.5f,
                pos.getY() + 0.5f,
                pos.getZ() + 0.5f
        );

        // 添加到物理世界
        physicsWorld.addBlockBody(pos.asLong(), shape, position);
    }

    /**
     * 卸载整个区块的碰撞
     *
     * @param physicsWorld 物理世界
     * @param level        世界
     * @param chunkX       区块 X
     * @param chunkZ       区块 Z
     */
    public static void unloadChunkCollisions(PhysicsWorld physicsWorld, Level level, int chunkX, int chunkZ) {
        // 遍历所有 Y 段
        for (int sectionY = level.getMinSection(); sectionY < level.getMaxSection(); sectionY++) {
            int worldY = SectionPos.sectionToBlockCoord(sectionY);

            for (int localX = 0; localX < 16; localX++) {
                for (int localY = 0; localY < 16; localY++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldX = SectionPos.sectionToBlockCoord(chunkX, localX);
                        int worldZ = SectionPos.sectionToBlockCoord(chunkZ, localZ);
                        int blockY = worldY + localY;

                        BlockPos pos = new BlockPos(worldX, blockY, worldZ);
                        physicsWorld.removeBlockBody(pos.asLong());
                    }
                }
            }
        }
    }

    /**
     * 高效卸载区块碰撞（使用区块坐标范围）
     */
    public static void unloadChunkCollisionsFast(PhysicsWorld physicsWorld, Level level, int chunkX, int chunkZ) {
        int minX = SectionPos.sectionToBlockCoord(chunkX);
        int minZ = SectionPos.sectionToBlockCoord(chunkZ);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        // 使用迭代器移除所有该区块范围内的碰撞体
        physicsWorld.removeBlockBodiesInRange(minX, minY, minZ, minX + 16, maxY, minZ + 16);
    }
}
