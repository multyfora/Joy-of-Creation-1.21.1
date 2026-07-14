package net.multyfora.client.seeker;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.multyfora.index.JocDataComponents;
import net.multyfora.index.JocItems;

public final class SeekerLinkedHighlightRenderer {

    private static final float RED = 0.27f;
    private static final float GREEN = 0.53f;
    private static final float BLUE = 1.0f;
    private static final float ALPHA = 0.45f;
    private static final double EXPAND = 0.015;

    private static final RenderType HIGHLIGHT = RenderType.create(
            "joc_seeker_link",
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

    private SeekerLinkedHighlightRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack held = findSeekerWithCapture(player);
        if (held == null) return;

        BlockPos targetPos = held.get(JocDataComponents.LINKED_SEEKER_POS.get());
        if (targetPos == null) return;

        Level level = player.level();
        if (!level.isLoaded(targetPos)) return;

        BlockEntity be = level.getBlockEntity(targetPos);
        if (!(be instanceof net.multyfora.content.seeker.SeekerBlockEntity)) return;

        VoxelShape shape = level.getBlockState(targetPos).getShape(level, targetPos);
        if (shape.isEmpty()) return;

        PoseStack ms = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        ms.pushPose();
        ms.translate(targetPos.getX() - camPos.x, targetPos.getY() - camPos.y, targetPos.getZ() - camPos.z);

        VertexConsumer consumer = buffer.getBuffer(HIGHLIGHT);
        for (AABB aabb : shape.toAabbs()) {
            double x0 = aabb.minX - EXPAND, y0 = aabb.minY - EXPAND, z0 = aabb.minZ - EXPAND;
            double x1 = aabb.maxX + EXPAND, y1 = aabb.maxY + EXPAND, z1 = aabb.maxZ + EXPAND;

            renderFace(consumer, ms.last(), x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
            renderFace(consumer, ms.last(), x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
            renderFace(consumer, ms.last(), x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
            renderFace(consumer, ms.last(), x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
            renderFace(consumer, ms.last(), x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            renderFace(consumer, ms.last(), x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        }

        ms.popPose();
        buffer.endBatch(HIGHLIGHT);
    }

    private static void renderFace(VertexConsumer consumer, PoseStack.Pose pose,
                                   double x0, double y0, double z0,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   double x3, double y3, double z3) {
        consumer.addVertex(pose, (float) x0, (float) y0, (float) z0).setColor(RED, GREEN, BLUE, ALPHA);
        consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(RED, GREEN, BLUE, ALPHA);
        consumer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(RED, GREEN, BLUE, ALPHA);
        consumer.addVertex(pose, (float) x3, (float) y3, (float) z3).setColor(RED, GREEN, BLUE, ALPHA);
    }

    private static ItemStack findSeekerWithCapture(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(JocItems.SEEKER.asItem()) && main.has(JocDataComponents.LINKED_SEEKER_POS.get())) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.is(JocItems.SEEKER.asItem()) && off.has(JocDataComponents.LINKED_SEEKER_POS.get())) {
            return off;
        }
        return null;
    }
}