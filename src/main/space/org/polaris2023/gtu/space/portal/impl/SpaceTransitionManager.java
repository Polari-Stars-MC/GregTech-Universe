package org.polaris2023.gtu.space.portal.impl;

import net.minecraft.server.level.ServerPlayer;
import org.polaris2023.gtu.space.portal.ITransitionManager;
import org.polaris2023.gtu.space.portal.Portal;
import org.polaris2023.gtu.space.portal.PortalType;
import org.polaris2023.gtu.space.portal.TransitionPhase;
import org.polaris2023.gtu.space.portal.TransitionState;
import org.polaris2023.gtu.space.portal.ValidationResult;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpaceTransitionManager implements ITransitionManager {
    private final ConcurrentHashMap<UUID, TransitionState> transitions = new ConcurrentHashMap<>();
    private final SpacePortalManager portalManager;
    private long serverTick;

    public SpaceTransitionManager(SpacePortalManager portalManager) {
        this.portalManager = portalManager;
    }

    public void setServerTick(long tick) {
        this.serverTick = tick;
    }

    @Override
    public TransitionState beginTransition(ServerPlayer player, Portal portal) {
        ValidationResult validation = validateTransition(player, portal);
        if (validation.isFailure()) {
            return null;
        }

        UUID transitionId = UUID.randomUUID();
        long estimatedDuration = estimateDuration(portal.type());
        TransitionState state = new TransitionState(
                transitionId,
                player.getUUID(),
                portal.id(),
                TransitionPhase.INITIATED,
                serverTick,
                serverTick + estimatedDuration,
                0.0
        );
        transitions.put(player.getUUID(), state);
        return state;
    }

    @Override
    public void completeTransition(ServerPlayer player) {
        TransitionState state = transitions.get(player.getUUID());
        if (state == null) {
            return;
        }
        transitions.put(player.getUUID(), state.withPhase(TransitionPhase.COMPLETED).withProgress(1.0));
        transitions.remove(player.getUUID());
    }

    @Override
    public void cancelTransition(ServerPlayer player, String reason) {
        TransitionState state = transitions.remove(player.getUUID());
        if (state != null) {
            // Portal cleanup handled by caller
        }
    }

    @Override
    public TransitionState getTransitionState(UUID playerId) {
        return transitions.get(playerId);
    }

    @Override
    public ValidationResult validateTransition(ServerPlayer player, Portal portal) {
        if (player == null) {
            return ValidationResult.failure("NULL_PLAYER", "Player is null");
        }
        if (portal == null) {
            return ValidationResult.failure("NULL_PORTAL", "Portal is null");
        }
        if (transitions.containsKey(player.getUUID())) {
            return ValidationResult.failure("ALREADY_TRANSITIONING", "Player is already in a transition");
        }
        return ValidationResult.success();
    }

    public void updateProgress(UUID playerId, double progress) {
        TransitionState state = transitions.get(playerId);
        if (state == null || state.isComplete() || state.isCancelled()) {
            return;
        }
        TransitionPhase phase;
        if (progress < 0.2) {
            phase = TransitionPhase.PREPARING;
        } else if (progress < 0.8) {
            phase = TransitionPhase.TRANSFERRING;
        } else {
            phase = TransitionPhase.COMPLETING;
        }
        transitions.put(playerId, state.withPhase(phase).withProgress(progress));
    }

    public void clear() {
        transitions.clear();
    }

    private static long estimateDuration(PortalType type) {
        return switch (type) {
            case SURFACE_TO_SPACE -> 200L;
            case SPACE_TO_SURFACE -> 160L;
            case INTER_DIMENSIONAL -> 100L;
        };
    }
}
