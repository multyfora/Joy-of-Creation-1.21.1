package net.multyfora.content.shatter_assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class ShatterAssemblerRenderer extends SmartBlockEntityRenderer<ShatterAssemblerBlockEntity> {
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