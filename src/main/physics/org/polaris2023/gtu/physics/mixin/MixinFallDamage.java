package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.polaris2023.gtu.physics.PhysicsConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin 到 LivingEntity 类
 * <p>
 * 修改摔落伤害计算，基于真实物理
 * <p>
 * 现实摔落伤害模型：
 * - 冲击速度 v = sqrt(2 * g * h)
 * - 冲击能量 E = 0.5 * m * v²
 * - 人体承受能力基于冲击速度
 * <p>
 * 现实参考数据：
 * - 3米: 轻微受伤 (骨折风险)
 * - 6米: 严重受伤 (多处骨折)
 * - 10米: 危及生命
 * - 15米+: 极高死亡率
 */
@Mixin(LivingEntity.class)
public abstract class MixinFallDamage {

    /**
     * 安全高度 (米) - 不会受伤
     */
    private static final float SAFE_HEIGHT = 3.0f;

    /**
     * 轻伤阈值速度 (m/s) - 约3米高
     */
    private static final float MINOR_INJURY_VELOCITY = 7.67f; // sqrt(2 * 9.8 * 3)

    /**
     * 重伤阈值速度 (m/s) - 约6米高
     */
    private static final float SEVERE_INJURY_VELOCITY = 10.84f; // sqrt(2 * 9.8 * 6)

    /**
     * 致命阈值速度 (m/s) - 约10米高
     */
    private static final float LETHAL_VELOCITY = 14.0f; // sqrt(2 * 9.8 * 10)

    /**
     * 修改摔落伤害计算
     * <p>
     * 基于冲击速度的伤害曲线：
 * - v < 7.67 m/s (3m): 无伤害
     * - v < 10.84 m/s (6m): 轻伤 (1-5 点)
     * - v < 14.0 m/s (10m): 重伤 (5-15 点)
     * - v >= 14.0 m/s: 致命伤 (15+ 点)
     */
    @Inject(method = "calculateFallDamage", at = @At("HEAD"), cancellable = true)
    private void gtu_physics$modifyFallDamage(float fallDistance, float damageMultiplier, CallbackInfoReturnable<Integer> cir) {
        // 安全区：无伤害
        if (fallDistance <= SAFE_HEIGHT) {
            cir.setReturnValue(0);
            return;
        }

        // 计算冲击速度 v = sqrt(2 * g * h)
        double impactVelocity = Math.sqrt(2 * PhysicsConstants.REAL_GRAVITY * fallDistance);

        // 基于速度的伤害计算
        int damage;

        if (impactVelocity < MINOR_INJURY_VELOCITY) {
            // 安全区（不应该到达这里，但保险起见）
            damage = 0;
        } else if (impactVelocity < SEVERE_INJURY_VELOCITY) {
            // 轻伤区: 3-6米
            // 速度每增加 1 m/s，伤害增加约 1.5 点
            double velocityAboveSafe = impactVelocity - MINOR_INJURY_VELOCITY;
            damage = (int) Math.floor(velocityAboveSafe * 1.5);
            damage = Math.max(1, Math.min(damage, 5));
        } else if (impactVelocity < LETHAL_VELOCITY) {
            // 重伤区: 6-10米
            // 基础伤害 5 点，速度每增加 1 m/s，额外伤害约 2.5 点
            double velocityAboveSevere = impactVelocity - SEVERE_INJURY_VELOCITY;
            damage = 5 + (int) Math.floor(velocityAboveSevere * 2.5);
            damage = Math.min(damage, 15);
        } else {
            // 致命区: 10米以上
            // 基础伤害 15 点，速度每增加 1 m/s，额外伤害约 3 点
            double velocityAboveLethal = impactVelocity - LETHAL_VELOCITY;
            damage = 15 + (int) Math.floor(velocityAboveLethal * 3.0);
            // 上限 40 点 (两倍玩家生命值)
            damage = Math.min(damage, 40);
        }

        cir.setReturnValue(damage);
    }
}
