package org.polaris2023.gtu.physics.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.polaris2023.gtu.physics.compat.PhysicsCompatManager;
import org.polaris2023.gtu.physics.config.PhysicsConfig;

/**
 * 维度物理配置
 * <p>
 * 不同维度有不同的物理参数，作为 Level 的数据附件存储
 */
public class DimensionPhysics {

    /**
     * 重力加速度 (m/s²)
     */
    public float gravity;

    /**
     * 空气阻力系数 (每tick减少的速度比例)
     */
    public float airResistance;

    /**
     * 最大坠落速度 (m/s)
     */
    public float terminalVelocity;

    /**
     * 安全坠落高度 (米)
     */
    public float safeFallHeight;

    /**
     * 维度名称
     */
    public String name;

    /**
     * 默认物理配置 (用于未知维度)
     */
    public static final DimensionPhysics DEFAULT = new DimensionPhysics(
            "I don't Know", 9.80665f, 0.02f, 78.4f, 3.0f
    );

    public DimensionPhysics(String name, float gravity, float airResistance,
                            float terminalVelocity, float safeFallHeight) {
        this.name = name;
        this.gravity = gravity;
        this.airResistance = airResistance;
        this.terminalVelocity = terminalVelocity;
        this.safeFallHeight = safeFallHeight;
    }

    /**
     * 根据维度返回默认物理配置
     * <p>
     * 用于 AttachmentType 的默认值生成，根据 Level 的维度 key 决定
     *
     * @param holder Attachment 持有者 (Level)
     * @return 该维度的默认物理配置
     */
    public static DimensionPhysics getDefaultFor(net.neoforged.neoforge.attachment.IAttachmentHolder holder) {
        if (holder instanceof Level level) {
            ResourceKey<Level> dim = level.dimension();

            if (dim.equals(Level.OVERWORLD)) {
                return new DimensionPhysics(
                        dim.location().getPath(),
                        PhysicsConfig.getOverworldGravity(),
                        PhysicsConfig.getOverworldAirResistance(),
                        PhysicsConfig.getOverworldTerminalVelocity(),
                        PhysicsConfig.getOverworldSafeFallHeight()
                );
            }
            if (dim.equals(Level.NETHER)) {
                return new DimensionPhysics(
                        dim.location().getPath(),
                        PhysicsConfig.getNetherGravity(),
                        PhysicsConfig.getNetherAirResistance(),
                        PhysicsConfig.getNetherTerminalVelocity(),
                        PhysicsConfig.getNetherSafeFallHeight()
                );
            }
            if (dim.equals(Level.END)) {
                return new DimensionPhysics(
                        dim.location().getPath(),
                        PhysicsConfig.getEndGravity(),
                        PhysicsConfig.getEndAirResistance(),
                        PhysicsConfig.getEndTerminalVelocity(),
                        PhysicsConfig.getEndSafeFallHeight()
                );
            }

            // 检查模组兼容维度
            DimensionPhysics compat = PhysicsCompatManager.getDefaultForDimension(dim);
            if (compat != null) {
                return compat;
            }
        }

        return DEFAULT;
    }

    /**
     * NBT 序列化器，用于 AttachmentType 持久化
     */
    public static final net.neoforged.neoforge.attachment.IAttachmentSerializer<CompoundTag, DimensionPhysics> SERIALIZER =
            new net.neoforged.neoforge.attachment.IAttachmentSerializer<>() {
                @Override
                public DimensionPhysics read(net.neoforged.neoforge.attachment.IAttachmentHolder holder,
                                           CompoundTag tag, HolderLookup.Provider provider) {
                    return new DimensionPhysics(
                            tag.contains("name") ? tag.getString("name") : "未知维度",
                            tag.contains("gravity") ? tag.getFloat("gravity") : 9.80665f,
                            tag.contains("air_resistance") ? tag.getFloat("air_resistance") : 0.02f,
                            tag.contains("terminal_velocity") ? tag.getFloat("terminal_velocity") : 78.4f,
                            tag.contains("safe_fall_height") ? tag.getFloat("safe_fall_height") : 3.0f
                    );
                }

                @Override
                public CompoundTag write(DimensionPhysics dp, HolderLookup.Provider provider) {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("name", dp.name);
                    tag.putFloat("gravity", dp.gravity);
                    tag.putFloat("air_resistance", dp.airResistance);
                    tag.putFloat("terminal_velocity", dp.terminalVelocity);
                    tag.putFloat("safe_fall_height", dp.safeFallHeight);
                    return tag;
                }
            };

    @Override
    public String toString() {
        return String.format("%s: 重力=%.2f m/s², 空气阻力=%.3f, 安全高度=%.1f m",
                name, gravity, airResistance, safeFallHeight);
    }
}
