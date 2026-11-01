package net.multyfora.client.balloon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.multyfora.content.balloon.DeflatedBalloonItem;
import net.multyfora.index.JocBlocks;

public final class BalloonInflation {

    private static final float RAISE_END = 8f / DeflatedBalloonItem.USE_DURATION;
    private static final float BLOW_START = (float) DeflatedBalloonItem.BLOW_SOUND_TICK / DeflatedBalloonItem.USE_DURATION;
    private static final float START_SIZE = 0.14f;
    private static final float FULL_SIZE = 0.8f;
    private static final float WOBBLE = 0.05f;
    private static final float WOBBLE_RADIANS_PER_TICK = 1.15f;

    private BalloonInflation() {}

    public static float progress(LivingEntity entity, float partialTick) {
        if (!entity.isUsingItem() || !(entity.getUseItem().getItem() instanceof DeflatedBalloonItem))
            return -1f;
        float ticks = entity.getTicksUsingItem() + partialTick;
        return Mth.clamp(ticks / DeflatedBalloonItem.USE_DURATION, 0f, 1f);
    }

    public static float raise(float progress) {
        return smoothstep(Mth.clamp(progress / RAISE_END, 0f, 1f));
    }

    public static void renderBlock(PoseStack ms, MultiBufferSource buffers, int light, DyeColor color, float progress) {
        float fill = smoothstep(Mth.clamp((progress - BLOW_START) / (1f - BLOW_START), 0f, 1f));
        float size = Mth.lerp(fill, START_SIZE, FULL_SIZE);
        float wobble = fill * WOBBLE * Mth.sin(progress * DeflatedBalloonItem.USE_DURATION * WOBBLE_RADIANS_PER_TICK);

        ms.pushPose();
        ms.scale(size * (1f + wobble), size * (1f + wobble), size * (1f - wobble));
        ms.translate(-0.5f, -0.5f, -1f);
        CachedBuffers.block(JocBlocks.BALLOONS.get(color).get().defaultBlockState())
                .light(light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));
        ms.popPose();
    }

    private static float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }
}
