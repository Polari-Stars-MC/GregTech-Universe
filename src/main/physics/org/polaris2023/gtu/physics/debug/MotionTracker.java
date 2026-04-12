package org.polaris2023.gtu.physics.debug;

import com.jme3.math.Vector3f;
import net.minecraft.world.entity.Entity;
import org.polaris2023.gtu.physics.PhysicsConstants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 实体运动轨迹记录器
 * <p>
 * 记录实体的位置、速度、加速度历史，用于绘制抛物线和加速度曲线
 */
public class MotionTracker {

    /**
     * 记录时长 (秒)
     */
    private static final int RECORD_SECONDS = 30;

    /**
     * 最大记录帧数 (30秒 * 20 ticks/秒 = 600)
     */
    private static final int MAX_HISTORY = RECORD_SECONDS * PhysicsConstants.TICKS_PER_SECOND;

    /**
     * 位置历史 (世界坐标)
     */
    private final LinkedList<Vector3f> positionHistory = new LinkedList<>();

    /**
     * 速度历史 (m/s)
     */
    private final LinkedList<Vector3f> velocityHistory = new LinkedList<>();

    /**
     * 加速度历史 (m/s²)
     */
    private final LinkedList<Vector3f> accelerationHistory = new LinkedList<>();

    /**
     * 时间戳历史 (tick)
     */
    private final LinkedList<Long> tickHistory = new LinkedList<>();

    /**
     * Y轴高度历史 (用于绘制高度曲线)
     */
    private final LinkedList<Float> heightHistory = new LinkedList<>();

    /**
     * 速度大小历史 (用于绘制速度曲线)
     */
    private final LinkedList<Float> speedHistory = new LinkedList<>();

    /**
     * Y轴速度历史 (用于绘制垂直速度曲线)
     */
    private final LinkedList<Float> velocityYHistory = new LinkedList<>();

    /**
     * 当前加速度 (m/s²)
     */
    private Vector3f currentAcceleration = new Vector3f(0, 0, 0);

    /**
     * 记录一帧数据
     *
     * @param entity 实体
     * @param tick   当前 tick
     */
    public void record(Entity entity, long tick) {
        Vector3f position = new Vector3f(
                (float) entity.getX(),
                (float) entity.getY(),
                (float) entity.getZ()
        );

        Vector3f velocity = new Vector3f(
                (float) entity.getDeltaMovement().x * 20, // 转换为 m/s
                (float) entity.getDeltaMovement().y * 20,
                (float) entity.getDeltaMovement().z * 20
        );

        // 计算加速度 (与上一帧速度差)
        if (!velocityHistory.isEmpty()) {
            Vector3f lastVelocity = velocityHistory.getLast();
            currentAcceleration.set(
                    (velocity.x - lastVelocity.x) * 20, // 转换为 m/s²
                    (velocity.y - lastVelocity.y) * 20,
                    (velocity.z - lastVelocity.z) * 20
            );
        }

        // 添加到历史
        positionHistory.addLast(position);
        velocityHistory.addLast(velocity);
        accelerationHistory.addLast(new Vector3f(currentAcceleration));
        tickHistory.addLast(tick);

        // 添加曲线数据
        heightHistory.addLast((float) entity.getY());
        speedHistory.addLast(velocity.length());
        velocityYHistory.addLast(velocity.y);

        // 限制历史长度
        while (positionHistory.size() > MAX_HISTORY) {
            positionHistory.removeFirst();
            velocityHistory.removeFirst();
            accelerationHistory.removeFirst();
            tickHistory.removeFirst();
            heightHistory.removeFirst();
            speedHistory.removeFirst();
            velocityYHistory.removeFirst();
        }
    }

    /**
     * 获取位置历史 (用于绘制抛物线)
     */
    public List<Vector3f> getPositionHistory() {
        return new ArrayList<>(positionHistory);
    }

    /**
     * 获取速度历史
     */
    public List<Vector3f> getVelocityHistory() {
        return new ArrayList<>(velocityHistory);
    }

    /**
     * 获取加速度历史
     */
    public List<Vector3f> getAccelerationHistory() {
        return new ArrayList<>(accelerationHistory);
    }

    /**
     * 获取高度历史 (Y轴位置)
     */
    public List<Float> getHeightHistory() {
        return new ArrayList<>(heightHistory);
    }

    /**
     * 获取速度大小历史
     */
    public List<Float> getSpeedHistory() {
        return new ArrayList<>(speedHistory);
    }

    /**
     * 获取Y轴速度历史
     */
    public List<Float> getVelocityYHistory() {
        return new ArrayList<>(velocityYHistory);
    }

    /**
     * 获取记录时长 (秒)
     */
    public int getRecordSeconds() {
        return RECORD_SECONDS;
    }

    /**
     * 获取当前加速度
     */
    public Vector3f getCurrentAcceleration() {
        return currentAcceleration;
    }

    /**
     * 获取当前速度大小 (m/s)
     */
    public float getCurrentSpeed() {
        if (velocityHistory.isEmpty()) return 0;
        return velocityHistory.getLast().length();
    }

    /**
     * 获取当前加速度大小 (m/s²)
     */
    public float getCurrentAccelerationMagnitude() {
        return currentAcceleration.length();
    }

    /**
     * 获取理论抛物线预测点
     *
     * @param steps      预测步数
     * @param gravity    重力加速度 (m/s²)
     * @param timeStep   时间步长 (秒)
     * @return 预测位置列表
     */
    public List<Vector3f> predictParabola(int steps, float gravity, float timeStep) {
        List<Vector3f> prediction = new ArrayList<>();

        if (velocityHistory.isEmpty() || positionHistory.isEmpty()) {
            return prediction;
        }

        Vector3f startPos = new Vector3f(positionHistory.getLast());
        Vector3f velocity = new Vector3f(velocityHistory.getLast());

        for (int i = 0; i < steps; i++) {
            float t = i * timeStep;

            // 抛物线运动方程: p = p0 + v0*t + 0.5*g*t²
            float x = startPos.x + velocity.x * t;
            float y = startPos.y + velocity.y * t + 0.5f * gravity * t * t;
            float z = startPos.z + velocity.z * t;

            prediction.add(new Vector3f(x, y, z));
        }

        return prediction;
    }

    /**
     * 清除历史
     */
    public void clear() {
        positionHistory.clear();
        velocityHistory.clear();
        accelerationHistory.clear();
        tickHistory.clear();
        heightHistory.clear();
        speedHistory.clear();
        velocityYHistory.clear();
        currentAcceleration.set(0, 0, 0);
    }

    /**
     * 获取统计数据
     */
    public MotionStats getStats() {
        MotionStats stats = new MotionStats();

        if (!velocityHistory.isEmpty()) {
            stats.currentSpeed = velocityHistory.getLast().length();
            stats.currentVelocity = new Vector3f(velocityHistory.getLast());
        }

        stats.currentAcceleration = new Vector3f(currentAcceleration);
        stats.accelerationMagnitude = currentAcceleration.length();

        if (!heightHistory.isEmpty()) {
            stats.currentHeight = heightHistory.getLast();
        }

        if (!accelerationHistory.isEmpty()) {
            // 计算平均加速度
            Vector3f avgAccel = new Vector3f();
            for (Vector3f acc : accelerationHistory) {
                avgAccel.addLocal(acc);
            }
            avgAccel.divideLocal(accelerationHistory.size());
            stats.averageAcceleration = avgAccel;
        }

        return stats;
    }

    /**
     * 运动统计数据
     */
    public static class MotionStats {
        public float currentSpeed;
        public Vector3f currentVelocity = new Vector3f();
        public Vector3f currentAcceleration = new Vector3f();
        public float accelerationMagnitude;
        public Vector3f averageAcceleration = new Vector3f();
        public float currentHeight;
    }
}
