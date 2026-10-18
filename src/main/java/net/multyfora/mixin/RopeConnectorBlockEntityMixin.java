package net.multyfora.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;

import net.minecraft.world.phys.AABB;

import net.multyfora.IMultiRopeBehavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(value = RopeConnectorBlockEntity.class, remap = false)
public class RopeConnectorBlockEntityMixin {

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void joc$setConnectorMultiRope(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        for(BlockEntityBehaviour behaviour : behaviours) {
            if(
                behaviour instanceof RopeStrandHolderBehavior
                && behaviour instanceof IMultiRopeBehavior multi
            ) {
                multi.joc$setMaxRopeAttachments(Integer.MAX_VALUE);
            }
        }
    }

    @Inject(method = "getRenderBoundingBox", at = @At("HEAD"), cancellable = true)
    private void joc$inflateForAllStrands(CallbackInfoReturnable<AABB> cir) {
        RopeStrandHolderBehavior holder = ((RopeConnectorBlockEntity) (Object) this).getRopeHolder();
        if (!(holder instanceof IMultiRopeBehavior multi)) return;

        Map<UUID, ClientRopeStrand> extras = multi.joc$getAllClientStrands();
        if (extras == null || extras.isEmpty()) return;

        AABB bounds = null;
        for (ClientRopeStrand strand : extras.values()) {
            AABB strandBounds = strand.getBounds();
            if (strandBounds == null) continue;
            bounds = bounds == null ? strandBounds : bounds.minmax(strandBounds);
        }
        if (bounds != null) {
            cir.setReturnValue(bounds.inflate(RopeConnectorBlockEntity.RENDER_BOUNDING_BOX_INFLATION));
        }
    }
}