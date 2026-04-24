package org.polaris2023.gtu.modpacks.mixin;

import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.modpacks.worldgen.river.RiverCurrentSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntityRiverCurrent {
    @Inject(method = "tick", at = @At("RETURN"))
    private void gtu_modpacks$applyRiverCurrent(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide() || !self.isInWater()) {
            return;
        }

        Vec3 current = RiverCurrentSampler.sampleFlow(self.level(), self.blockPosition());
        if (current.lengthSqr() <= 1.0E-6) {
            return;
        }

        double submersion = Mth.clamp(self.getFluidHeight(FluidTags.WATER) * 1.35, 0.2, 1.0);
        double pushScale = self instanceof Boat ? 0.16 : self instanceof LivingEntity ? 0.08 : 0.11;
        Vec3 velocity = self.getDeltaMovement().add(current.scale(pushScale * submersion));

        double maxHorizontalSpeed = self instanceof Boat ? 0.42 : 0.22;
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed > maxHorizontalSpeed) {
            double scale = maxHorizontalSpeed / horizontalSpeed;
            velocity = new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
        }

        self.setDeltaMovement(velocity);
    }
}
