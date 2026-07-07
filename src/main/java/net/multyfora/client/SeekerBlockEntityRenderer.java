package net.multyfora.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.content.seeker.SeekerBlock;
import net.multyfora.content.seeker.SeekerBlockEntity;

public class SeekerBlockEntityRenderer extends SafeBlockEntityRenderer<SeekerBlockEntity> {

    // Total degrees of "extra" spin applied on insertion, easing out to zero over the animation
    private static final float SPIN_TOTAL_DEGREES = 720f;

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
            case NONE -> {}
        }
    }

    private void renderSpyglass(
            SeekerBlockEntity blockEntity, float partialTick, float insertProgress,
            PoseStack ms, MultiBufferSource buffer, int light
    ) {
        BlockState state  = blockEntity.getBlockState();
        Direction  facing = state.getValue(SeekerBlock.FACING);
        float      pitch  = blockEntity.spyglassPointer.lerpedYawDegrees.getValue(partialTick) + 180.0f;
        float      yaw    = blockEntity.spyglassPointer.lerpedPitchDegrees.getValue(partialTick);

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        applyInsertAnimation(ms, insertProgress);
        switch (facing) {
            case NORTH -> ms.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> ms.mulPose(Axis.XP.rotationDegrees( 90));
            case EAST  -> ms.mulPose(Axis.ZP.rotationDegrees(-90));
            case WEST  -> ms.mulPose(Axis.ZP.rotationDegrees( 90));
            case DOWN  -> ms.mulPose(Axis.XP.rotationDegrees(180));
            case UP    -> {}
        }
        ms.mulPose(Axis.YP.rotationDegrees(pitch));
        ms.mulPose(Axis.XP.rotationDegrees(-yaw));
        ms.translate(-0.5, -0.5, -0.5);

        SuperByteBuffer superBuffer = CachedBuffers.partial(
                SeekerPartialModels.SEEKER_SPYGLASS,
                state
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
        BlockState state = blockEntity.getBlockState();
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
                state
        );
        superBuffer
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }

    /**
     * Applies the grow+twirl insertion animation: scale eases in with a slight overshoot
     * bounce (easeOutBack), while an extra spin winds down to zero with easeOutCubic.
     * No-ops entirely once the animation has finished (progress >= 1).
     **/
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