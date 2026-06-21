package net.multyfora.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.multyfora.IMultiRopeBehavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to RopeItem (the rope item from Simulated mod): allows the rope item to attach
 * to blocks that implement IMultiRopeBehavior and still have capacity for more ropes,
 * even if the base mod's isValidRopeAttachment returns false for them.
 **/
@Mixin(value = RopeItem.class, remap = false)
public class RopeItemMixin {

    /**
     * At the return of isValidRopeAttachment, if it returned false, check if the target
     * is a multi-rope holder that can accept another rope and override the return value
     **/
    @Inject(method = "isValidRopeAttachment", at = @At("RETURN"), cancellable = true, remap = false)
    private static void joc$acceptMultiRope(Level level, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        if (level.getBlockEntity(blockPos) instanceof SmartBlockEntity smartBlockEntity) {
            RopeStrandHolderBehavior behavior = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
            if (behavior instanceof IMultiRopeBehavior multi && multi.joc$canAcceptAnotherRope()) {
                cir.setReturnValue(true);
            }
        }
    }
}
