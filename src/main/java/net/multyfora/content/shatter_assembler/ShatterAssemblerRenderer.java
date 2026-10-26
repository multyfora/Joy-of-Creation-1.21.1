package net.multyfora.content.shatter_assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.multyfora.client.JocPartialModels;

public class ShatterAssemblerRenderer extends SmartBlockEntityRenderer<ShatterAssemblerBlockEntity> {
    private static final float SAW_GHOST_STEP_DEGREES = 4.5F;
    private static final int[] SAW_GHOST_ALPHA = {100, 75, 55, 35, 20};

    public ShatterAssemblerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ShatterAssemblerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState blockState = be.getBlockState();
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        SuperByteBuffer handle = CachedBuffers.partial(SimPartialModels.ASSEMBLER_LEVER, blockState);
        float angle = getRenderAngle(be, partialTicks);

        ((SuperByteBuffer)((SuperByteBuffer)this.transform(handle, blockState)
                .translate(0.5F, 0.4375F, 0.5F))
                .rotate(angle, Direction.EAST))
                .translate(-0.5F, -0.4375F, -0.5F);

        handle.light(light).renderInto(ms, vb);

        renderSaws(be, blockState, ms, buffer, vb, light);
    }

    private void renderSaws(ShatterAssemblerBlockEntity be, BlockState blockState, PoseStack ms,
                            MultiBufferSource buffer, VertexConsumer solid, int light) {
        float spin = (AnimationTickHolder.getRenderTime(be.getLevel())
                * ShatterAssemblerBlockEntity.SAW_SPEED_DEGREES_PER_TICK) % 360.0F;

        for (int side = -1; side <= 1; side += 2) {
            for (float z : ShatterAssemblerBlockEntity.SAW_ROW_Z) {
                renderSaw(blockState, ms, solid, light, side, z, spin, 255);
            }
        }

        VertexConsumer translucent = buffer.getBuffer(RenderType.translucent());
        for (int side = -1; side <= 1; side += 2) {
            for (float z : ShatterAssemblerBlockEntity.SAW_ROW_Z) {
                for (int i = 0; i < SAW_GHOST_ALPHA.length; i++) {
                    renderSaw(blockState, ms, translucent, light, side, z,
                            spin - (i + 1) * SAW_GHOST_STEP_DEGREES, SAW_GHOST_ALPHA[i]);
                }
            }
        }
    }

    private void renderSaw(BlockState blockState, PoseStack ms, VertexConsumer consumer, int light,
                           int side, float z, float angleDegrees, int alpha) {
        SuperByteBuffer saw = CachedBuffers.partial(JocPartialModels.SHATTER_SAW, blockState);
        this.transform(saw, blockState);
        saw.translate(ShatterAssemblerBlockEntity.getSawCenterX(side), ShatterAssemblerBlockEntity.SAW_CENTER_Y, z);
        saw.rotate((float) Math.toRadians(90.0F), Direction.UP);
        saw.rotate((float) Math.toRadians(-angleDegrees * side), Direction.EAST);
        saw.scale(ShatterAssemblerBlockEntity.SAW_SCALE);
        saw.translate(-0.5F, -0.5F, -0.5F);
        if (alpha < 255) {
            saw.color(255, 255, 255, alpha);
        }
        saw.light(light).renderInto(ms, consumer);
    }

    public static float getRenderAngle(ShatterAssemblerBlockEntity be, float partialTicks) {
        if (!be.isVirtual()) {
            be.initializeLeverPosition();
        }
        return (float)Math.toRadians((double)be.getClientAngle(partialTicks));
    }

    private SuperByteBuffer transform(SuperByteBuffer buffer, BlockState leverState) {
        AttachFace face = (AttachFace)leverState.getValue(AnalogLeverBlock.FACE);
        float rX = face == AttachFace.FLOOR ? 0.0F : (face == AttachFace.WALL ? 90.0F : 180.0F);
        float rY = AngleHelper.horizontalAngle((Direction)leverState.getValue(AnalogLeverBlock.FACING));
        buffer.rotateCentered((float)((double)(rY / 180.0F) * Math.PI), Direction.UP);
        buffer.rotateCentered((float)((double)(rX / 180.0F) * Math.PI), Direction.EAST);
        return buffer;
    }
}