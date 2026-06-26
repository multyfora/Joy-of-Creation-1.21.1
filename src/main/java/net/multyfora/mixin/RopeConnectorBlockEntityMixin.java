package net.multyfora.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;

import net.multyfora.IMultiRopeBehavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin to RopeConnectorBlockEntity: sets the maximum rope attachments to unlimited
 * so that rope connectors can accept multiple simultaneous rope connections.
 **/
@Mixin(value = RopeConnectorBlockEntity.class, remap = false)
public class RopeConnectorBlockEntityMixin {

    /**
     * After behaviours are added to the connector, find the RopeStrandHolderBehavior
     * and set its max attachments to Integer.MAX_VALUE
     **/
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
}
