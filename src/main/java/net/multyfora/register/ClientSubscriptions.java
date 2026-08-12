package net.multyfora.register;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.seeker.SeekerLinkedHighlightRenderer;
import net.multyfora.client.seeker.SpyglassTargetOutlineRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.multyfora.client.SeekerBlockEntityRenderer;
import net.multyfora.client.JocPartialModels;
import net.multyfora.client.SeekerPartialModels;
import net.multyfora.client.seeker.SeekerBakedModel;
import net.multyfora.client.seeker.SeekerRenderer;
import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;
import net.multyfora.client.portable_throttle.PortableThrottleLinkScreen;
import net.multyfora.client.portable_typewriter.PortableTypewriterClientHandler;
import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;
import net.multyfora.config.JocConfig;
import net.multyfora.content.physics_staff.CreativeStaffCaptureHandler;
import net.multyfora.content.physics_staff.EntityGrabClientState;
import net.multyfora.content.shears_cut.ShearsCutRemoteState;
import net.multyfora.content.shears_cut.ShearsCutState;
import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocEntityTypes;
import net.multyfora.index.JocMenuTypes;
import net.multyfora.network.EntityGrabPayloads;
import net.multyfora.network.SeekerPayloads;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.multyfora.content.gyroseat.GyroscopicSeatEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class ClientSubscriptions {

    // Subscriptions

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(JocBlockEntityTypes.SEEKER.get(), SeekerBlockEntityRenderer::new);
        event.registerEntityRenderer((EntityType<GyroscopicSeatEntity>) (EntityType<?>) JocEntityTypes.GYROSCOPIC_SEAT.get(), GyroscopicSeatRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(JocMenuTypes.TYPEWRITER_SCREEN.get(), PortableTypewriterScreen::new);
        event.register(JocMenuTypes.THROTTLE_SCREEN.get(), PortableThrottleLinkScreen::new);
    }

    @SubscribeEvent
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            SeekerLinkedHighlightRenderer.render(event);
        }
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
        JocPartialModels.init();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        PortableTypewriterClientHandler.tick();
        PortableThrottleClientHandler.tick();
        ShearsCutState.tick();
        ShearsCutRemoteState.tick();
    }

    @SubscribeEvent
    static void onMouseButtonPress(InputEvent.MouseButton.Pre event) {
        handleSpyglassScopedLeftClick(event);
    }

    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        handleMouseScroll(event);
    }

    // Longer Implementations

    private static void handleSpyglassScopedLeftClick(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != 1 || event.getButton() != 0) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        BlockPos targetPos = SpyglassTargetOutlineRenderer.getScopedTargetBlock(client.player);
        if (targetPos == null) return;

        UUID subLevelId = null;
        double localX = 0, localY = 0, localZ = 0;
        try {
            SubLevel sub = Sable.HELPER.getContaining(client.level, targetPos);
            if (sub != null) {
                subLevelId = sub.getUniqueId();
                Vec3 worldCenter = Vec3.atCenterOf(targetPos);
                Vec3 local = sub.logicalPose().transformPositionInverse(worldCenter);
                localX = local.x;
                localY = local.y;
                localZ = local.z;
            }
        } catch (Exception ignored) {}

        client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 2.0f);
        PacketDistributor.sendToServer(new SeekerPayloads.SetSpyglassTargetPayload(targetPos, subLevelId,
            localX, localY, localZ));
        event.setCanceled(true);
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

    public static class GyroscopicSeatRenderer extends EntityRenderer<GyroscopicSeatEntity> {
        public GyroscopicSeatRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public boolean shouldRender(GyroscopicSeatEntity entity, Frustum frustum, double x, double y, double z) {
            return false;
        }

        @Override
        public ResourceLocation getTextureLocation(GyroscopicSeatEntity entity) {
            return null;
        }
    }
}
