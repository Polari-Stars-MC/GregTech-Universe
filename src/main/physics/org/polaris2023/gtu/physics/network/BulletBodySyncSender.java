package org.polaris2023.gtu.physics.network;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.polaris2023.gtu.physics.world.PhysicsWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 服务器端 Bullet 刚体数据同步发送器
 */
public class BulletBodySyncSender {

    /**
     * 同步刚体数据到所有客户端
     */
    public static void syncToClients(ServerLevel level) {
        PhysicsWorld physicsWorld = org.polaris2023.gtu.physics.world.PhysicsManager.getOrCreatePhysicsWorld(level);
        if (physicsWorld == null || physicsWorld.isDestroyed()) return;

        Map<Integer, PhysicsRigidBody> bodies = physicsWorld.getEntityBodies();
        if (bodies.isEmpty()) return;

        List<BulletBodySyncPacket.BulletBodyData> dataList = new ArrayList<>();

        for (Map.Entry<Integer, PhysicsRigidBody> entry : bodies.entrySet()) {
            int entityId = entry.getKey();
            PhysicsRigidBody body = entry.getValue();

            if (body.getMass() <= 0) continue;  // 跳过静态体

            Vector3f pos = body.getPhysicsLocation(null);
            Vector3f vel = body.getLinearVelocity(null);
            CollisionShape shape = body.getCollisionShape();

            byte shapeType;
            float param1, param2, param3;

            if (shape instanceof CapsuleCollisionShape capsule) {
                shapeType = 1;
                param1 = capsule.getRadius();
                param2 = capsule.getHeight();
                param3 = 0;
            } else if (shape instanceof BoxCollisionShape box) {
                shapeType = 0;
                Vector3f halfExtents = box.getHalfExtents(null);
                param1 = halfExtents.x;
                param2 = halfExtents.y;
                param3 = halfExtents.z;
            } else {
                // 其他形状当作盒子处理
                shapeType = 0;
                var bb = level.getEntity(entityId);
                if (bb != null) {
                    param1 = (float) bb.getBbWidth() / 2;
                    param2 = (float) bb.getBbHeight() / 2;
                    param3 = (float) bb.getBbWidth() / 2;
                } else {
                    param1 = param2 = param3 = 0.3f;
                }
            }

            dataList.add(new BulletBodySyncPacket.BulletBodyData(
                    entityId,
                    pos.x, pos.y, pos.z,
                    vel.x, vel.y, vel.z,
                    shapeType, param1, param2, param3
            ));
        }

        if (dataList.isEmpty()) return;

        BulletBodySyncPacket packet = new BulletBodySyncPacket(dataList);

        // 发送给所有在当前世界的玩家
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }
}
