package org.polaris2023.gtu.modpacks;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModpacksNetwork {
    private ModpacksNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                CopperToolVeinMiningStatePayload.TYPE,
                CopperToolVeinMiningStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        CopperToolVeinMining.setEnabled(context.player(), payload.enabled()))
        );
    }
}
