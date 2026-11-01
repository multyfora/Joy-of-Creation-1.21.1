package net.multyfora.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchRenderer;

import net.createmod.catnip.render.SpriteShiftEntry;

import net.multyfora.IMultiRopeBehavior;
import net.multyfora.client.JocPartialModels;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RopeWinchRenderer.class, remap = false)
public abstract class RopeWinchRendererMixin {

    @ModifyExpressionValue(
            method = "renderComponents",
            at = @At(value = "FIELD", target = "Ldev/simulated_team/simulated/index/SimPartialModels;ROPE_WINCH_ROPE_COIL:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;")
    )
    private PartialModel joc$chainCoil(PartialModel model, RopeWinchBlockEntity be) {
        return joc$isChain(be) ? JocPartialModels.CHAIN_ROPE_COIL : model;
    }

    @ModifyExpressionValue(
            method = "renderComponents",
            at = @At(value = "INVOKE", target = "Ldev/simulated_team/simulated/content/blocks/rope/rope_winch/RopeWinchRenderer;getCoilShift()Lnet/createmod/catnip/render/SpriteShiftEntry;")
    )
    private SpriteShiftEntry joc$chainCoilShift(SpriteShiftEntry shift, RopeWinchBlockEntity be) {
        return joc$isChain(be) ? JocPartialModels.CHAIN_ROPE_COIL_SHIFT : shift;
    }

    @Unique
    private static boolean joc$isChain(RopeWinchBlockEntity be) {
        return be.getRopeHolder() instanceof IMultiRopeBehavior multi
                && multi.joc$getAttachedRopeID() != null
                && multi.joc$isChainRope(multi.joc$getAttachedRopeID());
    }
}
