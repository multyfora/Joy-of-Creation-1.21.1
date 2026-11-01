package net.multyfora.client.seeker;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.multyfora.client.JocPartialModels;
import net.multyfora.content.seeker.MapPinEntity;

import org.joml.Quaternionf;

public class MapPinRenderer extends EntityRenderer<MapPinEntity> {

    private static final float PIVOT_Y = 5 / 16f;

    public MapPinRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MapPinEntity pin, float entityYaw, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        Vec3 motion = pin.getDeltaMovement();
        ms.pushPose();
        if (motion.lengthSqr() > 1.0e-6)
            ms.mulPose(new Quaternionf().rotationTo(0, -1, 0, (float) motion.x, (float) motion.y, (float) motion.z));
        ms.translate(-0.5, -PIVOT_Y, -0.5);
        CachedBuffers.partial(JocPartialModels.SEEKER_TABLE_PIN, Blocks.AIR.defaultBlockState())
            .light(light)
            .renderInto(ms, buffer.getBuffer(Sheets.cutoutBlockSheet()));
        ms.popPose();
        super.render(pin, entityYaw, partialTicks, ms, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MapPinEntity pin) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
