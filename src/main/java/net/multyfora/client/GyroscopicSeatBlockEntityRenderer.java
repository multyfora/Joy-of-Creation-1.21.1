package net.multyfora.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.multyfora.content.gyroseat.GyroscopicSeatBlock;
import net.multyfora.content.gyroseat.GyroscopicSeatBlockEntity;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GyroscopicSeatBlockEntityRenderer extends SafeBlockEntityRenderer<GyroscopicSeatBlockEntity> {

    public GyroscopicSeatBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(GyroscopicSeatBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Vector3f gimbal = getBallOrientation(be, partialTicks).getEulerAnglesYXZ(new Vector3f());
        int cubeLight = getCubeLight(be, light);
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());

        ms.pushPose();
        ms.translate(1, 1, 1);
        ms.mulPose(Axis.YP.rotation(gimbal.y));
        renderPart(JocPartialModels.GYRO_OUTER_RING, state, ms, vc, cubeLight);
        ms.mulPose(Axis.XP.rotation(gimbal.x));
        renderPart(JocPartialModels.GYRO_INNER_RING, state, ms, vc, cubeLight);
        ms.mulPose(Axis.ZP.rotation(gimbal.z));
        renderPart(JocPartialModels.GYRO_BALL, state, ms, vc, cubeLight);
        ms.popPose();
    }

    private static void renderPart(PartialModel model, BlockState state, PoseStack ms, VertexConsumer vc, int light) {
        CachedBuffers.partial(model, state)
            .translate(-0.5f, -0.5f, -0.5f)
            .light(light)
            .renderInto(ms, vc);
    }

    private static Quaternionf getBallOrientation(GyroscopicSeatBlockEntity be, float partialTicks) {
        Quaternionf world = Axis.YP.rotationDegrees(-getSeatYaw(be, partialTicks));
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(be);
        if (subLevel == null)
            return world;
        return new Quaternionf(subLevel.renderPose(partialTicks).orientation()).conjugate().mul(world);
    }

    private static float getSeatYaw(GyroscopicSeatBlockEntity be, float partialTicks) {
        Entity rider = be.getRider();
        if (rider == null)
            return be.getSeatYaw();
        float yaw = rider instanceof LivingEntity living
            ? Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot)
            : rider.getViewYRot(partialTicks);
        be.setClientYaw(yaw);
        return yaw;
    }

    private static int getCubeLight(GyroscopicSeatBlockEntity be, int light) {
        Level level = be.getLevel();
        if (level == null)
            return light;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int part = 0; part < 8; part++) {
            pos.setWithOffset(be.getBlockPos(), GyroscopicSeatBlock.offsetOf(part));
            light = SuperByteBuffer.maxLight(light, LevelRenderer.getLightColor(level, pos));
        }
        return light;
    }
}
