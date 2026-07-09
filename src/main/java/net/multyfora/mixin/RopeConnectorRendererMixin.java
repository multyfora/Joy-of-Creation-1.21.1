package net.multyfora.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorRenderer;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;

import net.minecraft.client.renderer.MultiBufferSource;

import net.multyfora.IMultiRopeBehavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(value = RopeConnectorRenderer.class, remap = false)
public abstract class RopeConnectorRendererMixin {

    @Inject(method = "renderSafe", at = @At("TAIL"))
    private void joc$renderExtraStrands(
            RopeConnectorBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay, CallbackInfo ci
    ) {
        RopeStrandHolderBehavior holder = be.getRopeHolder();
        if (!(holder instanceof IMultiRopeBehavior multi)) return;

        Map<UUID, ClientRopeStrand> extras = multi.joc$getAllClientStrands();
        if (extras == null || extras.isEmpty()) return;

        ClientRopeStrand owned = multi.joc$getOwnedClientStrand();
        boolean originalOwner = multi.joc$isStrandOwner();
        multi.joc$setStrandOwner(true);
        for (ClientRopeStrand strand : extras.values()) {
            if (strand == owned) continue;
            multi.joc$setOwnedClientStrand(strand);
            RopeStrandRenderer.render(be, holder, partialTicks, ms, buffer);
        }
        multi.joc$setOwnedClientStrand(owned);
        multi.joc$setStrandOwner(originalOwner);
    }
}
