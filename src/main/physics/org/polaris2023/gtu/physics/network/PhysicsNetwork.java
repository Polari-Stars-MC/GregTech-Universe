package org.polaris2023.gtu.physics.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.polaris2023.gtu.physics.GregtechUniversePhysics;

import java.net.CacheRequest;

/**
 * 网络包注册
 */
@EventBusSubscriber(modid = GregtechUniversePhysics.MOD_ID)
public class PhysicsNetwork {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                BulletBodySyncPacket.TYPE,
                BulletBodySyncPacket.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        ClientBulletCache.update(packet);
                    });
                }
        );
    }
}
