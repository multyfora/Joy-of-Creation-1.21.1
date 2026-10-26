package net.multyfora.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.client.JocPartialModels;
import net.multyfora.client.RenderingPlayer;
import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;
import net.multyfora.client.portable_throttle.ThrottleVisualState;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;

public class PortableThrottleItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final float KNOB_REST_Z = 0f;
    private static final float KNOB_MAX_Z = 8f;

    public PortableThrottleItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int light, int overlay) {
        boolean inHand = displayContext.firstPerson()
            || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        Player holder = getHolder(displayContext);
        float knobProgress = holder != null ? ThrottleVisualState.getKnob(holder) : PortableThrottleClientHandler.getLastStrength() / 15f;
        int strength = holder != null ? ThrottleVisualState.getStrength(holder) : PortableThrottleClientHandler.getLastStrength();
        renderModel(poseStack, bufferSource, light, knobProgress, inHand, strength > 0);
    }

    public static void renderModel(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                   float knobProgress, boolean inHand, boolean powered) {
        BlockState dummyState = Blocks.AIR.defaultBlockState();
        SuperByteBuffer bodyBuffer = CachedBuffers.partial(powered ? JocPartialModels.THROTTLE_BODY_POWERED : JocPartialModels.THROTTLE_BODY, dummyState);
        bodyBuffer.light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.translucent()));

        poseStack.pushPose();
        poseStack.translate(0, 0, getKnobZ(knobProgress, inHand) / 16f);
        SuperByteBuffer knobBuffer = CachedBuffers.partial(JocPartialModels.THROTTLE_KNOB, dummyState);
        knobBuffer.light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.translucent()));
        poseStack.popPose();
    }

    public static float getKnobZ(float knobProgress, boolean inHand) {
        float effectiveProgress = inHand ? 1.0f - knobProgress : knobProgress;
        return Mth.lerp(effectiveProgress, KNOB_REST_Z, KNOB_MAX_Z);
    }

    private static Player getHolder(ItemDisplayContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (context.firstPerson()) return mc.player;
        boolean thirdPerson = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        return thirdPerson ? RenderingPlayer.get() : null;
    }
}
