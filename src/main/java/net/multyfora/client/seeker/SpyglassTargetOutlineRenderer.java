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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.index.JocDataComponents;
import net.multyfora.index.SeekerCapturedTarget;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class SpyglassTargetOutlineRenderer {

    private static final double SPYGLASS_MAX_RANGE = 256.0;
    private static final double EXPAND = 0.002;

    private static SubLevel cachedCursorSubLevel;
    private static SubLevel cachedCapturedSubLevel;
    private static UUID cachedCapturedSubLevelId;

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

        SubLevel cursorSub = getScopedSubLevel(mc.player);
        BlockPos cursorPos = cursorSub != null ? null : getScopedTargetBlock(mc.player);
        SubLevel capturedSub = getCapturedSubLevel(mc.player);
        BlockPos capturedBlock = capturedSub != null ? null : getCapturedBlock(mc.player);

        if (cursorSub != null) {
            renderBoundingBox(event, mc, cursorSub.boundingBox(), 1.0f, 1.0f, 1.0f, 0.35f);
        } else if (cursorPos != null) {
            renderBlockHighlight(event, mc, cursorPos, 1.0f, 1.0f, 1.0f, 0.35f);
        }

        if (capturedSub != null) {
            renderBoundingBox(event, mc, capturedSub.boundingBox(), 0.0f, 1.0f, 0.0f, 0.35f);
        } else if (capturedBlock != null) {
            renderBlockHighlight(event, mc, capturedBlock, 0.0f, 1.0f, 0.0f, 0.35f);
        }
    }

    private static void renderBlockHighlight(RenderLevelStageEvent event, Minecraft mc, BlockPos pos, float r, float g, float b, float a) {
        Level level = mc.player.level();

        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }

        PoseStack ms = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();

        ms.pushPose();
        ms.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(HIGHLIGHT);
        for (AABB aabb : shape.toAabbs()) {
            double x0 = aabb.minX - EXPAND, y0 = aabb.minY - EXPAND, z0 = aabb.minZ - EXPAND;
            double x1 = aabb.maxX + EXPAND, y1 = aabb.maxY + EXPAND, z1 = aabb.maxZ + EXPAND;

            renderFace(consumer, ms.last(), x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
            renderFace(consumer, ms.last(), x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
            renderFace(consumer, ms.last(), x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
            renderFace(consumer, ms.last(), x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
            renderFace(consumer, ms.last(), x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
            renderFace(consumer, ms.last(), x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        }

        ms.popPose();
        mc.renderBuffers().bufferSource().endBatch(HIGHLIGHT);
    }

    private static void renderBoundingBox(RenderLevelStageEvent event, Minecraft mc, BoundingBox3dc bbox, float r, float g, float b, float a) {
        PoseStack ms = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();

        double minX = bbox.minX(), minY = bbox.minY(), minZ = bbox.minZ();
        double maxX = bbox.maxX(), maxY = bbox.maxY(), maxZ = bbox.maxZ();
        double w = maxX - minX, h = maxY - minY, d = maxZ - minZ;

        ms.pushPose();
        ms.translate(minX - camPos.x, minY - camPos.y, minZ - camPos.z);

        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(HIGHLIGHT);

        renderFace(consumer, ms.last(), 0, 0, 0, w, 0, 0, w, 0, d, 0, 0, d, r, g, b, a);
        renderFace(consumer, ms.last(), 0, h, 0, w, h, 0, w, h, d, 0, h, d, r, g, b, a);
        renderFace(consumer, ms.last(), 0, 0, 0, w, 0, 0, w, h, 0, 0, h, 0, r, g, b, a);
        renderFace(consumer, ms.last(), 0, 0, d, w, 0, d, w, h, d, 0, h, d, r, g, b, a);
        renderFace(consumer, ms.last(), 0, 0, 0, 0, 0, d, 0, h, d, 0, h, 0, r, g, b, a);
        renderFace(consumer, ms.last(), w, 0, 0, w, 0, d, w, h, d, w, h, 0, r, g, b, a);

        ms.popPose();
        mc.renderBuffers().bufferSource().endBatch(HIGHLIGHT);
    }

    @Nullable
    private static SubLevel getScopedSubLevel(Player player) {
        if (!(player instanceof LocalPlayer lp) || !isScopedWithSpyglass(lp)) return null;

        Level level = player.level();
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 to = from.add(look.scale(SPYGLASS_MAX_RANGE));
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult result = level.clip(ctx);
        if (result.getType() == HitResult.Type.MISS) return null;

        try {
            SubLevel sub = Sable.HELPER.getContaining(level, result.getBlockPos());
            if (sub != null) {
                cachedCursorSubLevel = sub;
                return sub;
            }
        } catch (Exception ignored) {}

        cachedCursorSubLevel = null;
        return null;
    }

    @Nullable
    private static SubLevel getCapturedSubLevel(Player player) {
        if (!(player instanceof LocalPlayer)) return null;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack spyglass = mainHand.is(Items.SPYGLASS) ? mainHand
                : offHand.is(Items.SPYGLASS) ? offHand
                : null;

        if (spyglass == null) return null;
        SeekerCapturedTarget target = spyglass.get(JocDataComponents.SEEKER_CARRIED_TARGET.get());
        if (target == null || target.subLevelId() == null) {
            cachedCapturedSubLevel = null;
            cachedCapturedSubLevelId = null;
            return null;
        }

        if (cachedCapturedSubLevel == null || !target.subLevelId().equals(cachedCapturedSubLevelId) || cachedCapturedSubLevel.isRemoved()) {
            cachedCapturedSubLevel = null;
            cachedCapturedSubLevelId = null;
            try {
                var container = SubLevelContainer.getContainer(player.level());
                if (container != null) {
                    SubLevel found = container.getSubLevel(target.subLevelId());
                    if (found != null) {
                        cachedCapturedSubLevel = found;
                        cachedCapturedSubLevelId = target.subLevelId();
                    }
                }
            } catch (Exception ignored) {}
        }

        return cachedCapturedSubLevel;
    }

    @Nullable
    private static BlockPos getCapturedBlock(Player player) {
        if (!(player instanceof LocalPlayer)) return null;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack spyglass = mainHand.is(Items.SPYGLASS) ? mainHand
                : offHand.is(Items.SPYGLASS) ? offHand
                : null;

        if (spyglass == null) return null;
        SeekerCapturedTarget target = spyglass.get(JocDataComponents.SEEKER_CARRIED_TARGET.get());
        if (target == null || target.subLevelId() != null) return null;
        return target.pos();
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
