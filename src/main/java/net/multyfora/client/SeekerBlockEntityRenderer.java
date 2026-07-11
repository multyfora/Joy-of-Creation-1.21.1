package net.multyfora.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.multyfora.content.seeker.SeekerBlockEntity;

public class SeekerBlockEntityRenderer extends SafeBlockEntityRenderer<SeekerBlockEntity> {

    private static final float SPIN_TOTAL_DEGREES = 720f;
    private static final float MAX_MODULATING_SPEED = 720f;

    public SeekerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    protected void renderSafe(
            SeekerBlockEntity blockEntity, float partialTick,
            PoseStack ms, MultiBufferSource buffer,
            int light, int overlay
    ) {
        float insertProgress = blockEntity.getInsertAnimationProgress(partialTick);

        switch (blockEntity.getModule()) {
            case SPYGLASS -> renderSpyglass(blockEntity, partialTick, insertProgress, ms, buffer, light);
            case PLAYER_DIR -> renderEyeOfEnder(blockEntity, partialTick, insertProgress, ms, buffer, light);
            case MODULATING -> renderModulating(blockEntity, partialTick, insertProgress, ms, buffer, light);
            case NONE -> {}
        }
    }

    private void renderSpyglass(
            SeekerBlockEntity blockEntity, float partialTick, float insertProgress,
            PoseStack ms, MultiBufferSource buffer, int light
    ) {
        float pitch = blockEntity.spyglassPointer.lerpedYawDegrees.getValue(partialTick) + 180.0f;
        float yaw   = blockEntity.spyglassPointer.lerpedPitchDegrees.getValue(partialTick);

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        applyInsertAnimation(ms, insertProgress);
        ms.mulPose(Axis.YP.rotationDegrees(pitch));
        ms.mulPose(Axis.XP.rotationDegrees(-yaw));
        ms.translate(-0.5, -0.5, -0.5);

        SuperByteBuffer superBuffer = CachedBuffers.partial(
                SeekerPartialModels.SEEKER_SPYGLASS,
                blockEntity.getBlockState()
        );
        superBuffer
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }

    private void renderEyeOfEnder(
            SeekerBlockEntity blockEntity, float partialTick, float insertProgress,
            PoseStack ms, MultiBufferSource buffer, int light
    ) {
        float yaw   = blockEntity.spyglassPointer.lerpedYawDegrees.getValue(partialTick);
        float pitch = blockEntity.spyglassPointer.lerpedPitchDegrees.getValue(partialTick);

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        applyInsertAnimation(ms, insertProgress);
        ms.mulPose(Axis.YP.rotationDegrees(yaw));
        ms.mulPose(Axis.XP.rotationDegrees(pitch));
        ms.translate(-0.5, -0.5, -0.5);

        SuperByteBuffer superBuffer = CachedBuffers.partial(
                SeekerPartialModels.SEEKER_EYE_OF_ENDER,
                blockEntity.getBlockState()
        );
        superBuffer
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.translucent()));
        ms.popPose();
    }

    private void renderModulating(
            SeekerBlockEntity blockEntity, float partialTick, float insertProgress,
            PoseStack ms, MultiBufferSource buffer, int light
    ) {
        float rotationSpeed = 0;
        double dist = blockEntity.getDistanceToTarget();
        if (dist >= 0) {
            int minDist = blockEntity.getMinDistance();
            int maxDist = blockEntity.getMaxDistance();
            if (maxDist > minDist) {
                float t = (float) Math.max(0, Math.min(1, (dist - minDist) / (double)(maxDist - minDist)));
                rotationSpeed = (1 - t) * MAX_MODULATING_SPEED;
            } else {
                rotationSpeed = MAX_MODULATING_SPEED;
            }
        }
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        float rotationAngle = ((gameTime + partialTick) * rotationSpeed) % 360f;

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        applyInsertAnimation(ms, insertProgress);
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationAngle));
        ms.scale(0.25f, 0.25f, 0.25f);
        ms.translate(-0.5, -0.5, -0.5);

        SuperByteBuffer superBuffer = CachedBuffers.partial(
                SeekerPartialModels.SEEKER_MODULATING,
                blockEntity.getBlockState()
        );
        superBuffer
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }

    private void applyInsertAnimation(PoseStack ms, float progress) {
        if (progress >= 1f) return;

        float scale = easeOutBack(progress);
        float remainingSpin = SPIN_TOTAL_DEGREES * (1f - easeOutCubic(progress));

        ms.mulPose(Axis.YP.rotationDegrees(remainingSpin));
        ms.scale(scale, scale, scale);
    }

    private static float easeOutCubic(float t) {
        float f = t - 1f;
        return f * f * f + 1f;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float f = t - 1f;
        return 1f + c3 * f * f * f + c1 * f * f;
    }
}
