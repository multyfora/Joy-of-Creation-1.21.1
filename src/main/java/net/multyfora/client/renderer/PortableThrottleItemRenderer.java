package net.multyfora.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.client.JocPartialModels;
import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;
import net.multyfora.client.portable_throttle.PortableThrottleStrengthScreen;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import dev.engine_room.flywheel.lib.transform.TransformStack;

public class PortableThrottleItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final float KNOB_REST_Z = 0f;
    private static final float KNOB_MAX_Z = 10f;

    public PortableThrottleItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int light, int overlay) {
        float knobProgress = getKnobProgress(stack, displayContext);

        boolean isPitchedView = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        float effectiveProgress = isPitchedView ? 1.0f - knobProgress : knobProgress;

        boolean isFirstPersonRight = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        boolean isFirstPersonLeft = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        boolean isFirstPerson = isFirstPersonRight || isFirstPersonLeft;

        if (isFirstPerson) {
            applyFirstPersonTransforms(poseStack, isFirstPersonRight);
        }

        BlockState dummyState = Blocks.AIR.defaultBlockState();
        SuperByteBuffer bodyBuffer = CachedBuffers.partial(JocPartialModels.THROTTLE_BODY, dummyState);
        if (bodyBuffer != null) {
            bodyBuffer.light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
        }

        poseStack.pushPose();
        float zOffset = Mth.lerp(effectiveProgress, KNOB_REST_Z, KNOB_MAX_Z) / 16f;
        poseStack.translate(0, 0, zOffset);
        
        SuperByteBuffer knobBuffer = CachedBuffers.partial(JocPartialModels.THROTTLE_KNOB, dummyState);
        if (knobBuffer != null) {
            knobBuffer.light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
        }
        poseStack.popPose();
    }

    private void applyFirstPersonTransforms(PoseStack poseStack, boolean isRightHand) {
        float flip = isRightHand ? 1.0f : -1.0f;
        float gripProgress = PortableThrottleClientHandler.getGripProgress();
        
        float equipProgress = 1.0f;
        float equipAnim = Mth.sin(equipProgress * (float) Math.PI);
        float equipAnim2 = Mth.sin(Mth.sqrt(equipProgress) * (float) Math.PI);
        
        poseStack.translate(0, -equipAnim * 0.5f, equipAnim * 0.3f);
        poseStack.translate(0, -equipAnim2 * 0.4f, equipAnim2 * 0.2f);

        double x = Mth.lerp(gripProgress, flip * -0.15f, flip * -0.30f);
        double y = Mth.lerp(gripProgress, -0.05f, -0.02f);
        double z = Mth.lerp(gripProgress, 0.12f, 0.06f);
        
        poseStack.translate(x, y, z);
        
        poseStack.scale(0.8f, 0.8f, 0.8f);
    }

    private float getKnobProgress(ItemStack stack, ItemDisplayContext context) {
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND &&
            context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND &&
            context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND &&
            context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return PortableThrottleClientHandler.getLastStrength() / 15f;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return PortableThrottleClientHandler.getLastStrength() / 15f;
        }

        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
            context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            if (mc.screen instanceof PortableThrottleStrengthScreen) {
                return PortableThrottleClientHandler.getLiveValue();
            }
            return PortableThrottleClientHandler.getSmoothedStrength(mc.player.getUUID());
        }

        return PortableThrottleClientHandler.getLastStrength() / 15f;
    }
}