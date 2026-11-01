package net.multyfora.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;

import net.multyfora.IMultiRopeBehavior;
import net.multyfora.client.JocPartialModels;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RopeStrandRenderer.class, remap = false)
public abstract class RopeStrandRendererMixin {

    @ModifyExpressionValue(
            method = "render",
            at = @At(value = "FIELD", target = "Ldev/simulated_team/simulated/index/SimPartialModels;ROPE:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;")
    )
    private static PartialModel joc$chainSegment(PartialModel model, SmartBlockEntity be, RopeStrandHolderBehavior ropeHolder) {
        return joc$isChain(ropeHolder) ? JocPartialModels.CHAIN_ROPE : model;
    }

    @ModifyExpressionValue(
            method = "render",
            at = @At(value = "FIELD", target = "Ldev/simulated_team/simulated/index/SimPartialModels;ROPE_KNOT:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;")
    )
    private static PartialModel joc$chainKnot(PartialModel model, SmartBlockEntity be, RopeStrandHolderBehavior ropeHolder) {
        return joc$isChain(ropeHolder) ? JocPartialModels.CHAIN_ROPE_KNOT : model;
    }

    @Unique
    private static boolean joc$isChain(RopeStrandHolderBehavior ropeHolder) {
        ClientRopeStrand strand = ropeHolder.getClientStrand();
        return strand != null
                && ropeHolder instanceof IMultiRopeBehavior multi
                && multi.joc$isChainRope(strand.getUuid());
    }
}
