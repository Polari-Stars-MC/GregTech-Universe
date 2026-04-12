package org.polaris2023.gtu.physics.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

/**
 * 实体物理属性
 * <p>
 * 存储实体的浮力、密度等物理参数，用于流体物理计算
 * <p>
 * 阿基米德原理：浮力 = ρ_流体 × V_浸入 × g
 * 当浮力 > 重力时，实体上浮；当浮力 < 重力时，实体下沉
 */
public class EntityPhysics {

    /**
     * 实体平均密度 (kg/m³)
     * <p>
     * 人体平均密度 ≈ 985 kg/m³ (略小于水 1000 kg/m³，所以人会浮起来)
     * 铁 ≈ 7874 kg/m³ (会沉)
     * 木头 ≈ 600 kg/m³ (会浮)
     */
    public float density;

    /**
     * 浮力系数 (乘数)
     * <p>
     * 1.0 = 正常浮力
     * > 1.0 = 更容易浮起来（如救生衣）
     * < 1.0 = 更容易下沉（如负重）
     */
    public float buoyancyFactor;

    /**
     * 流体阻力系数
     * <p>
     * 在水中移动时的阻力，越大越难移动
     */
    public float fluidDrag;

    /**
     * 体积 (m³)
     * <p>
     * 根据碰撞箱自动计算，用于浮力计算
     */
    public float volume;

    /**
     * 是否可浮（用于船只等）
     */
    public boolean canFloat;

    /**
     * 默认配置（普通实体）
     */
    public static final EntityPhysics DEFAULT = new EntityPhysics(985.0f, 1.0f, 0.5f, 1.0f, false);

    public EntityPhysics(float density, float buoyancyFactor, float fluidDrag, float volume, boolean canFloat) {
        this.density = density;
        this.buoyancyFactor = buoyancyFactor;
        this.fluidDrag = fluidDrag;
        this.volume = volume;
        this.canFloat = canFloat;
    }

    /**
     * 根据实体类型获取默认物理参数
     */
    public static EntityPhysics getDefaultFor(IAttachmentHolder holder) {
        if (holder instanceof Player player) {
            return getPlayerPhysics(player);
        }
        if (holder instanceof Boat) {
            return getBoatPhysics();
        }
        if (holder instanceof ItemEntity item) {
            return getItemPhysics(item);
        }
        if (holder instanceof LivingEntity living) {
            return getLivingEntityPhysics(living);
        }
        if (holder instanceof Entity entity) {
            return getGenericEntityPhysics(entity);
        }
        return DEFAULT;
    }

    /**
     * 玩家物理参数
     * <p>
     * 人体密度 ≈ 985 kg/m³，略小于水，可以浮起来
     * 肺部有空气时密度更低
     */
    private static EntityPhysics getPlayerPhysics(Player player) {
        // 根据负重调整密度
        float loadFactor = 1.0f;
        // TODO: 根据背包重量调整

        return new EntityPhysics(
                985.0f * loadFactor,  // 密度
                1.0f,                 // 浮力系数
                0.5f,                 // 流体阻力
                calculateVolume(player), // 体积
                false                 // 玩家默认不能像船一样漂浮
        );
    }

    /**
     * 船只物理参数
     * <p>
     * 木船密度低，可以浮在水面上
     */
    private static EntityPhysics getBoatPhysics() {
        return new EntityPhysics(
                500.0f,   // 木头密度
                2.0f,     // 高浮力系数（船体设计）
                0.3f,     // 低流体阻力
                1.0f,     // 体积
                true      // 可以漂浮
        );
    }

    /**
     * 物品实体物理参数
     * <p>
     * 根据物品材质决定是否浮起
     */
    private static EntityPhysics getItemPhysics(ItemEntity item) {
        // 根据物品类型判断密度
        var stack = item.getItem();
        String id = stack.getItem().builtInRegistryHolder().key().location().toString();

        float density = 3000.0f; // 默认较重

        // 木制品会浮
        if (id.contains("wood") || id.contains("log") || id.contains("plank") ||
            id.contains("stick") || id.contains("boat")) {
            density = 600.0f;
        }
        // 食物略微浮起
        else if (id.contains("apple") || id.contains("bread") || id.contains("meat")) {
            density = 800.0f;
        }
        // 石头、矿物会沉
        else if (id.contains("stone") || id.contains("ore") || id.contains("iron") ||
                 id.contains("gold") || id.contains("diamond")) {
            density = 5000.0f;
        }
        // 羊毛、羽毛会浮
        else if (id.contains("wool") || id.contains("feather")) {
            density = 100.0f;
        }

        return new EntityPhysics(
                density,
                1.0f,
                0.8f,  // 物品在水中阻力较大
                0.1f,  // 小体积
                density < 1000.0f  // 密度小于水可以浮
        );
    }

    /**
     * 生物物理参数
     */
    private static EntityPhysics getLivingEntityPhysics(LivingEntity entity) {
        // 大多数生物密度接近水
        float density = 1000.0f;

        // 某些生物更容易浮起
        String id = entity.getType().builtInRegistryHolder().key().location().toString();
        if (id.contains("chicken") || id.contains("duck")) {
            density = 800.0f; // 鸡鸭会浮
        } else if (id.contains("fish") || id.contains("squid") || id.contains("dolphin")) {
            density = 1050.0f; // 水生生物略重于水（需要游泳保持浮力）
        } else if (id.contains("cow") || id.contains("pig") || id.contains("sheep")) {
            density = 950.0f; // 陆地动物略轻于水
        } else if (id.contains("iron_golem")) {
            density = 7874.0f; // 铁傀儡会沉
        }

        return new EntityPhysics(
                density,
                1.0f,
                0.5f,
                calculateVolume(entity),
                false
        );
    }

    /**
     * 通用实体物理参数
     */
    private static EntityPhysics getGenericEntityPhysics(Entity entity) {
        return new EntityPhysics(
                1000.0f,  // 默认等于水密度
                1.0f,
                0.5f,
                calculateVolume(entity),
                false
        );
    }

    /**
     * 根据碰撞箱计算体积
     */
    private static float calculateVolume(Entity entity) {
        var bb = entity.getBoundingBox();
        return (float) (bb.getXsize() * bb.getYsize() * bb.getZsize());
    }

    /**
     * 计算实体质量 (kg)
     */
    public float getMass() {
        return density * volume;
    }

    /**
     * 判断实体是否会在水中浮起
     */
    public boolean willFloat() {
        return density < 1000.0f || canFloat;
    }

    /**
     * NBT 序列化器
     */
    public static final IAttachmentSerializer<CompoundTag, EntityPhysics> SERIALIZER =
            new IAttachmentSerializer<>() {
                @Override
                public EntityPhysics read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                    return new EntityPhysics(
                            tag.contains("density") ? tag.getFloat("density") : 985.0f,
                            tag.contains("buoyancy") ? tag.getFloat("buoyancy") : 1.0f,
                            tag.contains("fluid_drag") ? tag.getFloat("fluid_drag") : 0.5f,
                            tag.contains("volume") ? tag.getFloat("volume") : 1.0f,
                            tag.contains("can_float") && tag.getBoolean("can_float")
                    );
                }

                @Override
                public CompoundTag write(EntityPhysics ep, HolderLookup.Provider provider) {
                    CompoundTag tag = new CompoundTag();
                    tag.putFloat("density", ep.density);
                    tag.putFloat("buoyancy", ep.buoyancyFactor);
                    tag.putFloat("fluid_drag", ep.fluidDrag);
                    tag.putFloat("volume", ep.volume);
                    tag.putBoolean("can_float", ep.canFloat);
                    return tag;
                }
            };
}
