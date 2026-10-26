package net.multyfora.client.portable_typewriter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import java.util.HashSet;
import java.util.Set;

public final class PortableTypewriterHandRenderer {
    private static final float SWAP_POINT = 0.35F;

    private static final float KEYBOARD_SCALE = 0.055F;
    private static final float KEYBOARD_Y = 0.02F;
    private static final float REST_DROP = 0.36F;
    private static final float REST_TILT_DEGREES = -78.0F;
    private static final float HAND_RAISE = 0.2F;
    private static final float HAND_INWARD = 0.1F;
    private static final float HAND_DIP_DEPTH = 0.05F;
    private static final float HAND_DIP_DROP = 0.012F;
    private static final float HAND_DIP_FLEX_DEGREES = 9.0F;

    private PortableTypewriterHandRenderer() {}

    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        InteractionHand typewriterHand = player.getMainHandItem().getItem() instanceof PortableTypewriterItem
            ? InteractionHand.MAIN_HAND
            : player.getOffhandItem().getItem() instanceof PortableTypewriterItem ? InteractionHand.OFF_HAND : null;
        if (typewriterHand == null) return;

        float hold = TypewriterVisualState.getHold();
        if (hold <= 0.0F) return;

        boolean twoHanded = hold > SWAP_POINT;
        if (event.getHand() != typewriterHand) {
            if (twoHanded) event.setCanceled(true);
            return;
        }
        event.setCanceled(true);

        PoseStack ms = event.getPoseStack();
        ms.pushPose();
        if (twoHanded) {
            float raise = TypewriterVisualState.ease((hold - SWAP_POINT) / (1.0F - SWAP_POINT));
            renderTwoHanded(mc, player, event, event.getItemStack(), 1.0F - raise);
        } else {
            float lower = TypewriterVisualState.ease(hold / SWAP_POINT);
            renderOneHanded(player, event, typewriterHand, lower);
        }
        ms.popPose();
    }

    private static void renderOneHanded(LocalPlayer player, RenderHandEvent event, InteractionHand hand, float lower) {
        PoseStack ms = event.getPoseStack();
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean right = arm == HumanoidArm.RIGHT;
        int side = right ? 1 : -1;
        float swing = event.getSwingProgress();

        float swingX = -0.4F * Mth.sin(Mth.sqrt(swing) * Mth.PI);
        float swingY = 0.2F * Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI);
        float swingZ = -0.2F * Mth.sin(swing * Mth.PI);
        ms.translate(side * swingX, swingY, swingZ);

        float equipped = Math.min(1.0F, event.getEquipProgress() + lower);
        ms.translate(side * 0.56F, -0.52F + equipped * -0.6F, -0.72F);

        float swingSq = Mth.sin(swing * swing * Mth.PI);
        ms.mulPose(Axis.YP.rotationDegrees(side * (45.0F + swingSq * -20.0F)));
        float swingRoot = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        ms.mulPose(Axis.ZP.rotationDegrees(side * swingRoot * -20.0F));
        ms.mulPose(Axis.XP.rotationDegrees(swingRoot * -80.0F));
        ms.mulPose(Axis.YP.rotationDegrees(side * -45.0F));

        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
            player, event.getItemStack(),
            right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            !right, ms, event.getMultiBufferSource(), event.getPackedLight());
    }

    private static void renderTwoHanded(Minecraft mc, LocalPlayer player, RenderHandEvent event, ItemStack stack, float equipped) {
        PoseStack ms = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();

        float tilt = calculateTilt(event.getInterpolatedPitch());
        ms.translate(0.0F, 0.04F + equipped * -1.2F + tilt * -REST_DROP, -0.72F);
        ms.mulPose(Axis.XP.rotationDegrees(tilt * REST_TILT_DEGREES));

        if (!player.isInvisible()) {
            renderTypingHand(mc, player, ms, buffers, light, HumanoidArm.RIGHT, TypewriterVisualState.getRightDip());
            renderTypingHand(mc, player, ms, buffers, light, HumanoidArm.LEFT, TypewriterVisualState.getLeftDip());
        }

        ms.pushPose();
        ms.translate(0.0F, KEYBOARD_Y, 0.0F);
        ms.mulPose(Axis.XP.rotationDegrees(90.0F));
        ms.scale(KEYBOARD_SCALE, KEYBOARD_SCALE, KEYBOARD_SCALE);
        ms.translate(0.0F, -TypewriterKeyboardModel.HEIGHT, 0.0F);
        Set<Integer> boundKeys = new HashSet<>(PortableTypewriterItem.getAllBoundKeys(stack));
        TypewriterKeyboardModel.render(ms, buffers, light, OverlayTexture.NO_OVERLAY, boundKeys, true, true);
        ms.popPose();
    }

    private static void renderTypingHand(Minecraft mc, LocalPlayer player, PoseStack ms, MultiBufferSource buffers,
                                         int light, HumanoidArm side, float dip) {
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        float f = side == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        ms.pushPose();
        ms.translate(-f * HAND_INWARD, HAND_RAISE - dip * HAND_DIP_DROP, -dip * HAND_DIP_DEPTH);
        ms.mulPose(Axis.XP.rotationDegrees(-dip * HAND_DIP_FLEX_DEGREES));
        ms.mulPose(Axis.YP.rotationDegrees(90.0F));
        ms.mulPose(Axis.YP.rotationDegrees(92.0F));
        ms.mulPose(Axis.XP.rotationDegrees(45.0F));
        ms.mulPose(Axis.ZP.rotationDegrees(f * -41.0F));
        ms.translate(f * 0.3F, -1.1F, 0.45F);
        if (side == HumanoidArm.RIGHT) {
            renderer.renderRightHand(ms, buffers, light, player);
        } else {
            renderer.renderLeftHand(ms, buffers, light, player);
        }
        ms.popPose();
    }

    private static float calculateTilt(float pitch) {
        float f = 1.0F - pitch / 45.0F + 0.1F;
        f = Mth.clamp(f, 0.0F, 1.0F);
        return -Mth.cos(f * Mth.PI) * 0.5F + 0.5F;
    }
}
