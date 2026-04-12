package org.polaris2023.gtu.physics.inertia;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.world.DimensionPhysics;
import org.polaris2023.gtu.physics.world.PhysicsManager;

/**
 * 玩家惯性状态管理器
 * 管理玩家的滞空状态和惯性运动
 */
public class InertiaManager {

    /**
     * 检查玩家是否处于滞空状态（不能控制移动）
     *
     * @param player 玩家
     * @return true 如果玩家滞空且不能控制
     */
    public static boolean isInAirborne(Player player) {
        // 滞空条件：不在地面、不在水中、不在爬梯子、不在骑乘
        return !player.onGround()
                && !player.isInWater()
                && !player.isInLava()
                && !player.onClimbable()
                && !player.isPassenger()
                && !player.isFallFlying(); // 鞘翅飞行除外
    }

    /**
     * 应用惯性移动
     * 在滞空时，玩家只能保持当前速度，不能主动改变方向
     *
     * @param player 玩家
     */
    public static void applyInertia(Player player) {
        if (!isInAirborne(player)) {
            return;
        }

        // 获取当前维度的物理配置
        DimensionPhysics config = PhysicsManager.getDimensionPhysics(player.level());

        // 获取当前速度
        Vec3 velocity = player.getDeltaMovement();

        // 滞空时只能进行微小的姿态调整，不能大幅度改变方向
        // 水平方向的速度保持不变（只有空气阻力会轻微减速）
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // 应用空气阻力
        double newHorizontalSpeed = horizontalSpeed * (1.0 - config.airResistance);

        if (horizontalSpeed > 0.001) {
            // 保持原有方向，只改变速度大小
            double ratio = newHorizontalSpeed / horizontalSpeed;
            player.setDeltaMovement(velocity.x * ratio, velocity.y, velocity.z * ratio);
        }
    }

    /**
     * 获取玩家的惯性信息
     * @param player 玩家
     * @return 惯性信息字符串
     */
    public static String getInertiaInfo(Player player) {
        DimensionPhysics config = PhysicsManager.getDimensionPhysics(player.level());

        if (!isInAirborne(player)) {
            return "地面/水中";
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20; // m/s
        double verticalSpeed = velocity.y * 20; // m/s

        return String.format("滞空 | 水平: %.2f m/s | 垂直: %.2f m/s | 重力: %.1f m/s²",
                horizontalSpeed, verticalSpeed, config.gravity);
    }
}
