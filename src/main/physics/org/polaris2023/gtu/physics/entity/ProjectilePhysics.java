package org.polaris2023.gtu.physics.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

/**
 * 投掷物物理状态
 * <p>
 * 存储投掷物的物理参数，用于弹道计算
 */
public class ProjectilePhysics {

    /**
     * 质量 (千克)
     */
    public float mass;

    /**
     * 空气阻力系数 (无量纲)
     * 典型值: 球体 ~0.47, 流线型 ~0.04, 平板 ~1.28
     */
    public float dragCoefficient;

    /**
     * 横截面积 (平方米)
     */
    public float crossSectionArea;

    /**
     * 初始速度 (m/s)
     */
    public float initialSpeed;

    /**
     * 是否已初始化
     */
    public boolean initialized;

    /**
     * 默认配置 (未知投掷物)
     */
    public static final ProjectilePhysics DEFAULT = new ProjectilePhysics(0.1f, 0.47f, 0.001f);

    public ProjectilePhysics(float mass, float dragCoefficient, float crossSectionArea) {
        this.mass = mass;
        this.dragCoefficient = dragCoefficient;
        this.crossSectionArea = crossSectionArea;
        this.initialized = false;
    }

    /**
     * 根据投掷物类型获取默认物理参数
     */
    public static ProjectilePhysics getDefaultFor(IAttachmentHolder holder) {
        if (holder instanceof AbstractArrow arrow) {
            return getArrowPhysics(arrow);
        }
        if (holder instanceof ThrowableProjectile projectile) {
            return getThrowablePhysics(projectile);
        }
        return DEFAULT;
    }

    /**
     * 箭矢物理参数
     * <p>
     * 箭矢: 质量 ~0.025kg, C_d ~0.2 (流线型), 面积 ~0.0001 m²
     */
    private static ProjectilePhysics getArrowPhysics(AbstractArrow arrow) {
        String id = arrow.getType().builtInRegistryHolder().key().location().toString();

        // 根据箭矢类型调整参数
        float mass = 0.025f;  // 25g
        float drag = 0.2f;    // 流线型箭矢
        float area = 0.00005f; // 箭杆横截面积

        // 特殊箭矢
        if (id.contains("spectral")) {
            mass = 0.015f;
            drag = 0.1f; // 光谱箭更轻更快
        }

        return new ProjectilePhysics(mass, drag, area);
    }

    /**
     * 投掷物物理参数
     * <p>
     * 雪球、末影珍珠等
     */
    private static ProjectilePhysics getThrowablePhysics(ThrowableProjectile projectile) {
        String id = projectile.getType().builtInRegistryHolder().key().location().toString();

        // 雪球
        if (id.contains("snowball")) {
            return new ProjectilePhysics(0.1f, 0.47f, 0.002f); // 球体
        }

        // 末影珍珠
        if (id.contains("ender_pearl")) {
            return new ProjectilePhysics(0.1f, 0.4f, 0.001f);
        }

        // 鸡蛋
        if (id.contains("egg")) {
            return new ProjectilePhysics(0.05f, 0.4f, 0.002f);
        }

        // 喷溅药水
        if (id.contains("potion") || id.contains("splash")) {
            return new ProjectilePhysics(0.3f, 0.45f, 0.003f);
        }

        // 恶火球
        if (id.contains("fireball")) {
            return new ProjectilePhysics(0.1f, 0.3f, 0.01f);
        }

        // 默认投掷物
        return new ProjectilePhysics(0.1f, 0.47f, 0.002f);
    }

    /**
     * NBT 序列化器
     */
    public static final IAttachmentSerializer<CompoundTag, ProjectilePhysics> SERIALIZER =
            new IAttachmentSerializer<>() {
                @Override
                public ProjectilePhysics read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                    return new ProjectilePhysics(
                            tag.contains("mass") ? tag.getFloat("mass") : 0.1f,
                            tag.contains("drag") ? tag.getFloat("drag") : 0.47f,
                            tag.contains("area") ? tag.getFloat("area") : 0.002f
                    );
                }

                @Override
                public CompoundTag write(ProjectilePhysics pp, HolderLookup.Provider provider) {
                    CompoundTag tag = new CompoundTag();
                    tag.putFloat("mass", pp.mass);
                    tag.putFloat("drag", pp.dragCoefficient);
                    tag.putFloat("area", pp.crossSectionArea);
                    return tag;
                }
            };
}
