package net.multyfora.compat.createthrusters.mixin;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.IMultiRopeBehavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(targets = "com.rieno.gadgetsandgizmos.content.PoweredZiplineBlockEntity", remap = false)
public abstract class PoweredZiplineRopeConnectorMixin {

    @Inject(
            method = "isHangingRopeHolderFree",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void joc$allowJocConnectors(RopeStrandHolderBehavior behavior, CallbackInfoReturnable<Boolean> cir) {
        if (behavior.blockEntity == (Object) this) {
            return;
        }
        if (behavior instanceof IMultiRopeBehavior multi
                && multi.joc$getMaxRopeAttachments() > 1
                && multi.joc$canAcceptAnotherRope()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "isFreeConnector",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void joc$allowJocConnectors(RopeConnectorBlockEntity connector, CallbackInfoReturnable<Boolean> cir) {
        RopeStrandHolderBehavior holder = connector.getRopeHolder();
        if (holder instanceof IMultiRopeBehavior multi
                && multi.joc$getMaxRopeAttachments() > 1
                && multi.joc$canAcceptAnotherRope()) {
            AeronauticsJoyofcreation.LOGGER.debug("[JOC-ZIP] isFreeConnector {} -> true (multi max={} used={})",
                    connector.getBlockPos(), multi.joc$getMaxRopeAttachments(), multi.joc$getAllAttachedRopeIDs().size());
            cir.setReturnValue(true);
            return;
        }
        AeronauticsJoyofcreation.LOGGER.debug("[JOC-ZIP] isFreeConnector {} -> base (holder={})",
                connector.getBlockPos(), holder == null ? "null" : holder.getClass().getSimpleName());
    }

    @Inject(
            method = "attachToRope",
            at = @At("RETURN"),
            remap = false
    )
    private void joc$logAttachToRope(UUID uuid, float position, CallbackInfoReturnable<Boolean> cir) {
        AeronauticsJoyofcreation.LOGGER.debug("[JOC-ZIP] attachToRope uuid={} -> {}", uuid, cir.getReturnValue());
    }

    @Inject(
            method = "tryCreateRopeFromSimulated",
            at = @At("RETURN"),
            remap = false
    )
    private void joc$logTryCreateRopeFromSimulated(RopeStrandHolderBehavior a, RopeStrandHolderBehavior b, CallbackInfoReturnable<Boolean> cir) {
        AeronauticsJoyofcreation.LOGGER.debug("[JOC-ZIP] tryCreateRopeFromSimulated a={} b={} -> {}",
                a != null ? a.blockEntity.getBlockPos() : "null",
                b != null ? b.blockEntity.getBlockPos() : "null",
                cir.getReturnValue());
    }
}