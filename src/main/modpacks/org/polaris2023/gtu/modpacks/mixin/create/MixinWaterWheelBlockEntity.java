package org.polaris2023.gtu.modpacks.mixin.create;

import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.modpacks.worldgen.river.RiverCurrentSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaterWheelBlockEntity.class)
public abstract class MixinWaterWheelBlockEntity {
    @Inject(method = "getFlowVectorAtPosition", at = @At("RETURN"), cancellable = true)
    private void gtu$useRiverCurrent(BlockPos pos, CallbackInfoReturnable<Vec3> cir) {
        Vec3 original = cir.getReturnValue();
        if (original.lengthSqr() > 1.0E-6) {
            return;
        }

        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() == null) {
            return;
        }

        Vec3 current = RiverCurrentSampler.sampleFlow(self.getLevel(), pos);
        if (current.lengthSqr() > 1.0E-6) {
            cir.setReturnValue(current);
        }
    }
}
