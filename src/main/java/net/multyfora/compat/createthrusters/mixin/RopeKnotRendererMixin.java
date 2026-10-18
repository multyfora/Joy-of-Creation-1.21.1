package net.multyfora.compat.createthrusters.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;

import net.minecraft.client.renderer.MultiBufferSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(targets = "com.rieno.gadgetsandgizmos.neoforge.client.RopeKnotRenderer", remap = false)
public abstract class RopeKnotRendererMixin {

    @Unique
    private static volatile Method joc$getRopeHolder;

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void joc$renderOwnedStrand(
            Object be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay, CallbackInfo ci
    ) {
        RopeStrandHolderBehavior holder = joc$resolveHolder(be);
        if (holder == null || !holder.ownsRope()) return;
        if (be instanceof SmartBlockEntity smart) {
            RopeStrandRenderer.render(smart, holder, partialTicks, ms, buffer);
        }
    }

    @Unique
    private static RopeStrandHolderBehavior joc$resolveHolder(Object be) {
        if (be == null) return null;
        try {
            Method m = joc$getRopeHolder;
            if (m == null) {
                m = be.getClass().getMethod("getRopeHolder");
                joc$getRopeHolder = m;
            }
            Object result = m.invoke(be);
            return result instanceof RopeStrandHolderBehavior holder ? holder : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
