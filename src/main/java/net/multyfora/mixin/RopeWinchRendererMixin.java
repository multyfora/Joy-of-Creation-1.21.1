package net.multyfora.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;

import net.multyfora.IMultiRopeBehavior;
import net.multyfora.client.JocPartialModels;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RopeWinchRenderer.class, remap = false)
public abstract class RopeWinchRendererMixin {

    @Shadow protected abstract SpriteShiftEntry getCoilShift();

    @Redirect(
            method = "renderComponents",
            at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;")
    )
    private SuperByteBuffer joc$chainCoil(
            PartialModel model, BlockState state,
            RopeWinchBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay
    ) {
        return CachedBuffers.partial(joc$isChain(be) ? JocPartialModels.CHAIN_ROPE_COIL : model, state);
    }

    @Redirect(
            method = "renderComponents",
            at = @At(value = "INVOKE", target = "Ldev/simulated_team/simulated/content/blocks/rope/rope_winch/RopeWinchRenderer;getCoilShift()Lnet/createmod/catnip/render/SpriteShiftEntry;")
    )
    private SpriteShiftEntry joc$chainCoilShift(
            RopeWinchRenderer renderer,
            RopeWinchBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay
    ) {
        return joc$isChain(be) ? JocPartialModels.CHAIN_ROPE_COIL_SHIFT : getCoilShift();
    }

    @Unique
    private static boolean joc$isChain(RopeWinchBlockEntity be) {
        return be.getRopeHolder() instanceof IMultiRopeBehavior multi
                && multi.joc$getAttachedRopeID() != null
                && multi.joc$isChainRope(multi.joc$getAttachedRopeID());
    }
}
