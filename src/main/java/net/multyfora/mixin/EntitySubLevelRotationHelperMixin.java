package net.multyfora.mixin;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.world.entity.Entity;

import net.multyfora.content.gyroseat.GyroscopicSeatEntity;

import org.joml.Quaterniond;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(value = EntitySubLevelRotationHelper.class, remap = false)
public class EntitySubLevelRotationHelperMixin {

    @Inject(
        method = "getSubLevelInheritedOrientation",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void joc$suppressGyroscopicRotation(
        Entity cameraEntity,
        Function<SubLevel, Pose3dc> poseProvider,
        EntitySubLevelRotationHelper.Type type,
        CallbackInfoReturnable<Quaterniond> cir
    ) {
        Entity current = cameraEntity;
        while (current != null) {
            if (current instanceof GyroscopicSeatEntity) {
                cir.setReturnValue(null);
                return;
            }
            current = current.getVehicle();
        }
    }
}
