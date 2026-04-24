package org.polaris2023.gtu.space.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.network.ClientSpaceCache;
import org.polaris2023.gtu.space.network.SpaceLandingRequestPacket;
import org.polaris2023.gtu.space.network.SpaceStateSyncPacket;
import org.polaris2023.gtu.space.runtime.SpacePlayerState;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID, value = Dist.CLIENT)
public final class SpaceRenderEvents {
    private static boolean landingKeyState;

    private SpaceRenderEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SpaceRenderRuntime.tickClient();
        pollLandingShortcut();
    }

    private static void pollLandingShortcut() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options == null) {
            landingKeyState = false;
            return;
        }

        boolean shiftDown = minecraft.options.keyShift.isDown();
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        boolean canRequestLanding = state != null
                && state.mode() == SpacePlayerState.Mode.SPACE
                && state.vesselId() != null
                && state.transitionId() == null;

        boolean desiredState = shiftDown && canRequestLanding;
        if (desiredState != landingKeyState) {
            PacketDistributor.sendToServer(new SpaceLandingRequestPacket(desiredState));
            landingKeyState = desiredState;
        }
    }
}
