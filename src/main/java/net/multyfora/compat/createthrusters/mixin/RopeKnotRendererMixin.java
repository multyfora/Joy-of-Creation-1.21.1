package net.multyfora.compat.createthrusters.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rieno.gadgetsandgizmos.content.RopeKnotBlockEntity;
import com.rieno.gadgetsandgizmos.neoforge.client.RopeKnotRenderer;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;

import net.minecraft.client.renderer.MultiBufferSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The base RopeKnotRenderer only draws the knot block itself, so any rope the
 * knot owns (e.g. when a rope is created FROM the knot) is never rendered.
 * Mirrors RopeConnectorRenderer: render the owned strand after the block.
 **/
@Mixin(value = RopeKnotRenderer.class, remap = false)
public abstract class RopeKnotRendererMixin {

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void joc$renderOwnedStrand(
            RopeKnotBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay, CallbackInfo ci
    ) {
        RopeStrandHolderBehavior holder = be.getRopeHolder();
        if (holder == null || !holder.ownsRope()) return;
        RopeStrandRenderer.render(be, holder, partialTicks, ms, buffer);
    }
}