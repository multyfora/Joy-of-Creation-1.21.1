package net.multyfora.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import net.multyfora.IMultiRopeBehavior;
import net.multyfora.client.JocPartialModels;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RopeStrandRenderer.class, remap = false)
public abstract class RopeStrandRendererMixin {

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/createmod/catnip/render/CachedBuffers;partialFacing(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Lnet/createmod/catnip/render/SuperByteBuffer;")
    )
    private static SuperByteBuffer joc$chainSegment(
            PartialModel model, BlockState state, Direction facing,
            SmartBlockEntity be, RopeStrandHolderBehavior ropeHolder, float partialTick, PoseStack ps, MultiBufferSource buffer
    ) {
        return CachedBuffers.partialFacing(joc$isChain(ropeHolder) ? JocPartialModels.CHAIN_ROPE : model, state, facing);
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/createmod/catnip/render/CachedBuffers;partialFacing(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Lnet/createmod/catnip/render/SuperByteBuffer;")
    )
    private static SuperByteBuffer joc$chainKnot(
            PartialModel model, BlockState state, Direction facing,
            SmartBlockEntity be, RopeStrandHolderBehavior ropeHolder, float partialTick, PoseStack ps, MultiBufferSource buffer
    ) {
        return CachedBuffers.partialFacing(joc$isChain(ropeHolder) ? JocPartialModels.CHAIN_ROPE_KNOT : model, state, facing);
    }

    @Unique
    private static boolean joc$isChain(RopeStrandHolderBehavior ropeHolder) {
        ClientRopeStrand strand = ropeHolder.getClientStrand();
        return strand != null
                && ropeHolder instanceof IMultiRopeBehavior multi
                && multi.joc$isChainRope(strand.getUuid());
    }
}
