package net.multyfora.mixin.physics_staff;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback.Result;
import net.multyfora.config.JocConfig;
import net.multyfora.content.physics_staff.EntityGrabClientState;
import net.multyfora.network.EntityGrabPayloads;
import net.neoforged.neoforge.network.PacketDistributor;

@Mixin(targets = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler$PhysicsStaffMouseHandler")
public class PhysicsStaffMouseHandlerMixin {
    @Inject(
        method = "onUse",
        at = @At("HEAD"),
        cancellable = true
    )
    private void joc$onUse(int modifiers, int action, KeyMapping mapping, CallbackInfoReturnable<Result> cir) {
        if(action != 1 || !JocConfig.ENABLE_CREATIVE_STAFF.get()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if(mapping != client.options.keyUse) {
            return;
        }

        if (EntityGrabClientState.grabbedEntityId != 0) {
            EntityGrabClientState.grabbedEntityId = 0;
            PacketDistributor.sendToServer( new EntityGrabPayloads.Stop() );
            cir.setReturnValue( new Result(true) );
            return;
        }

        if (SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.getDragSession() != null) {
            return;
        }

        Entity target = null;
        Vec3 entityHitPoint = null;
        Entity viewer = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;
        if (viewer == null) {
            return;
        }

        float range = 64.0f;
        Vec3 start = viewer.getEyePosition();
        Vec3 look = viewer.getLookAngle();

        if(client.crosshairPickEntity != null) {
            target = client.crosshairPickEntity;
            entityHitPoint = target.getBoundingBox()
                .clip(start, start.add(look.scale(range)))
                .orElse(target.getBoundingBox().getCenter());
        } else {
            Vec3 end = start.add(look.scale(range));
            AABB aabb = viewer
                .getBoundingBox()
                .expandTowards( look.scale(range) )
                .inflate(1.0, 1.0, 1.0)
            ;

            EntityHitResult result = ProjectileUtil.getEntityHitResult(
                viewer, start, end, aabb,
                    (entity) -> { return !entity.isSpectator() && entity.isPickable(); },
                range * range
            );

            if (result != null) {
                target = result.getEntity();
                entityHitPoint = result.getLocation();
            }
        }

        if (target == null) {
            return;
        }

        HitResult pick = client.player.pick(PhysicsStaffItem.RANGE, 1.0F, false);
        if (pick instanceof BlockHitResult blockHit
            && blockHit.getType() != HitResult.Type.MISS
            && Sable.HELPER.getContainingClient(blockHit.getLocation()) != null
            && blockHit.getLocation().distanceToSqr(start) < entityHitPoint.distanceToSqr(start)) {
            return;
        }

        PacketDistributor.sendToServer(
            new EntityGrabPayloads.GrabRequest( target.getId() )
        );
        cir.setReturnValue( new Result(true) );
    }
}
