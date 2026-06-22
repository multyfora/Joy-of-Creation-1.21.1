package net.multyfora.mixin.physics_staff;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.LineOutline;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.physics_staff.EntityGrabClientState;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(PhysicsStaffClientHandler.class)
public class PhysicsStaffClientHandlerMixin {
    private static Class<?> beamNodeClass;
    private static Field beamNodesField;
    private static Field beamNodeRadiusField;
    private static Field nodePosField;
    private static Field nodePrevPosField;
    private static Field handlerBeamsField;

    private static final LineOutline beamRenderLine = new LineOutline();

    static {
        beamRenderLine.getParams()
                .colored(0xFFFFFF)
                .disableLineNormals()
                .lineWidth(0.0375f);
    }

    static {
        try {
            Class<?> beamClass = Class.forName("dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler$PhysicsBeam");
            beamNodeClass = Class.forName("dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler$PhysicsBeam$BeamNode");

            beamNodesField = beamClass.getDeclaredField("nodes");
            beamNodesField.setAccessible(true);
            beamNodeRadiusField = beamClass.getDeclaredField("currentNodeRadius");
            beamNodeRadiusField.setAccessible(true);

            nodePosField = beamNodeClass.getDeclaredField("position");
            nodePosField.setAccessible(true);
            nodePrevPosField = beamNodeClass.getDeclaredField("previousPosition");
            nodePrevPosField.setAccessible(true);

            handlerBeamsField = PhysicsStaffClientHandler.class.getDeclaredField("beams");
            handlerBeamsField.setAccessible(true);

            AeronauticsJoyofcreation.LOGGER.info("PhysicsBeam reflection initialized");
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.error("Failed to init PhysicsBeam reflection", e);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void joc$tickTail(CallbackInfo ci) {
        PhysicsStaffClientHandler handler = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER;
        int grabbedId = EntityGrabClientState.grabbedEntityId;

        if (grabbedId == 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            EntityGrabClientState.grabbedEntityId = 0;
            return;
        }

        Entity target = mc.level.getEntity(grabbedId);
        if (target == null || !target.isAlive()) {
            EntityGrabClientState.grabbedEntityId = 0;
            return;
        }

        boolean mainHand = mc.player.getMainHandItem().getItem() instanceof PhysicsStaffItem;
        Vec3 focusPos = PhysicsStaffClientHandler.getStaffFocusPos(mc.player, mainHand, 1.0F);
        if (focusPos.equals(Vec3.ZERO)) return;

        Vec3 center = target.position().add(0, target.getBbHeight() / 2, 0);

        // Delegate to the handler's beam lifecycle
        handler.updateBeam(mc.level, mc.player.getUUID(), focusPos, center);

        // Animate the staff model in hand
        handler.extension = 1.0f;
        handler.targetExtension = 1.0f;
        handler.cubeScale = 1.0f;
        handler.tilt = 1.0f;
    }

    @Inject(method = "onRender", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/SuperRenderTypeBuffer;draw()V", shift = At.Shift.BEFORE))
    private void joc$onRenderBeforeDraw(PoseStack poseStack, CallbackInfo ci) {
        int grabbedId = EntityGrabClientState.grabbedEntityId;
        if (grabbedId == 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity target = mc.level.getEntity(grabbedId);
        if (target == null || !target.isAlive()) return;

        boolean mainHand = mc.player.getMainHandItem().getItem() instanceof PhysicsStaffItem;
        if (!mainHand && !(mc.player.getOffhandItem().getItem() instanceof PhysicsStaffItem)) return;

        float partialTicks = AnimationTickHolder.getPartialTicks();
        Vec3 focusPos = PhysicsStaffClientHandler.getStaffFocusPos(mc.player, mainHand, partialTicks);
        if (focusPos.equals(Vec3.ZERO)) return;

        // Ensure beam exists in the handler's map (first-frame fallback)
        PhysicsStaffClientHandler handler = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER;
        Vec3 renderEnd = target.getPosition(partialTicks).add(0, target.getBbHeight() / 2, 0);

        // Check if beam exists in handler's map
        Object beam;
        try {
            Object beamsMap = handlerBeamsField.get(handler);
            beam = beamsMap.getClass().getMethod("get", Object.class).invoke(beamsMap, mc.player.getUUID());
        } catch (Exception e) {
            beam = null;
        }
        if (beam == null) {
            // First frame before tick ran — create it directly
            handler.updateBeam(mc.level, mc.player.getUUID(), focusPos, renderEnd);
            try {
                Object beamsMap = handlerBeamsField.get(handler);
                beam = beamsMap.getClass().getMethod("get", Object.class).invoke(beamsMap, mc.player.getUUID());
            } catch (Exception e) {
                return;
            }
            if (beam == null) return;
        }

        try {
            List<?> nodes = (List<?>) beamNodesField.get(beam);
            double radius = beamNodeRadiusField.getDouble(beam);

            if (nodes.size() < 2) return;

            Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();

            Vec3 diff = renderEnd.subtract(focusPos);
            int count = nodes.size();
            Vec3 prev = focusPos;

            for (int i = 1; i < count; i++) {
                Object node = nodes.get(i);
                Vec3 prevPos = (Vec3) nodePrevPosField.get(node);
                Vec3 currPos = (Vec3) nodePosField.get(node);
                Vec3 nodePos = prevPos.lerp(currPos, partialTicks);

                Vec3 currentPos = focusPos.add(
                        diff.scale((double) i / (double) count)
                ).add(nodePos.scale(radius));

                beamRenderLine.set(prev, currentPos);
                beamRenderLine.render(poseStack, buffer, cameraPos, partialTicks);

                prev = currentPos;
            }
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.error("[Beam] Failed to render", e);
        }
    }
}
