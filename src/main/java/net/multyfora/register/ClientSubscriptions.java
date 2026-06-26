package net.multyfora.register;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.balloon.BalloonTetherRenderer;
import net.multyfora.client.coordnav.CoordNavRenderer;
import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;
import net.multyfora.client.portable_throttle.PortableThrottleLinkScreen;
import net.multyfora.client.portable_typewriter.PortableTypewriterClientHandler;
import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;
import net.multyfora.config.JocConfig;
import net.multyfora.content.physics_staff.CreativeStaffCaptureHandler;
import net.multyfora.content.physics_staff.EntityGrabClientState;
import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocMenuTypes;
import net.multyfora.network.EntityGrabPayloads;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class ClientSubscriptions {

    // Subscriptions
    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(JocBlockEntityTypes.BALLOON.get(), BalloonTetherRenderer::new);
        event.registerBlockEntityRenderer(JocBlockEntityTypes.COORD_NAV.get(), CoordNavRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(JocMenuTypes.TYPEWRITER_SCREEN.get(), PortableTypewriterScreen::new);
        event.register(JocMenuTypes.THROTTLE_SCREEN.get(), PortableThrottleLinkScreen::new);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        PortableTypewriterClientHandler.tick();
        PortableThrottleClientHandler.tick();
    }

    @SubscribeEvent
    static void onMouseButtonPress(InputEvent.MouseButton.Pre event) {
        handleMouseButtonPress(event);
    }

    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        handleMouseScroll(event);
    }

    // Longer Implementations

    private static void handleMouseButtonPress(InputEvent.MouseButton.Pre event) {
        if(
               event.getAction() != 1
            || event.getButton() != 1
            || !JocConfig.ENABLE_CREATIVE_STAFF.get()
        ) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if(
            client.player == null || client.level == null
            || !CreativeStaffCaptureHandler.isHoldingStaff(client.player)
        ) {
            return;
        }

        Entity target = null;
        if(client.crosshairPickEntity != null) {
            target = client.crosshairPickEntity;
        } else {
            Entity viewer = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;
            if (viewer == null) {
                return;
            }

            float range = 64.0f;
            Vec3 start = viewer.getEyePosition();
            Vec3 look = viewer.getLookAngle();
            Vec3 end = start.add( look.scale(range) );
            AABB aabb = viewer
                .getBoundingBox()
                .expandTowards( look.scale(range) )
                .inflate(1.0, 1.0, 1.0)
            ;

            EntityHitResult result = ProjectileUtil.getEntityHitResult(
                viewer, start, end, aabb,
                (entity) -> { return !entity.isSpectator() && entity.isPickable(); },
                range * range
            );

            if (result != null) {
                target = result.getEntity();
            }
        }

        if (target != null) {
            PacketDistributor.sendToServer(new EntityGrabPayloads.GrabRequest(target.getId()));
            event.setCanceled(true);
        }
    }

    static void handleMouseScroll(InputEvent.MouseScrollingEvent event) {
        if( !JocConfig.ENABLE_CREATIVE_STAFF.get() ) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if(
               client.player == null || client.level == null
            || EntityGrabClientState.grabbedEntityId == 0
            || !CreativeStaffCaptureHandler.isHoldingStaff(client.player)
        ) {
            return;
        }

        double delta = event.getScrollDeltaY();
        if (delta == 0) {
            return;
        }

        double sensitivity = 0.5;
        double new_distance = EntityGrabClientState.holdDistance + delta * sensitivity;
        double maximum_distance = 64.0;
        new_distance = Math.clamp(new_distance, 0.5, maximum_distance);

        if(new_distance != EntityGrabClientState.holdDistance) {
            EntityGrabClientState.holdDistance = new_distance;
            PacketDistributor.sendToServer(
                new EntityGrabPayloads.SetHoldDistance(
                    EntityGrabClientState.grabbedEntityId,
                    new_distance
                )
            );
        }

        event.setCanceled(true);
    }
}
