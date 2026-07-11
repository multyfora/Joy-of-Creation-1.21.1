package net.multyfora.client.seeker;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.multyfora.AeronauticsJoyofcreation;

import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class SpyglassTargetOutlineRenderer {

    private static final double SPYGLASS_MAX_RANGE = 256.0;
    private static final double EXPAND = 0.002;

    private static final RenderType HIGHLIGHT = RenderType.create(
            "joc_spyglass_highlight",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        BlockPos pos = getScopedTargetBlock(mc.player);
        if (pos == null) return;

        VoxelShape shape = mc.player.level().getBlockState(pos).getShape(mc.player.level(), pos);
        if (shape.isEmpty()) return;

        PoseStack ms = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        ms.pushPose();
        ms.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

        VertexConsumer consumer = buffer.getBuffer(HIGHLIGHT);
        for (AABB aabb : shape.toAabbs()) {
            double x0 = aabb.minX - EXPAND, y0 = aabb.minY - EXPAND, z0 = aabb.minZ - EXPAND;
            double x1 = aabb.maxX + EXPAND, y1 = aabb.maxY + EXPAND, z1 = aabb.maxZ + EXPAND;

            float r = 1.0f, g = 1.0f, b = 1.0f, a = 0.35f;

            renderFace(consumer, ms.last(), x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
            renderFace(consumer, ms.last(), x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
            renderFace(consumer, ms.last(), x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
            renderFace(consumer, ms.last(), x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
            renderFace(consumer, ms.last(), x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
            renderFace(consumer, ms.last(), x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        }

        ms.popPose();
        buffer.endBatch(HIGHLIGHT);
    }

    private static void renderFace(VertexConsumer consumer, PoseStack.Pose pose,
                                   double x0, double y0, double z0,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   double x3, double y3, double z3,
                                   float r, float g, float b, float a) {
        consumer.addVertex(pose, (float) x0, (float) y0, (float) z0).setColor(r, g, b, a);
        consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        consumer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
        consumer.addVertex(pose, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
    }

    @Nullable
    public static BlockPos getScopedTargetBlock(Player player) {
        if (!(player instanceof LocalPlayer lp) || !isScopedWithSpyglass(lp)) return null;

        Level level = player.level();
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 to = from.add(look.scale(SPYGLASS_MAX_RANGE));
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult result = level.clip(ctx);
        if (result.getType() == HitResult.Type.MISS) return null;
        return result.getBlockPos();
    }

    public static boolean isScopedWithSpyglass(LocalPlayer player) {
        if (!player.isUsingItem()) return false;
        ItemStack using = player.getUseItem();
        return using.is(Items.SPYGLASS);
    }
}