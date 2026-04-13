package org.polaris2023.gtu.physics.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.polaris2023.gtu.physics.GregtechUniversePhysics;
import org.polaris2023.gtu.physics.collision.ChunkCollisionLoader;
import org.polaris2023.gtu.physics.collision.EntityCollisionManager;
import org.polaris2023.gtu.physics.collision.RigidBodyCollisionDetector;
import org.polaris2023.gtu.physics.collision.VoxelShapeConverter;
import org.polaris2023.gtu.physics.world.PhysicsManager;
import org.polaris2023.gtu.physics.world.PhysicsPauseManager;
import org.polaris2023.gtu.physics.world.PhysicsWorld;

/**
 * 物理事件处理器
 * <p>
 * 处理物理世界的生命周期和更新
 */
@EventBusSubscriber(modid = GregtechUniversePhysics.MOD_ID)
public class PhysicsEventHandler {

    /**
     * 服务器启动时初始化物理系统
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        PhysicsManager.initServerPhysics();

        // 默认禁用区块碰撞加载（太慢）
        ChunkCollisionLoader.setEnabled(false);
    }

    /**
     * 服务器停止时清理物理系统
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        EntityCollisionManager.getInstance().shutdown();
        ChunkCollisionLoader.shutdown();
        RigidBodyCollisionDetector.getInstance().clearStackingState();
        VoxelShapeConverter.clearCache();
        PhysicsManager.shutdown();
    }

    /**
     * 世界加载时创建物理世界
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PhysicsManager.getOrCreatePhysicsWorld(level);
        }
    }

    /**
     * 世界卸载时销毁物理世界
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            PhysicsManager.unloadPhysicsWorld((Level) event.getLevel());
        }
    }

    /**
     * 区块加载时异步加载碰撞（默认禁用）
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // 区块碰撞加载默认禁用，可通过配置启用
        // ChunkCollisionLoader.loadChunkCollisionsAsync(physicsWorld, chunk);
    }

    /**
     * 区块卸载时卸载碰撞
     */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(level);
            if (physicsWorld != null) {
                ChunkCollisionLoader.unloadChunkCollisionsFast(physicsWorld, level, chunk.getPos().x, chunk.getPos().z);
            }
        }
    }

    /**
     * 每tick更新物理世界
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerTick(ServerTickEvent.Pre event) {
        // 检查暂停状态
        PhysicsPauseManager pauseManager = PhysicsPauseManager.getInstance();

        for (ServerLevel level : event.getServer().getAllLevels()) {
            PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(level);

            // 处理待创建的实体刚体
            PhysicsManager.processPendingCreations();

            // 提交异步加载的区块碰撞
            if (physicsWorld != null) {
                ChunkCollisionLoader.submitPendingCollisions(physicsWorld);
            }

            // 物理步进（内部会检查暂停状态）
            PhysicsManager.tickPhysics(level);

            // 检测实体堆叠状态
            RigidBodyCollisionDetector.getInstance().detectStacking(level);
        }
    }

    /**
     * 实体加入世界时延迟创建物理体
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            // 使用延迟创建，避免大量实体同时加入时卡顿
            PhysicsManager.createEntityBodyDeferred(event.getEntity());
        }
    }

    /**
     * 实体离开世界时移除物理体
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(event.getLevel());
            if (physicsWorld != null) {
                physicsWorld.removeEntityBody(event.getEntity().getId());
            }
        }
    }

    /**
     * 方块放置时添加碰撞体
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide()) {
            PhysicsManager.updateBlockCollision((Level) event.getLevel(), event.getPos(), event.getPlacedBlock());
        }
    }

    /**
     * 方块破坏时移除碰撞体
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide()) {
            PhysicsManager.removeBlockCollision((Level) event.getLevel(), event.getPos());
        }
    }
}
