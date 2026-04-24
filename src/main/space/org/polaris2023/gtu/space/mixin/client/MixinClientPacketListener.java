package org.polaris2023.gtu.space.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.entity.player.Abilities;
import org.polaris2023.gtu.space.network.ClientSpaceCache;
import org.polaris2023.gtu.space.network.SpaceSeamlessTeleportPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void gtu_space$prepareSeamlessRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        ClientSpaceCache.prepareSeamlessRespawn(packet.commonPlayerSpawnInfo().dimension().location().toString());
    }

    @Inject(
            method = "handleRespawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ClientHooks;firePlayerRespawn(Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/network/Connection;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void gtu_space$applySeamlessRespawnPose(
            ClientboundRespawnPacket packet,
            CallbackInfo ci,
            @Local(ordinal = 0) LocalPlayer previousPlayer,
            @Local(ordinal = 1) LocalPlayer nextPlayer
    ) {
        if (!ClientSpaceCache.shouldSmoothRespawn()) {
            return;
        }
        SpaceSeamlessTeleportPacket seamlessTeleport = ClientSpaceCache.activeSeamlessTeleport();
        if (seamlessTeleport == null) {
            return;
        }

        nextPlayer.setPos(seamlessTeleport.x(), seamlessTeleport.y(), seamlessTeleport.z());
        nextPlayer.xo = seamlessTeleport.x();
        nextPlayer.yo = seamlessTeleport.y();
        nextPlayer.zo = seamlessTeleport.z();
        nextPlayer.xOld = seamlessTeleport.x();
        nextPlayer.yOld = seamlessTeleport.y();
        nextPlayer.zOld = seamlessTeleport.z();

        nextPlayer.setXRot(seamlessTeleport.xRot());
        nextPlayer.setYRot(seamlessTeleport.yRot());
        nextPlayer.setYHeadRot(seamlessTeleport.yRot());
        nextPlayer.setYBodyRot(seamlessTeleport.yRot());
        nextPlayer.xRotO = seamlessTeleport.xRot();
        nextPlayer.yRotO = seamlessTeleport.yRot();
        nextPlayer.yHeadRotO = seamlessTeleport.yRot();
        nextPlayer.yBodyRotO = seamlessTeleport.yRot();

        nextPlayer.xBob = previousPlayer.xBob;
        nextPlayer.yBob = previousPlayer.yBob;
        nextPlayer.xBobO = previousPlayer.xBobO;
        nextPlayer.yBobO = previousPlayer.yBobO;
        nextPlayer.walkDist = previousPlayer.walkDist;
        nextPlayer.walkDistO = previousPlayer.walkDistO;
        nextPlayer.xCloak = previousPlayer.xCloak;
        nextPlayer.yCloak = previousPlayer.yCloak;
        nextPlayer.zCloak = previousPlayer.zCloak;
        nextPlayer.xCloakO = previousPlayer.xCloakO;
        nextPlayer.yCloakO = previousPlayer.yCloakO;
        nextPlayer.zCloakO = previousPlayer.zCloakO;
        nextPlayer.setDeltaMovement(previousPlayer.getDeltaMovement());

        Abilities previousAbilities = previousPlayer.getAbilities();
        Abilities nextAbilities = nextPlayer.getAbilities();
        nextAbilities.flying = previousAbilities.flying;

        Minecraft.getInstance().setCameraEntity(nextPlayer);
        ClientSpaceCache.markSeamlessRespawnApplied();
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void gtu_space$finishSeamlessRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        ClientSpaceCache.finishSeamlessRespawn(packet.commonPlayerSpawnInfo().dimension().location().toString());
    }
}
