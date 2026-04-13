package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.rotation.RotationalPhysics;
import org.polaris2023.gtu.physics.rotation.RotationalPhysicsCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 实体旋转物理 Mixin
 * <p>
 * 在实体更新时应用旋转物理效果
 */
@Mixin(Entity.class)
public class MixinEntityRotation {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        // 只在服务端处理
        if (entity.level().isClientSide()) return;

        // 获取旋转物理状态
        RotationalPhysics rp = entity.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        // 更新旋转物理
        RotationalPhysicsCalculator.updateEntityRotation(entity);
    }
}
