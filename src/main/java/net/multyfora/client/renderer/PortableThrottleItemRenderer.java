package net.multyfora.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import com.mojang.math.Axis;
import org.joml.Vector3f;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.client.JocPartialModels;
import net.multyfora.client.RenderingPlayer;
import net.multyfora.client.portable_throttle.ThrottleVisualState;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;

public class PortableThrottleItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final float KNOB_REST_Z = 0f;
    private static final float KNOB_MAX_Z = 8f;

    private static final Vector3f SCREEN_ORIGIN = new Vector3f(8.0F, 10.0F, 2.95F).div(16.0F);
    private static final float SCREEN_CENTER_ABOVE_ORIGIN = 0.85F / 16.0F;
    private static final float DIGIT_SCALE = 0.028F;
    private static final float DIGIT_HALF_HEIGHT = 3.5F;
    private static final float NIXIE_SHADOW_OFFSET = 0.5F;
    private static final float NIXIE_SHADOW_DEPTH = 0.5F;
    private static final int NIXIE_BRIGHT = 0xFFEE9246;
    private static final int NIXIE_DARK = 0xFFD94A27;
    private static final int NIXIE_SHADOW = 0xFF8D3019;
    private static final RandomSource FLICKER = RandomSource.create();

    public PortableThrottleItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int light, int overlay) {
        boolean inHand = displayContext.firstPerson()
            || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        int strength = PortableThrottleItem.getStrength(stack);
        float knobProgress = strength / 15f;
        Player holder = getHolder(displayContext);
        if (holder != null && ThrottleVisualState.isActiveStack(holder, stack)) {
            knobProgress = ThrottleVisualState.getKnob(holder);
            strength = ThrottleVisualState.getStrength(holder);
        }
        renderModel(poseStack, bufferSource, light, knobProgress, inHand, strength);
    }

    public static void renderModel(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                   float knobProgress, boolean inHand, int strength) {
        boolean powered = strength > 0;
        renderScreenDigits(poseStack, bufferSource, strength);
        BlockState dummyState = Blocks.AIR.defaultBlockState();
        SuperByteBuffer bodyBuffer = CachedBuffers.partial(JocPartialModels.THROTTLE_BODY, dummyState);
        bodyBuffer.light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.translucent()));

        SuperByteBuffer antennaBuffer = CachedBuffers.partial(powered ? JocPartialModels.THROTTLE_ANTENNA_POWERED : JocPartialModels.THROTTLE_ANTENNA, dummyState);
        antennaBuffer.light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.cutout()));

        poseStack.pushPose();
        poseStack.translate(0, 0, getKnobZ(knobProgress, inHand) / 16f);
        SuperByteBuffer knobBuffer = CachedBuffers.partial(JocPartialModels.THROTTLE_KNOB, dummyState);
        knobBuffer.light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.translucent()));
        poseStack.popPose();
    }

    private static void renderScreenDigits(PoseStack poseStack, MultiBufferSource bufferSource, int strength) {
        Font font = Minecraft.getInstance().font;
        String text = String.valueOf(strength);
        float width = font.width(text);
        int bright = FastColor.ARGB32.lerp(FLICKER.nextFloat() / 4.0F, NIXIE_BRIGHT, NIXIE_DARK);

        poseStack.pushPose();
        poseStack.translate(SCREEN_ORIGIN.x, SCREEN_ORIGIN.y, SCREEN_ORIGIN.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
        poseStack.translate(0, SCREEN_CENTER_ABOVE_ORIGIN, 0);
        poseStack.scale(DIGIT_SCALE, -DIGIT_SCALE, DIGIT_SCALE);

        drawNixieLayer(poseStack, bufferSource, font, text, width, bright, NIXIE_DARK);
        poseStack.scale(-1.0F, 1.0F, -1.0F);
        drawNixieLayer(poseStack, bufferSource, font, text, width, NIXIE_DARK, NIXIE_SHADOW);
        poseStack.popPose();

        if (bufferSource instanceof MultiBufferSource.BufferSource batched) {
            batched.endLastBatch();
        }
    }

    private static void drawNixieLayer(PoseStack poseStack, MultiBufferSource bufferSource, Font font, String text,
                                       float width, int color, int shadowColor) {
        float x = -(width - NIXIE_SHADOW_OFFSET) / 2.0F;
        float y = -DIGIT_HALF_HEIGHT;
        Matrix4f pose = poseStack.last().pose();
        font.drawInBatch(text, x, y, color, false, pose, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.pushPose();
        poseStack.translate(NIXIE_SHADOW_OFFSET, NIXIE_SHADOW_OFFSET, -NIXIE_SHADOW_DEPTH);
        font.drawInBatch(text, x, y, shadowColor, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
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
