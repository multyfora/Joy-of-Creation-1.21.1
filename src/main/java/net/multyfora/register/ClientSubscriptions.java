package net.multyfora.register;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.multyfora.AeronauticsJoyofcreation;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.multyfora.client.SeekerBlockEntityRenderer;
import net.multyfora.client.SeekerPartialModels;
import net.multyfora.client.seeker.SeekerBakedModel;
import net.multyfora.client.seeker.SeekerRenderer;
import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;
import net.multyfora.client.portable_throttle.PortableThrottleLinkScreen;
import net.multyfora.client.portable_typewriter.PortableTypewriterClientHandler;
import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;
import net.multyfora.config.JocConfig;
import net.multyfora.content.balloon.BalloonBlock;
import net.multyfora.content.physics_staff.CreativeStaffCaptureHandler;
import net.multyfora.content.physics_staff.EntityGrabClientState;
import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocMenuTypes;
import net.multyfora.network.EntityGrabPayloads;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class ClientSubscriptions {

    // Subscriptions
    private static final int[] DYE_COLORS = {
        0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA,
        0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
        0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA,
        0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21
    };

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(JocBlockEntityTypes.SEEKER.get(), SeekerBlockEntityRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> DYE_COLORS[state.getValue(BalloonBlock.COLOR)], JocBlocks.BALLOON.get());
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(JocMenuTypes.TYPEWRITER_SCREEN.get(), PortableTypewriterScreen::new);
        event.register(JocMenuTypes.THROTTLE_SCREEN.get(), PortableThrottleLinkScreen::new);
    }
    @SubscribeEvent
    static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("joc", "block/seeker")));
        event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("joc", "block/seeker_active")));
        event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("joc", "block/seeker_2d")));
        event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("joc", "block/seeker_2d_active")));
    }

    @SubscribeEvent
    static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var seekerBlockId = ResourceLocation.fromNamespaceAndPath("joc", "seeker");
        var modelDefault = ResourceLocation.fromNamespaceAndPath("joc", "block/seeker");
        var modelActive = ResourceLocation.fromNamespaceAndPath("joc", "block/seeker_active");
        var model2D = ResourceLocation.fromNamespaceAndPath("joc", "block/seeker_2d");
        var model2DActive = ResourceLocation.fromNamespaceAndPath("joc", "block/seeker_2d_active");

        var defaultKey = ModelResourceLocation.standalone(modelDefault);
        var activeKey = ModelResourceLocation.standalone(modelActive);
        var key2D = ModelResourceLocation.standalone(model2D);
        var key2DActive = ModelResourceLocation.standalone(model2DActive);

        BakedModel defaultModel = event.getModels().get(defaultKey);
        BakedModel activeModel = event.getModels().get(activeKey);
        BakedModel d2Model = event.getModels().get(key2D);
        BakedModel d2ActiveModel = event.getModels().get(key2DActive);

        if (defaultModel == null || activeModel == null || d2Model == null || d2ActiveModel == null) {
            return;
        }

        var models = event.getModels();
        for (var entry : models.entrySet().stream()
                .filter(e -> e.getKey().id().equals(seekerBlockId))
                .toList()
        ) {
            var variantKey = entry.getKey();
            var originalVariant = entry.getValue();
            models.put(variantKey, new SeekerBakedModel(
                    originalVariant, activeModel, d2Model, d2ActiveModel, originalVariant
            ));
        }
    }

    static {
        SeekerPartialModels.init();
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
