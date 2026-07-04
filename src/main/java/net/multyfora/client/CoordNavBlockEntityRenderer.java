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
import net.multyfora.content.coordnav.CoordNavBlock;
import net.multyfora.content.coordnav.CoordNavBlockEntity;

public class CoordNavBlockEntityRenderer extends SafeBlockEntityRenderer<CoordNavBlockEntity> {
    public CoordNavBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    protected void renderSafe(
            CoordNavBlockEntity blockEntity, float partialTick,
            PoseStack ms, MultiBufferSource buffer,
            int light, int overlay
    ) {
        switch (blockEntity.getModule()) {
            case SPYGLASS -> renderSpyglass(blockEntity, partialTick, ms, buffer, light);
            case PLAYER_DIR -> renderEyeOfEnder(blockEntity, partialTick, ms, buffer, light);
            case NONE -> {}
        }
    }

    private void renderSpyglass(
            CoordNavBlockEntity blockEntity, float partialTick,
            PoseStack ms, MultiBufferSource buffer, int light
    ) {
        BlockState state  = blockEntity.getBlockState();
        Direction  facing = state.getValue(CoordNavBlock.FACING);
        float      pitch  = blockEntity.spyglassPointer.lerpedYawDegrees.getValue(partialTick) + 180.0f;
        float      yaw    = blockEntity.spyglassPointer.lerpedPitchDegrees.getValue(partialTick);

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
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
                CoordNavPartialModels.COORD_NAV_SPYGLASS,
                blockEntity.getBlockState()
        );
        superBuffer
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }

    private void renderEyeOfEnder(
            CoordNavBlockEntity blockEntity, float partialTick,
            PoseStack ms, MultiBufferSource buffer, int light
    ) {
        BlockState state = blockEntity.getBlockState();
        float yaw   = blockEntity.spyglassPointer.lerpedYawDegrees.getValue(partialTick);
        float pitch = blockEntity.spyglassPointer.lerpedPitchDegrees.getValue(partialTick);

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);

        // Omnidirectional: yaw around Y first, then pitch around the resulting local X axis
        ms.mulPose(Axis.YP.rotationDegrees(yaw));
        ms.mulPose(Axis.XP.rotationDegrees(pitch));

        ms.translate(-0.5, -0.5, -0.5);

        SuperByteBuffer superBuffer = CachedBuffers.partial(
                CoordNavPartialModels.COORD_NAV_EYE_OF_ENDER,
                state
        );
        superBuffer
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }

}