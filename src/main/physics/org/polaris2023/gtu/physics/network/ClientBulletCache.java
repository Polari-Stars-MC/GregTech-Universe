package org.polaris2023.gtu.physics.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.network.BulletBodySyncPacket.BulletBodyData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端 Bullet 刚体数据缓存
 * <p>
 * 存储从服务器同步过来的刚体数据，用于调试渲染
 */
public class ClientBulletCache {

    private static final Map<Integer, BulletBodyData> cache = new ConcurrentHashMap<>();
    private static long lastUpdateTime = 0;

    /**
     * 更新缓存（从网络包接收）
     */
    public static void update(BulletBodySyncPacket packet) {
        cache.clear();
        for (BulletBodyData data : packet.bodies()) {
            cache.put(data.entityId(), data);
        }
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 获取指定实体的刚体数据
     */
    public static BulletBodyData get(int entityId) {
        return cache.get(entityId);
    }

    /**
     * 获取所有缓存的刚体数据
     */
    public static Map<Integer, BulletBodyData> getAll() {
        return cache;
    }

    /**
     * 清空缓存
     */
    public static void clear() {
        cache.clear();
    }

    /**
     * 缓存是否有效（5秒内更新过）
     */
    public static boolean isValid() {
        return System.currentTimeMillis() - lastUpdateTime < 5000;
    }

    /**
     * 获取相对于相机的位置
     */
    public static Vec3 getRelativePos(int entityId, Vec3 cameraPos) {
        BulletBodyData data = cache.get(entityId);
        if (data == null) return null;
        return new Vec3(data.x() - cameraPos.x, data.y() - cameraPos.y, data.z() - cameraPos.z);
    }
}
