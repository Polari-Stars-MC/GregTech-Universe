package org.polaris2023.gtu.physics.rotation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

/**
 * 旋转物理属性
 * <p>
 * 存储实体的转动惯量、角速度、角加速度等旋转物理参数
 * <p>
 * 物理公式：
 * <ul>
 *   <li>转动惯量: I = ∫r²dm (对于刚体)</li>
 *   <li>角动量: L = I × ω</li>
 *   <li>力矩: τ = I × α = dL/dt</li>
 *   <li>转动动能: E = 0.5 × I × ω²</li>
 * </ul>
 * <p>
 * 常见形状的转动惯量：
 * <ul>
 *   <li>球体(绕中心): I = (2/5) × m × r²</li>
 *   <li>圆柱体(绕轴): I = (1/2) × m × r²</li>
 *   <li>长方体(绕中心轴): I = (1/12) × m × (w² + h²)</li>
 *   <li>细杆(绕中心): I = (1/12) × m × L²</li>
 *   <li>细杆(绕端点): I = (1/3) × m × L²</li>
 * </ul>
 */
public class RotationalPhysics {

    /**
     * 转动惯量张量的主对角线分量 (kg·m²)
     * <p>
     * Ixx, Iyy, Izz 分别为绕 x, y, z 轴的转动惯量
     * 对于对称物体，这些就是主转动惯量
     */
    public float Ixx, Iyy, Izz;

    /**
     * 角速度向量 (rad/s)
     * <p>
     * ωx, ωy, ωz 分别为绕 x, y, z 轴的角速度
     */
    public float omegaX, omegaY, omegaZ;

    /**
     * 角加速度向量 (rad/s²)
     */
    public float alphaX, alphaY, alphaZ;

    /**
     * 力矩向量 (N·m)
     * <p>
     * τ = r × F (力矩 = 力臂 × 力)
     */
    public float torqueX, torqueY, torqueZ;

    /**
     * 角阻尼系数 (1/s)
     * <p>
     * 模拟空气阻力/摩擦导致的旋转减速
     */
    public float angularDamping;

    /**
     * 当前旋转角度 (rad)
     * <p>
     * 用于渲染和碰撞检测
     */
    public float rotationX, rotationY, rotationZ;

    /**
     * 质量 (kg) - 缓存用于计算
     */
    public float mass;

    /**
     * 是否启用旋转物理
     */
    public boolean enabled;

    /**
     * 默认配置（不旋转）
     */
    public static final RotationalPhysics DEFAULT = new RotationalPhysics(
            1.0f, 1.0f, 1.0f,  // Ixx, Iyy, Izz
            0.0f, 0.0f, 0.0f,  // omega
            0.1f,              // damping
            1.0f,              // mass
            false              // enabled
    );

    public RotationalPhysics(float Ixx, float Iyy, float Izz,
                             float omegaX, float omegaY, float omegaZ,
                             float angularDamping, float mass, boolean enabled) {
        this.Ixx = Ixx;
        this.Iyy = Iyy;
        this.Izz = Izz;
        this.omegaX = omegaX;
        this.omegaY = omegaY;
        this.omegaZ = omegaZ;
        this.angularDamping = angularDamping;
        this.mass = mass;
        this.enabled = enabled;
        this.alphaX = this.alphaY = this.alphaZ = 0.0f;
        this.torqueX = this.torqueY = this.torqueZ = 0.0f;
        this.rotationX = this.rotationY = this.rotationZ = 0.0f;
    }

    /**
     * 根据实体类型获取默认旋转物理参数
     */
    public static RotationalPhysics getDefaultFor(IAttachmentHolder holder) {
        if (holder instanceof AbstractArrow arrow) {
            return getArrowPhysics(arrow);
        }
        if (holder instanceof ItemEntity item) {
            return getItemPhysics(item);
        }
        if (holder instanceof Boat boat) {
            return getBoatPhysics(boat);
        }
        if (holder instanceof Player player) {
            return getPlayerPhysics(player);
        }
        if (holder instanceof Entity entity) {
            return getGenericEntityPhysics(entity);
        }
        return DEFAULT;
    }

    /**
     * 箭矢旋转物理
     * <p>
     * 箭矢主要绕飞行轴旋转（陀螺效应稳定飞行）
     * 细杆绕中心: I = (1/12) × m × L²
     */
    private static RotationalPhysics getArrowPhysics(AbstractArrow arrow) {
        float length = 1.0f;  // 箭杆长度 (m)
        float radius = 0.05f; // 箭杆半径 (m)
        float mass = 0.025f;  // 25g

        // 细杆绕垂直轴的转动惯量
        float Iyy = (1.0f / 12.0f) * mass * length * length;
        // 绕自身轴的转动惯量（圆柱体）
        float Ixx = (1.0f / 2.0f) * mass * radius * radius;
        float Izz = Ixx;

        return new RotationalPhysics(
                Ixx, Iyy, Izz,
                0.0f, 0.0f, 50.0f,  // 初始角速度：绕z轴快速旋转（陀螺稳定）
                0.02f,              // 低阻尼
                mass,
                true                // 启用旋转
        );
    }

    /**
     * 物品实体旋转物理
     * <p>
     * 掉落的物品会旋转
     */
    private static RotationalPhysics getItemPhysics(ItemEntity item) {
        float size = 0.25f;  // 物品大小
        float mass = 0.1f;   // 估算质量

        // 球体转动惯量: I = (2/5) × m × r²
        float I = (2.0f / 5.0f) * mass * size * size;

        return new RotationalPhysics(
                I, I, I,
                (float) (Math.random() - 0.5) * 10.0f,  // 随机初始角速度
                (float) (Math.random() - 0.5) * 10.0f,
                (float) (Math.random() - 0.5) * 10.0f,
                0.1f,    // 中等阻尼
                mass,
                true
        );
    }

    /**
     * 船只旋转物理
     * <p>
     * 船只在水中可以旋转，主要受水阻力影响
     */
    private static RotationalPhysics getBoatPhysics(Boat boat) {
        float length = 2.0f;   // 船长
        float width = 0.8f;    // 船宽
        float height = 0.5f;   // 船高
        float mass = 100.0f;   // 木船质量

        // 长方体转动惯量
        // Ixx (绕x轴，前后翻滚): (1/12) × m × (width² + height²)
        // Iyy (绕y轴，左右转向): (1/12) × m × (length² + width²)
        // Izz (绕z轴，侧倾): (1/12) × m × (length² + height²)
        float Ixx = (1.0f / 12.0f) * mass * (width * width + height * height);
        float Iyy = (1.0f / 12.0f) * mass * (length * length + width * width);
        float Izz = (1.0f / 12.0f) * mass * (length * length + height * height);

        return new RotationalPhysics(
                Ixx, Iyy, Izz,
                0.0f, 0.0f, 0.0f,
                0.5f,    // 水中高阻尼
                mass,
                true
        );
    }

    /**
     * 玩家旋转物理
     * <p>
     * 玩家主要控制转向，物理旋转有限
     */
    private static RotationalPhysics getPlayerPhysics(Player player) {
        float height = 1.8f;
        float width = 0.6f;
        float mass = 70.0f;

        // 圆柱体近似
        float radius = width / 2.0f;
        // Iyy (转向): (1/2) × m × r²
        float Iyy = (1.0f / 2.0f) * mass * radius * radius;
        // Ixx, Izz (前后/侧向翻滚): (1/12) × m × (3r² + h²)
        float Ixz = (1.0f / 12.0f) * mass * (3 * radius * radius + height * height);

        return new RotationalPhysics(
                Ixz, Iyy, Ixz,
                0.0f, 0.0f, 0.0f,
                1.0f,    // 高阻尼，玩家控制优先
                mass,
                false    // 玩家不启用物理旋转（由输入控制）
        );
    }

    /**
     * 通用实体旋转物理
     */
    private static RotationalPhysics getGenericEntityPhysics(Entity entity) {
        AABB bb = entity.getBoundingBox();
        float width = (float) bb.getXsize();
        float height = (float) bb.getYsize();
        float depth = (float) bb.getZsize();
        float mass = width * height * depth * 100.0f;  // 估算质量

        // 长方体转动惯量
        float Ixx = (1.0f / 12.0f) * mass * (depth * depth + height * height);
        float Iyy = (1.0f / 12.0f) * mass * (width * width + depth * depth);
        float Izz = (1.0f / 12.0f) * mass * (width * width + height * height);

        return new RotationalPhysics(
                Ixx, Iyy, Izz,
                0.0f, 0.0f, 0.0f,
                0.2f,
                mass,
                false
        );
    }

    // ==================== 物理计算方法 ====================

    /**
     * 计算角动量向量 (kg·m²/s)
     * <p>
     * L = I × ω
     */
    public float[] getAngularMomentum() {
        return new float[] {
                Ixx * omegaX,
                Iyy * omegaY,
                Izz * omegaZ
        };
    }

    /**
     * 计算角动量大小 (kg·m²/s)
     */
    public float getAngularMomentumMagnitude() {
        float[] L = getAngularMomentum();
        return (float) Math.sqrt(L[0] * L[0] + L[1] * L[1] + L[2] * L[2]);
    }

    /**
     * 计算转动动能 (J)
     * <p>
     * E = 0.5 × (Ixx×ωx² + Iyy×ωy² + Izz×ωz²)
     */
    public float getRotationalKineticEnergy() {
        return 0.5f * (Ixx * omegaX * omegaX + Iyy * omegaY * omegaY + Izz * omegaZ * omegaZ);
    }

    /**
     * 计算角速度大小 (rad/s)
     */
    public float getAngularSpeed() {
        return (float) Math.sqrt(omegaX * omegaX + omegaY * omegaY + omegaZ * omegaZ);
    }

    /**
     * 施加力矩
     *
     * @param tx 绕x轴力矩 (N·m)
     * @param ty 绕y轴力矩 (N·m)
     * @param tz 绕z轴力矩 (N·m)
     */
    public void applyTorque(float tx, float ty, float tz) {
        this.torqueX += tx;
        this.torqueY += ty;
        this.torqueZ += tz;
    }

    /**
     * 施加冲量矩（瞬时角动量变化）
     * <p>
     * ΔL = τ × Δt = r × J (冲量矩)
     *
     * @param Lx x方向角动量变化
     * @param Ly y方向角动量变化
     * @param Lz z方向角动量变化
     */
    public void applyAngularImpulse(float Lx, float Ly, float Lz) {
        // Δω = ΔL / I
        this.omegaX += Lx / Ixx;
        this.omegaY += Ly / Iyy;
        this.omegaZ += Lz / Izz;
    }

    /**
     * 施加碰撞产生的力矩
     * <p>
     * τ = r × F
     *
     * @param rx 接触点相对于质心的x坐标
     * @param ry 接触点相对于质心的y坐标
     * @param rz 接触点相对于质心的z坐标
     * @param fx 碰撞力x分量
     * @param fy 碰撞力y分量
     * @param fz 碰撞力z分量
     */
    public void applyCollisionTorque(float rx, float ry, float rz,
                                     float fx, float fy, float fz) {
        // τ = r × F
        float tx = ry * fz - rz * fy;
        float ty = rz * fx - rx * fz;
        float tz = rx * fy - ry * fx;
        applyTorque(tx, ty, tz);
    }

    /**
     * 更新旋转状态（每tick调用）
     *
     * @param deltaTime 时间步长 (秒)
     */
    public void update(float deltaTime) {
        if (!enabled) return;

        // 1. 计算角加速度: α = τ / I
        alphaX = torqueX / Ixx;
        alphaY = torqueY / Iyy;
        alphaZ = torqueZ / Izz;

        // 2. 应用角阻尼: α -= damping × ω
        alphaX -= angularDamping * omegaX;
        alphaY -= angularDamping * omegaY;
        alphaZ -= angularDamping * omegaZ;

        // 3. 更新角速度: ω += α × dt
        omegaX += alphaX * deltaTime;
        omegaY += alphaY * deltaTime;
        omegaZ += alphaZ * deltaTime;

        // 4. 更新旋转角度: θ += ω × dt
        rotationX += omegaX * deltaTime;
        rotationY += omegaY * deltaTime;
        rotationZ += omegaZ * deltaTime;

        // 5. 角度归一化到 [0, 2π)
        rotationX = normalizeAngle(rotationX);
        rotationY = normalizeAngle(rotationY);
        rotationZ = normalizeAngle(rotationZ);

        // 6. 清除力矩（下一帧重新计算）
        torqueX = torqueY = torqueZ = 0.0f;
    }

    /**
     * 角度归一化
     */
    private float normalizeAngle(float angle) {
        final float TWO_PI = (float) (2 * Math.PI);
        angle = angle % TWO_PI;
        if (angle < 0) angle += TWO_PI;
        return angle;
    }

    /**
     * 设置角速度（用于外部控制）
     */
    public void setAngularVelocity(float omegaX, float omegaY, float omegaZ) {
        this.omegaX = omegaX;
        this.omegaY = omegaY;
        this.omegaZ = omegaZ;
    }

    /**
     * 设置旋转角度（用于同步）
     */
    public void setRotation(float rx, float ry, float rz) {
        this.rotationX = rx;
        this.rotationY = ry;
        this.rotationZ = rz;
    }

    /**
     * 重置旋转状态
     */
    public void reset() {
        omegaX = omegaY = omegaZ = 0.0f;
        alphaX = alphaY = alphaZ = 0.0f;
        torqueX = torqueY = torqueZ = 0.0f;
        rotationX = rotationY = rotationZ = 0.0f;
    }

    /**
     * 获取旋转信息字符串
     */
    public String getInfo() {
        return String.format(
                "ω: (%.2f, %.2f, %.2f) rad/s | L: %.4f kg·m²/s | E: %.4f J",
                omegaX, omegaY, omegaZ,
                getAngularMomentumMagnitude(),
                getRotationalKineticEnergy()
        );
    }

    // ==================== NBT 序列化 ====================

    public static final IAttachmentSerializer<CompoundTag, RotationalPhysics> SERIALIZER =
            new IAttachmentSerializer<>() {
                @Override
                public RotationalPhysics read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                    RotationalPhysics rp = new RotationalPhysics(
                            tag.getFloat("Ixx"), tag.getFloat("Iyy"), tag.getFloat("Izz"),
                            tag.getFloat("omegaX"), tag.getFloat("omegaY"), tag.getFloat("omegaZ"),
                            tag.getFloat("damping"),
                            tag.getFloat("mass"),
                            tag.getBoolean("enabled")
                    );
                    rp.alphaX = tag.getFloat("alphaX");
                    rp.alphaY = tag.getFloat("alphaY");
                    rp.alphaZ = tag.getFloat("alphaZ");
                    rp.rotationX = tag.getFloat("rotX");
                    rp.rotationY = tag.getFloat("rotY");
                    rp.rotationZ = tag.getFloat("rotZ");
                    return rp;
                }

                @Override
                public CompoundTag write(RotationalPhysics rp, HolderLookup.Provider provider) {
                    CompoundTag tag = new CompoundTag();
                    tag.putFloat("Ixx", rp.Ixx);
                    tag.putFloat("Iyy", rp.Iyy);
                    tag.putFloat("Izz", rp.Izz);
                    tag.putFloat("omegaX", rp.omegaX);
                    tag.putFloat("omegaY", rp.omegaY);
                    tag.putFloat("omegaZ", rp.omegaZ);
                    tag.putFloat("alphaX", rp.alphaX);
                    tag.putFloat("alphaY", rp.alphaY);
                    tag.putFloat("alphaZ", rp.alphaZ);
                    tag.putFloat("damping", rp.angularDamping);
                    tag.putFloat("mass", rp.mass);
                    tag.putBoolean("enabled", rp.enabled);
                    tag.putFloat("rotX", rp.rotationX);
                    tag.putFloat("rotY", rp.rotationY);
                    tag.putFloat("rotZ", rp.rotationZ);
                    return tag;
                }
            };
}
