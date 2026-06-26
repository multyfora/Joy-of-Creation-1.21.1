package net.multyfora;

import net.multyfora.content.portable_throttle.PortableThrottleRenderHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.multyfora.client.balloon.BalloonTetherRenderer;
import net.multyfora.client.coordnav.CoordNavRenderer;
import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;
import net.multyfora.client.portable_throttle.PortableThrottleScreen;
import net.multyfora.client.portable_typewriter.PortableTypewriterClientHandler;
import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;
import net.multyfora.config.JocConfig;
import net.multyfora.content.physics_staff.CreativeStaffCaptureHandler;
import net.multyfora.content.physics_staff.EntityGrabClientState;
import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocMenuTypes;
import net.multyfora.network.EntityGrabPayloads;

@Mod(value = AeronauticsJoyofcreation.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class AeronauticsJoyofcreationClient {

    public static final PortableThrottleRenderHandler PORTABLE_THROTTLE_RENDER_HANDLER = new PortableThrottleRenderHandler();

    public AeronauticsJoyofcreationClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        PORTABLE_THROTTLE_RENDER_HANDLER.registerListeners(NeoForge.EVENT_BUS);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(JocBlockEntityTypes.BALLOON.get(), BalloonTetherRenderer::new);
        event.registerBlockEntityRenderer(JocBlockEntityTypes.COORD_NAV.get(), CoordNavRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(JocMenuTypes.TYPEWRITER_SCREEN.get(), PortableTypewriterScreen::new);
        event.register(JocMenuTypes.THROTTLE_SCREEN.get(), PortableThrottleScreen::new);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        PortableTypewriterClientHandler.tick();
        PortableThrottleClientHandler.tick();
    }

    @SubscribeEvent
    static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != 1) return;
        if (event.getButton() != 1) return;

        if (!JocConfig.ENABLE_CREATIVE_STAFF.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!CreativeStaffCaptureHandler.isHoldingStaff(mc.player)) {
            return;
        }

        Entity target = null;

        if (mc.crosshairPickEntity != null) {
            target = mc.crosshairPickEntity;
        } else {
            Entity viewer = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
            if (viewer == null) return;

            float range = 64.0f;
            Vec3 start = viewer.getEyePosition();
            Vec3 look = viewer.getLookAngle();
            Vec3 end = start.add(look.scale(range));
            AABB aabb = viewer.getBoundingBox()
                    .expandTowards(look.scale(range))
                    .inflate(1.0, 1.0, 1.0);

            EntityHitResult result = ProjectileUtil.getEntityHitResult(
                    viewer, start, end, aabb,
                    e -> !e.isSpectator() && e.isPickable(),
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

    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!JocConfig.ENABLE_CREATIVE_STAFF.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (EntityGrabClientState.grabbedEntityId == 0) return;
        if (!CreativeStaffCaptureHandler.isHoldingStaff(mc.player)) return;

        double delta = event.getScrollDeltaY();
        if (delta == 0) return;

        double sensitivity = 0.5;
        double newDist = EntityGrabClientState.holdDistance + delta * sensitivity;
        double maxDist = 64.0;
        newDist = Math.clamp(newDist, 0.5, maxDist);

        if (newDist != EntityGrabClientState.holdDistance) {
            EntityGrabClientState.holdDistance = newDist;
            PacketDistributor.sendToServer(new EntityGrabPayloads.SetHoldDistance(
                    EntityGrabClientState.grabbedEntityId, newDist
            ));
        }

        event.setCanceled(true);
    }
}
