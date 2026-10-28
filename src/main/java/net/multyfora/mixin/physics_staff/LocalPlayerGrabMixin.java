package net.multyfora.mixin.physics_staff;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.multyfora.content.physics_staff.EntityGrabClientState;

@Mixin(LocalPlayer.class)
public class LocalPlayerGrabMixin {

    private static final double JOC$MAX_STEP = 3.0;
    private static final double JOC$FOLLOW = 0.35;
    private static final long JOC$TIMEOUT_MS = 500;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void joc$followHeldTarget(CallbackInfo ci) {
        if (!net.multyfora.config.JocConfig.ENABLE_CREATIVE_STAFF.get()) return;
        Vec3 target = EntityGrabClientState.grabTargetPos;
        if (target == null || !Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) return;

        if (System.currentTimeMillis() - EntityGrabClientState.lastGrabPacketTime > JOC$TIMEOUT_MS) {
            EntityGrabClientState.grabTargetPos = null;
            return;
        }

        LocalPlayer self = (LocalPlayer) (Object) this;
        Vec3 step = target.subtract(self.position()).scale(JOC$FOLLOW);
        if (step.lengthSqr() > JOC$MAX_STEP * JOC$MAX_STEP) {
            step = step.normalize().scale(JOC$MAX_STEP);
        }
        self.setDeltaMovement(step);
        self.fallDistance = 0;
    }
}
