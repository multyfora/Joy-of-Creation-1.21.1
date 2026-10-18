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

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void joc$onSendPositionHead(CallbackInfo ci) {
        if (!net.multyfora.config.JocConfig.ENABLE_CREATIVE_STAFF.get()) return;
        Vec3 target = EntityGrabClientState.grabTargetPos;
        if (target == null || !Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) return;

        if (System.currentTimeMillis() - EntityGrabClientState.lastGrabPacketTime > 5000) {
            EntityGrabClientState.grabTargetPos = null;
            return;
        }

        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.position().closerThan(target, 0.01)) return;

        Vec3 current = self.position();
        Vec3 delta = target.subtract(current);
        double maxStep = 1.0;
        if (delta.lengthSqr() > maxStep * maxStep) {
            delta = delta.normalize().scale(maxStep);
        }
        Vec3 lerped = current.add(delta.scale(0.3));
        self.setPos(lerped.x, lerped.y, lerped.z);
    }
}
