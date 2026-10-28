package net.multyfora.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.multyfora.IMultiRopeBehavior;
import net.multyfora.config.JocFeatures;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RopeItem.class, remap = false)
public class RopeItemMixin {

    @Inject(
        method = "isValidRopeAttachment",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private static void joc$acceptMultiRope(Level level, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if( cir.getReturnValue() || !JocFeatures.isEnabled(JocFeatures.MULTI_ROPE) ) {
            return;
        }

        if(level.getBlockEntity(blockPos) instanceof SmartBlockEntity smartBlockEntity) {
            RopeStrandHolderBehavior behavior = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
            if(
                behavior instanceof IMultiRopeBehavior multi
                && multi.joc$canAcceptAnotherRope()
            ) {
                cir.setReturnValue(true);
            }
        }
    }
}
