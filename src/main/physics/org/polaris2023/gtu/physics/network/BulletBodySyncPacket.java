package org.polaris2023.gtu.physics.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.GregtechUniversePhysics;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步 Bullet 刚体数据到客户端
 * <p>
 * 包含：实体ID、位置、速度、碰撞形状类型、碰撞形状参数
 */
public record BulletBodySyncPacket(
        List<BulletBodyData> bodies
) implements CustomPacketPayload {

    public static final Type<BulletBodySyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GregtechUniversePhysics.MOD_ID, "bullet_body_sync"));

    /**
     * 单个刚体数据
     */
    public record BulletBodyData(
            int entityId,
            double x, double y, double z,
            double vx, double vy, double vz,
            byte shapeType,  // 0=box, 1=capsule
            float param1,    // box: halfWidth, capsule: radius
            float param2,    // box: halfHeight, capsule: cylinderHeight
            float param3     // box: halfDepth, capsule: unused
    ) {
        public Vec3 position() {
            return new Vec3(x, y, z);
        }

        public Vec3 velocity() {
            return new Vec3(vx, vy, vz);
        }
    }

    public static final StreamCodec<FriendlyByteBuf, BulletBodySyncPacket> STREAM_CODEC =
            StreamCodec.of(
                    BulletBodySyncPacket::encode,
                    BulletBodySyncPacket::decode
            );

    private static void encode(FriendlyByteBuf buf, BulletBodySyncPacket packet) {
        buf.writeVarInt(packet.bodies.size());
        for (BulletBodyData data : packet.bodies) {
            buf.writeVarInt(data.entityId());
            buf.writeDouble(data.x());
            buf.writeDouble(data.y());
            buf.writeDouble(data.z());
            buf.writeDouble(data.vx());
            buf.writeDouble(data.vy());
            buf.writeDouble(data.vz());
            buf.writeByte(data.shapeType());
            buf.writeFloat(data.param1());
            buf.writeFloat(data.param2());
            buf.writeFloat(data.param3());
        }
    }

    private static BulletBodySyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<BulletBodyData> bodies = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int entityId = buf.readVarInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            double vx = buf.readDouble();
            double vy = buf.readDouble();
            double vz = buf.readDouble();
            byte shapeType = buf.readByte();
            float param1 = buf.readFloat();
            float param2 = buf.readFloat();
            float param3 = buf.readFloat();
            bodies.add(new BulletBodyData(entityId, x, y, z, vx, vy, vz, shapeType, param1, param2, param3));
        }
        return new BulletBodySyncPacket(bodies);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
