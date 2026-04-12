package org.polaris2023.gtu.physics.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.entity.EntityPhysics;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.world.DimensionPhysics;
import org.polaris2023.gtu.physics.world.PhysicsManager;

/**
 * 流体物理计算
 * <p>
 * 实现阿基米德原理（浮力）、流体阻力、水流推力
 * <p>
 * 物理公式：
 * <ul>
 *   <li>浮力: F_b = ρ_流体 × V_浸入 × g</li>
 *   <li>浮力加速度: a_b = F_b / m = (ρ_流体 / ρ_实体 - 1) × g</li>
 *   <li>流体阻力: F_d = 0.5 × ρ × v² × C_d × A</li>
 *   <li>水流推力: 基于流体流动方向和速度</li>
 * </ul>
 */
public class FluidPhysics {

    /**
     * 水的密度 (kg/m³)
     */
    public static final float WATER_DENSITY = 1000.0f;

    /**
     * 岩浆的密度 (kg/m³)
     */
    public static final float LAVA_DENSITY = 3100.0f;

    /**
     * 重力加速度 (m/s²)
     */
    public static final float GRAVITY = 9.80665f;

    /**
     * 计算并应用流体物理效果
     *
     * @param entity 实体
     */
    public static void applyFluidPhysics(Entity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return;

        // 获取实体物理属性
        EntityPhysics physics = entity.getData(DataAttachments.ENTITY_PHYSICS.get());
        if (physics == null) return;

        // 获取维度物理配置
        DimensionPhysics dimPhysics = PhysicsManager.getDimensionPhysics(level);

        // 检查是否在水中
        if (entity.isInWater()) {
            applyWaterPhysics(entity, physics, dimPhysics);
        }
        // 检查是否在岩浆中
        else if (entity.isInLava()) {
            applyLavaPhysics(entity, physics, dimPhysics);
        }
    }

    /**
     * 应用水中物理效果
     */
    private static void applyWaterPhysics(Entity entity, EntityPhysics physics, DimensionPhysics dimPhysics) {
        Vec3 velocity = entity.getDeltaMovement();

        // 1. 计算浮力加速度
        float buoyancyAccel = calculateBuoyancyAcceleration(physics, WATER_DENSITY, dimPhysics.gravity);

        // 2. 应用浮力（向上）
        double newVy = velocity.y + buoyancyAccel * 0.05; // 0.05s = 1 tick

        // 3. 计算流体阻力
        Vec3 drag = calculateFluidDrag(velocity, physics, WATER_DENSITY);

        // 4. 获取水流推力
        Vec3 flowVelocity = getWaterFlowVelocity(entity);

        // 5. 组合所有力
        double vx = (velocity.x + drag.x + flowVelocity.x) * (1.0 - physics.fluidDrag * 0.1);
        double vy = newVy + drag.y;
        double vz = (velocity.z + drag.z + flowVelocity.z) * (1.0 - physics.fluidDrag * 0.1);

        // 6. 应用速度限制
        double maxSpeed = 0.5; // 水中最大速度
        vx = Math.clamp(vx, -maxSpeed, maxSpeed);
        vz = Math.clamp(vz, -maxSpeed, maxSpeed);

        // 对于可浮实体（如船），限制下沉速度
        if (physics.canFloat && vy > 0) {
            vy = Math.min(vy, 0.1);
        }

        entity.setDeltaMovement(vx, vy, vz);
    }

    /**
     * 应用岩浆中物理效果
     */
    private static void applyLavaPhysics(Entity entity, EntityPhysics physics, DimensionPhysics dimPhysics) {
        Vec3 velocity = entity.getDeltaMovement();

        // 岩浆密度更高，浮力更大，但阻力也更大
        float buoyancyAccel = calculateBuoyancyAcceleration(physics, LAVA_DENSITY, dimPhysics.gravity);

        double newVy = velocity.y + buoyancyAccel * 0.05;

        // 岩浆阻力更大
        Vec3 drag = calculateFluidDrag(velocity, physics, LAVA_DENSITY);
        double dragFactor = 0.5; // 岩浆中移动更困难

        double vx = (velocity.x + drag.x) * (1.0 - physics.fluidDrag * dragFactor);
        double vy = newVy + drag.y;
        double vz = (velocity.z + drag.z) * (1.0 - physics.fluidDrag * dragFactor);

        // 岩浆中速度限制更严格
        double maxSpeed = 0.2;
        vx = Math.clamp(vx, -maxSpeed, maxSpeed);
        vz = Math.clamp(vz, -maxSpeed, maxSpeed);

        entity.setDeltaMovement(vx, vy, vz);
    }

    /**
     * 计算浮力加速度
     * <p>
     * 阿基米德原理：浮力 = ρ_流体 × V_浸入 × g
     * 浮力加速度 = 浮力 / 质量 = (ρ_流体 / ρ_实体 - 1) × g
     * <p>
     * 正值 = 上浮，负值 = 下沉
     *
     * @param physics      实体物理属性
     * @param fluidDensity 流体密度 (kg/m³)
     * @param gravity      重力加速度 (m/s²)
     * @return 浮力加速度 (m/s²)
     */
    public static float calculateBuoyancyAcceleration(EntityPhysics physics, float fluidDensity, float gravity) {
        // 浮力加速度 = (ρ_流体 / ρ_实体 - 1) × g × 浮力系数
        float densityRatio = fluidDensity / physics.density;
        float buoyancyAccel = (densityRatio - 1.0f) * gravity * physics.buoyancyFactor;

        // 限制最大浮力加速度，避免实体瞬间弹出水面
        // 使用 Math.abs 确保边界值始终为正
        float maxBuoyancyAccel = Math.abs(gravity) * 0.5f;
        return Math.clamp(buoyancyAccel, -maxBuoyancyAccel, maxBuoyancyAccel);
    }

    /**
     * 计算流体阻力
     * <p>
     * 公式：F_d = 0.5 × ρ × v² × C_d × A
     * 简化为：a_d = v² × dragFactor
     *
     * @param velocity      当前速度
     * @param physics       实体物理属性
     * @param fluidDensity  流体密度
     * @return 阻力向量（与速度方向相反）
     */
    private static Vec3 calculateFluidDrag(Vec3 velocity, EntityPhysics physics, float fluidDensity) {
        double speed = velocity.length();
        if (speed < 0.001) {
            return Vec3.ZERO;
        }

        // 阻力系数
        double dragCoeff = 0.5 * fluidDensity * physics.fluidDrag * physics.volume / physics.getMass();

        // 阻力方向与速度相反
        double dragForce = dragCoeff * speed * speed * 0.05; // 0.05 = dt (1 tick)

        return velocity.normalize().scale(-dragForce);
    }

    /**
     * 获取水流推力
     * <p>
     * 根据水流方向和速度计算推力
     *
     * @param entity 实体
     * @return 水流推力向量
     */
    private static Vec3 getWaterFlowVelocity(Entity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        // 获取当前位置的流体状态
        FluidState fluidState = level.getFluidState(pos);

        if (fluidState.isEmpty() || fluidState.getType() == Fluids.WATER) {
            // 检查是否有水流
            if (!fluidState.isSource()) {
                // 获取水流方向和速度
                Vec3 flow = getFlowVelocity(level, pos, fluidState);

                // 根据水流强度计算推力
                float flowStrength = fluidState.getOwnHeight();

                return flow.scale(flowStrength * 0.1);
            }
        }

        return Vec3.ZERO;
    }

    /**
     * 获取流体流动速度
     */
    private static Vec3 getFlowVelocity(Level level, BlockPos pos, FluidState fluidState) {
        // 简化处理：检查周围方块的流体高度差来估计流向
        double vx = 0, vz = 0;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            FluidState neighborFluid = level.getFluidState(neighborPos);

            if (!neighborFluid.isEmpty()) {
                float heightDiff = neighborFluid.getOwnHeight() - fluidState.getOwnHeight();

                if (heightDiff > 0) {
                    // 流体从这个方向流来
                    vx += -dir.getStepX() * heightDiff;
                    vz += -dir.getStepZ() * heightDiff;
                }
            }
        }

        return new Vec3(vx, 0, vz);
    }

    /**
     * 检查实体是否应该在水中浮起
     *
     * @param entity 实体
     * @return true 如果实体会浮起
     */
    public static boolean shouldFloat(Entity entity) {
        EntityPhysics physics = entity.getData(DataAttachments.ENTITY_PHYSICS.get());
        if (physics == null) return false;

        return physics.willFloat();
    }

    /**
     * 获取实体在水中的浮力信息
     *
     * @param entity 实体
     * @return 浮力信息字符串
     */
    public static String getBuoyancyInfo(Entity entity) {
        EntityPhysics physics = entity.getData(DataAttachments.ENTITY_PHYSICS.get());
        if (physics == null) return "Unknow";

        DimensionPhysics dimPhysics = PhysicsManager.getDimensionPhysics(entity.level());

        float waterBuoyancy = calculateBuoyancyAcceleration(physics, WATER_DENSITY, dimPhysics.gravity);
        float lavaBuoyancy = calculateBuoyancyAcceleration(physics, LAVA_DENSITY, dimPhysics.gravity);

        return String.format("密度: %.0f kg/m³ | 水中浮力: %.2f m/s² | 岩浆浮力: %.2f m/s² | %s",
                physics.density, waterBuoyancy, lavaBuoyancy,
                physics.willFloat() ? "可浮" : "会沉");
    }
}
