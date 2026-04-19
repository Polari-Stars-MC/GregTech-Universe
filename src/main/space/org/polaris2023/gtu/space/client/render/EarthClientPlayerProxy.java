package org.polaris2023.gtu.space.client.render;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class EarthClientPlayerProxy {
    private static final int PROXY_ENTITY_ID = Integer.MIN_VALUE + 20230419;

    private RemotePlayer proxy;

    public void ensurePresent(ClientLevel level, LocalPlayer source) {
        if (proxy != null && proxy.level() == level) {
            return;
        }

        discard(level);

        GameProfile profile = new GameProfile(derivedUuid(source.getUUID()), source.getGameProfile().getName());
        profile.getProperties().putAll(source.getGameProfile().getProperties());

        RemotePlayer created = new RemotePlayer(level, profile);
        created.setId(PROXY_ENTITY_ID);
        created.copyPosition(source);
        created.setYRot(source.getYRot());
        created.setXRot(source.getXRot());
        created.setYHeadRot(source.getYHeadRot());
        created.setYBodyRot(source.yBodyRot);
        created.setInvisible(true);
        created.setNoGravity(true);
        created.noPhysics = true;
        level.addEntity(created);
        proxy = created;
    }

    public void syncFrom(LocalPlayer source, EarthClientSeamState state) {
        if (proxy == null) {
            return;
        }

        proxy.setPos(state.wrappedX(), state.sourceY(), state.sourceZ());
        proxy.setOldPosAndRot();
        proxy.xo = state.wrappedX();
        proxy.yo = state.sourceY();
        proxy.zo = state.sourceZ();
        proxy.setYRot(source.getYRot());
        proxy.setXRot(source.getXRot());
        proxy.setYHeadRot(source.getYHeadRot());
        proxy.setYBodyRot(source.yBodyRot);
        proxy.setDeltaMovement(source.getDeltaMovement());
        proxy.setPose(source.getPose());
        proxy.setSprinting(source.isSprinting());
        proxy.setShiftKeyDown(source.isShiftKeyDown());
        proxy.setSwimming(source.isSwimming());
        proxy.setOnGround(source.onGround());
        proxy.walkAnimation.setSpeed(source.walkAnimation.speed());
        proxy.walkAnimation.position(source.walkAnimation.position());
        syncEquipment(source, proxy);
    }

    public void setVisible(boolean visible) {
        if (proxy != null) {
            proxy.setInvisible(!visible);
        }
    }

    public RemotePlayer entity() {
        return proxy;
    }

    public void discard(ClientLevel level) {
        if (proxy == null) {
            return;
        }
        ClientLevel targetLevel = level != null ? level : (ClientLevel) proxy.level();
        if (targetLevel != null) {
            targetLevel.removeEntity(proxy.getId(), Entity.RemovalReason.DISCARDED);
        }
        proxy = null;
    }

    private static void syncEquipment(LocalPlayer source, RemotePlayer proxy) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            proxy.setItemSlot(slot, source.getItemBySlot(slot).copy());
        }
    }

    private static UUID derivedUuid(UUID sourceUuid) {
        return new UUID(sourceUuid.getMostSignificantBits() ^ 0x454152544850524FL, sourceUuid.getLeastSignificantBits() ^ 0x58595F50524F5859L);
    }
}
