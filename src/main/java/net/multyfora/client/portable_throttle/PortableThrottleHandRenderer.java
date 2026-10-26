package net.multyfora.client.portable_throttle;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.multyfora.client.renderer.PortableThrottleItemRenderer;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PortableThrottleHandRenderer {
    private static final Vector3f HELD_IDLE = new Vector3f(0.46F, -0.22F, -0.90F);
    private static final Vector3f HELD_GRIPPING = new Vector3f(0.10F, -0.12F, -1.05F);
    private static final float TILT_IDLE_DEGREES = 45.0F;
    private static final float TILT_GRIPPING_DEGREES = 25.0F;
    private static final float TURN_IN_DEGREES = 12.0F;
    private static final float SCALE_IDLE = 0.5F;
    private static final float SCALE_GRIPPING = 0.75F;
    private static final Vector3f MODEL_CENTER = new Vector3f(9.5F, 8.0F, 7.0F).div(16.0F);
    private static final Vector3f HOLD_POINT = new Vector3f(9.0F, 7.0F, 13.5F).div(16.0F);
    private static final float HAND_SIDE_X = 11.4F, STRIP_TOP = 9.0F, KNOB_FRONT_Z = 4.0F, HAND_END_BEHIND_KNOB = 1.7F;
    private static final float ARM_HALF_THICKNESS = 2.0F / 16.0F;
    private static final Vector3f HOLD_ARM_DIR = new Vector3f(0.55F, -1.0F, 0.35F).normalize();
    private static final float HOLD_ARM_ROLL = 20.0F;
    private static final Vector3f REACH_ARM_DIR = new Vector3f(0.0F, 0.12F, 1.0F).normalize();
    private static final float REACH_OFFSCREEN = 0.9F;

    private PortableThrottleHandRenderer() {}

    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        InteractionHand throttleHand = PortableThrottleGripHandler.throttleHand(player);
        if (throttleHand == null) return;

        float grip = ThrottleVisualState.getGrip(player);
        if (event.getHand() != throttleHand) {
            if (grip > 0.0F) event.setCanceled(true);
            return;
        }
        event.setCanceled(true);

        HumanoidArm holdingArm = throttleHand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = holdingArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float equip = event.getEquipProgress();

        Vector3f held = new Vector3f(HELD_IDLE).lerp(HELD_GRIPPING, grip);
        held.x *= side;
        held.y -= equip * 0.6F;
        float tilt = Mth.lerp(grip, TILT_IDLE_DEGREES, TILT_GRIPPING_DEGREES);
        Matrix4f device = new Matrix4f()
            .translate(held)
            .rotateY((float) Math.toRadians(-side * TURN_IN_DEGREES))
            .rotateX((float) Math.toRadians(90.0F - tilt))
            .scale(Mth.lerp(grip, SCALE_IDLE, SCALE_GRIPPING))
            .translate(-MODEL_CENTER.x, -MODEL_CENTER.y, -MODEL_CENTER.z);

        PoseStack ms = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float knob = ThrottleVisualState.getKnob(player);

        ms.pushPose();
        ms.mulPose(device);
        PortableThrottleItemRenderer.renderModel(ms, buffers, light, knob, true, ThrottleVisualState.getStrength(player) > 0);
        ms.popPose();

        if (player.isInvisible()) return;

        Vector3f holdPoint = device.transformPosition(new Vector3f(HOLD_POINT));
        renderArmTo(mc, player, ms, buffers, light, holdingArm, holdPoint, mirrored(HOLD_ARM_DIR, side), side * HOLD_ARM_ROLL);

        if (grip > 0.0F) {
            float knobFront = KNOB_FRONT_Z + PortableThrottleItemRenderer.getKnobZ(knob, true);
            Vector3f contact = device.transformPosition(new Vector3f(HAND_SIDE_X, STRIP_TOP, knobFront + HAND_END_BEHIND_KNOB).div(16.0F));
            Vector3f surfaceNormal = device.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
            Vector3f across = device.transformDirection(new Vector3f(1.0F, 0.0F, 0.0F)).normalize();
            Vector3f reachDir = device.transformDirection(new Vector3f(REACH_ARM_DIR)).normalize();

            Vector3f armY = new Vector3f(reachDir).negate();
            Vector3f armZ = new Vector3f(surfaceNormal).fma(-surfaceNormal.dot(armY), armY).normalize();
            Vector3f armX = new Vector3f(armY).cross(armZ);
            Quaternionf orientation = new Quaternionf().setFromNormalized(new Matrix3f(armX, armY, armZ));
            Vector3f handEnd = new Vector3f(contact).fma(ARM_HALF_THICKNESS, armZ).fma(ARM_HALF_THICKNESS, across);
            handEnd.fma((1.0F - grip) * REACH_OFFSCREEN, reachDir);
            renderArm(mc, player, ms, buffers, light, holdingArm.getOpposite(), handEnd, orientation);
        }
    }

    private static void renderArmTo(Minecraft mc, LocalPlayer player, PoseStack ms, MultiBufferSource buffers, int light,
                                    HumanoidArm arm, Vector3f target, Vector3f towardsShoulder, float roll) {
        Quaternionf orientation = new Quaternionf().rotationTo(0.0F, -1.0F, 0.0F, towardsShoulder.x, towardsShoulder.y, towardsShoulder.z)
            .rotateY((float) Math.toRadians(roll));
        renderArm(mc, player, ms, buffers, light, arm, target, orientation);
    }

    private static void renderArm(Minecraft mc, LocalPlayer player, PoseStack ms, MultiBufferSource buffers, int light,
                                  HumanoidArm arm, Vector3f target, Quaternionf orientation) {
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        boolean right = arm == HumanoidArm.RIGHT;
        Vector3f handEnd = new Vector3f((right ? -1 : 1) * (slim ? 5.5F : 6.0F), slim ? 12.5F : 12.0F, 0.0F).div(16.0F);

        ms.pushPose();
        ms.translate(target.x, target.y, target.z);
        ms.mulPose(orientation);
        ms.translate(-handEnd.x, -handEnd.y, -handEnd.z);
        if (right) {
            renderer.renderRightHand(ms, buffers, light, player);
        } else {
            renderer.renderLeftHand(ms, buffers, light, player);
        }
        ms.popPose();
    }

    private static Vector3f mirrored(Vector3f rightHanded, float side) {
        return new Vector3f(rightHanded.x * side, rightHanded.y, rightHanded.z);
    }
}
