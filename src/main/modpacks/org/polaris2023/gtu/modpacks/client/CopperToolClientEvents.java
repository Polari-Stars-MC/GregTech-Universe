package org.polaris2023.gtu.modpacks.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.polaris2023.gtu.modpacks.CopperToolVeinMiningStatePayload;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID, value = Dist.CLIENT)
public final class CopperToolClientEvents {
    private static boolean lastSent;

    private CopperToolClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            lastSent = false;
            return;
        }

        boolean enabled = CopperToolKeyMappings.VEIN_MINE_KEY.get().isDown();
        if (enabled != lastSent) {
            lastSent = enabled;
            PacketDistributor.sendToServer(new CopperToolVeinMiningStatePayload(enabled));
        }
    }
}
