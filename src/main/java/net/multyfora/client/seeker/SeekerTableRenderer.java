package net.multyfora.client.seeker;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.client.JocPartialModels;
import net.multyfora.config.JocClientConfig;
import net.multyfora.content.seeker.SeekerTableBlock;
import net.multyfora.content.seeker.SeekerTableBlockEntity;
import net.multyfora.content.seeker.SeekerTableMarker;

import org.joml.Matrix3f;
import org.joml.Quaternionf;

public class SeekerTableRenderer extends SafeBlockEntityRenderer<SeekerTableBlockEntity> {

    private static final float MAP_LIFT = 0.004f;
    private static final float PIN_SCALE = 0.8f;
    private static final float PIN_DEPTH = 1.5f / 16;
    private static final float PIN_DROP_TICKS = 6;
    private static final float OFF_MAP_TILT = (float) Math.toRadians(35);
    private static final ResourceLocation ARROW_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    private static final float ARROW_LEAN = 58, ARROW_YAW = 150;

    private static final float MAX_RELIEF = 9f / 16;
    private static final float HEIGHT_STEP = 0.5f / 16;
    static final float CELLS_PER_BLOCK = 12;
    private static final int MAX_CELLS = 128;
    private static final double COMPASS_INSET = 0.42;
    private static final float COMPASS_SCALE = 0.72f;
    private static final float HERE_SCALE = 0.4f;

    public SeekerTableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(SeekerTableBlockEntity table, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Level level = table.getLevel();
        if (level == null || !table.isController() || table.getWidth() < 2 || table.getDepth() < 2)
            return;

        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(table);
        Pose3dc pose = subLevel == null ? null : subLevel.renderPose(partialTicks);
        Vec3 centerLocal = table.getMapCenterLocal();
        Vec3 worldCenter = pose == null ? centerLocal : pose.transformPosition(centerLocal);

        SeekerTableMap map = SeekerTableMap.get(table);
        map.tick(level, worldCenter.x, worldCenter.z);
        Surface surface = new Surface(table, map, pose);

        ms.pushPose();
        toLocalFrame(table.getFacing(), ms);
        renderMap(surface, ms, buffer, light);
        renderCompass(table, surface, worldCenter, pose, ms, buffer, light);
        renderPart(JocPartialModels.SEEKER_TABLE_HERE, table, ms, buffer, light, table.getWidth() / 2.0,
            surface.mapY + surface.hereTop, table.getDepth() / 2.0, 0, HERE_SCALE);
        if (table.hasMarker())
            renderPin(table, map, surface, worldCenter, pose, partialTicks, ms, buffer, light);
        ms.popPose();
    }

    private static void toLocalFrame(Direction facing, PoseStack ms) {
        if (facing == Direction.UP)
            return;
        Direction right = SeekerTableBlock.toWorld(facing, Direction.EAST);
        Direction down = SeekerTableBlock.toWorld(facing, Direction.SOUTH);
        Matrix3f turn = new Matrix3f()
            .setColumn(0, right.getStepX(), right.getStepY(), right.getStepZ())
            .setColumn(1, facing.getStepX(), facing.getStepY(), facing.getStepZ())
            .setColumn(2, down.getStepX(), down.getStepY(), down.getStepZ());
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(turn.getNormalizedRotation(new Quaternionf()));
        ms.translate(-0.5, -0.5, -0.5);
    }

    private static final class Surface {
        final SeekerTableMap map;
        final float x0, x1, z0, z1;
        final double worldX, worldZ, xx, xz, zx, zz;
        final int scale;
        final boolean terrain = JocClientConfig.SEEKER_TABLE_TERRAIN.get();
        final int nx, nz;
        final float cellX, cellZ;
        final float[] heights;
        final double compassX, compassZ;
        final float compassTop, hereTop;
        final double mapY;

        Surface(SeekerTableBlockEntity table, SeekerTableMap map, Pose3dc pose) {
            this.map = map;
            scale = table.getScale();
            x0 = z0 = (float) SeekerTableBlockEntity.RIM;
            x1 = (float) (table.getWidth() - SeekerTableBlockEntity.RIM);
            z1 = (float) (table.getDepth() - SeekerTableBlockEntity.RIM);
            mapY = table.getMapY();
            Vec3 o = table.mapToWorld(0, 0, pose);
            Vec3 ex = table.mapToWorld(1, 0, pose);
            Vec3 ez = table.mapToWorld(0, 1, pose);
            worldX = o.x;
            worldZ = o.z;
            xx = ex.x - o.x;
            xz = ex.z - o.z;
            zx = ez.x - o.x;
            zz = ez.z - o.z;

            compassX = compassZ = SeekerTableBlockEntity.RIM + COMPASS_INSET;
            compassTop = plateau(compassX, compassZ, COMPASS_SCALE * 0.5);
            hereTop = plateau(table.getWidth() / 2.0, table.getDepth() / 2.0, HERE_SCALE * 0.5);

            nx = Mth.clamp(Mth.ceil((x1 - x0) * CELLS_PER_BLOCK), 1, MAX_CELLS);
            nz = Mth.clamp(Mth.ceil((z1 - z0) * CELLS_PER_BLOCK), 1, MAX_CELLS);
            cellX = (x1 - x0) / nx;
            cellZ = (z1 - z0) / nz;
            heights = new float[nx * nz];
            for (int j = 0; j < nz; j++) {
                for (int i = 0; i < nx; i++) {
                    double x = x0 + (i + 0.5) * cellX, z = z0 + (j + 0.5) * cellZ;
                    float h;
                    if (within(x, z, compassX, compassZ, COMPASS_SCALE * 0.5))
                        h = compassTop;
                    else if (within(x, z, table.getWidth() / 2.0, table.getDepth() / 2.0, HERE_SCALE * 0.5))
                        h = hereTop;
                    else
                        h = ground(x, z);
                    heights[j * nx + i] = h;
                }
            }
        }

        float u(float x, float z) {
            return map.u(worldX + x * xx + z * zx);
        }

        float v(float x, float z) {
            return map.v(worldZ + x * xz + z * zz);
        }

        private float ground(double x, double z) {
            if (!terrain)
                return 0;
            float rise = map.rise(worldX + x * xx + z * zx, worldZ + x * xz + z * zz);
            float h = MAX_RELIEF * (1 - (float) Math.exp(-rise / scale / MAX_RELIEF));
            return Math.round(h / HEIGHT_STEP) * HEIGHT_STEP;
        }

        private float plateau(double x, double z, double radius) {
            float top = ground(x, z);
            for (int ring = 1; ring <= 2; ring++)
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4, r = radius * ring / 2;
                    top = Math.max(top, ground(x + Math.cos(a) * r, z + Math.sin(a) * r));
                }
            return top;
        }

        private static boolean within(double x, double z, double cx, double cz, double radius) {
            return (x - cx) * (x - cx) + (z - cz) * (z - cz) <= radius * radius;
        }

        float cell(int i, int j) {
            return i < 0 || j < 0 || i >= nx || j >= nz ? 0 : heights[j * nx + i];
        }

        double top(double x, double z) {
            int i = Mth.clamp(Mth.floor((x - x0) / cellX), 0, nx - 1);
            int j = Mth.clamp(Mth.floor((z - z0) / cellZ), 0, nz - 1);
            return mapY + heights[j * nx + i];
        }

        private static float smooth(float t) {
            if (t <= 0)
                return 0;
            if (t >= 1)
                return 1;
            return t * t * (3 - 2 * t);
        }
    }

    private static void renderMap(Surface surface, PoseStack ms, MultiBufferSource buffer, int light) {
        VertexConsumer vc = buffer.getBuffer(RenderType.text(surface.map.location()));
        PoseStack.Pose last = ms.last();
        float base = (float) surface.mapY + MAP_LIFT;
        for (int j = 0; j < surface.nz; j++) {
            for (int i = 0; i < surface.nx; i++) {
                float h = surface.cell(i, j);
                float xa = surface.x0 + i * surface.cellX, xb = xa + surface.cellX;
                float za = surface.z0 + j * surface.cellZ, zb = za + surface.cellZ;
                float top = base + h;

                float x = (xa + xb) / 2, z = (za + zb) / 2;
                float edge = Math.min(Math.min(x - surface.x0, surface.x1 - x), Math.min(z - surface.z0, surface.z1 - z));
                float tone = (0.86f + 0.14f * Surface.smooth(edge / 0.35f)) * (0.9f + 0.1f * h / MAX_RELIEF);
                int c = (int) (255 * tone);

                vertex(vc, last, surface, xa, top, za, xa, za, c, light);
                vertex(vc, last, surface, xa, top, zb, xa, zb, c, light);
                vertex(vc, last, surface, xb, top, zb, xb, zb, c, light);
                vertex(vc, last, surface, xb, top, za, xb, za, c, light);

                int lit = (int) (c * 0.78f), dark = (int) (c * 0.58f);
                float low = base + surface.cell(i - 1, j);
                if (low < top) {
                    vertex(vc, last, surface, xa, top, za, xa, za, lit, light);
                    vertex(vc, last, surface, xa, low, za, xa, za, lit, light);
                    vertex(vc, last, surface, xa, low, zb, xa, zb, lit, light);
                    vertex(vc, last, surface, xa, top, zb, xa, zb, lit, light);
                }
                low = base + surface.cell(i + 1, j);
                if (low < top) {
                    vertex(vc, last, surface, xb, top, zb, xb, zb, dark, light);
                    vertex(vc, last, surface, xb, low, zb, xb, zb, dark, light);
                    vertex(vc, last, surface, xb, low, za, xb, za, dark, light);
                    vertex(vc, last, surface, xb, top, za, xb, za, dark, light);
                }
                low = base + surface.cell(i, j - 1);
                if (low < top) {
                    vertex(vc, last, surface, xb, top, za, xb, za, lit, light);
                    vertex(vc, last, surface, xb, low, za, xb, za, lit, light);
                    vertex(vc, last, surface, xa, low, za, xa, za, lit, light);
                    vertex(vc, last, surface, xa, top, za, xa, za, lit, light);
                }
                low = base + surface.cell(i, j + 1);
                if (low < top) {
                    vertex(vc, last, surface, xa, top, zb, xa, zb, dark, light);
                    vertex(vc, last, surface, xa, low, zb, xa, zb, dark, light);
                    vertex(vc, last, surface, xb, low, zb, xb, zb, dark, light);
                    vertex(vc, last, surface, xb, top, zb, xb, zb, dark, light);
                }
            }
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose last, Surface surface,
                               float x, float y, float z, float mapX, float mapZ, int c, int light) {
        vc.addVertex(last, x, y, z)
            .setColor(c, c, c, 255)
            .setUv(surface.u(mapX, mapZ), surface.v(mapX, mapZ))
            .setLight(light);
    }

    private static void renderCompass(SeekerTableBlockEntity table, Surface surface, Vec3 worldCenter, Pose3dc pose,
                                      PoseStack ms, MultiBufferSource buffer, int light) {
        double[] north = table.worldToMapOffset(worldCenter.x, worldCenter.z - 1, worldCenter, 1, pose);
        float angle = (float) Math.atan2(-north[0], -north[1]);
        renderPart(JocPartialModels.SEEKER_TABLE_COMPASS, table, ms, buffer, light, surface.compassX,
            surface.mapY + surface.compassTop, surface.compassZ, angle, COMPASS_SCALE);
    }

    private static void renderPart(PartialModel model, SeekerTableBlockEntity table, PoseStack ms, MultiBufferSource buffer,
                                   int light, double x, double y, double z, float yaw, float scale) {
        ms.pushPose();
        ms.translate(x, y + MAP_LIFT * 2, z);
        ms.mulPose(Axis.YP.rotation(yaw));
        ms.scale(scale, 1, scale);
        ms.translate(-0.5, 0, -0.5);
        CachedBuffers.partial(model, table.getBlockState())
            .light(light)
            .renderInto(ms, buffer.getBuffer(RenderType.cutout()));
        ms.popPose();
    }

    private static void renderPin(SeekerTableBlockEntity table, SeekerTableMap map, Surface surface, Vec3 worldCenter, Pose3dc pose,
                                  float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        double[] offset = table.worldToMapOffset(table.getMarkerX(), table.getMarkerZ(), worldCenter, table.getScale(), pose);

        Level level = table.getLevel();
        SeekerTableMarker style = table.getMarkerStyle();
        if (table.getMarkerX() != map.shownMarkerX || table.getMarkerZ() != map.shownMarkerZ || style != map.shownMarkerStyle) {
            map.shownMarkerX = table.getMarkerX();
            map.shownMarkerZ = table.getMarkerZ();
            map.shownMarkerStyle = style;
            map.markerPlacedAt = level.getGameTime();
        }
        float time = level.getGameTime() + partialTicks;
        float drop = Mth.clamp((time - map.markerPlacedAt) / PIN_DROP_TICKS, 0, 1);
        float lift = (1 - drop) * (1 - drop) * 0.6f;

        double halfW = table.getWidth() / 2.0, halfD = table.getDepth() / 2.0;
        double margin = SeekerTableBlockEntity.RIM + 0.18;
        double x = Mth.clamp(offset[0], -halfW + margin, halfW - margin);
        double z = Mth.clamp(offset[1], -halfD + margin, halfD - margin);
        boolean offMap = x != offset[0] || z != offset[1];

        double ground = surface.top(halfW + x, halfD + z);
        ms.pushPose();
        ms.translate(halfW + x, ground + lift, halfD + z);
        if (offMap) {
            double ox = offset[0] - x, oz = offset[1] - z;
            double length = Math.sqrt(ox * ox + oz * oz);
            ms.mulPose(new Quaternionf().rotateAxis(OFF_MAP_TILT, (float) (oz / length), 0, (float) (-ox / length)));
        }

        BlockState state = table.getBlockState();
        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
        switch (style) {
            case PIN -> renderMarkerPart(JocPartialModels.SEEKER_TABLE_PIN, state, ms, vc, light, PIN_SCALE, PIN_DEPTH);
            case KNIFE -> {
                ms.mulPose(Axis.YP.rotationDegrees(-35));
                ms.mulPose(Axis.XP.rotationDegrees(14));
                renderMarkerPart(JocPartialModels.SEEKER_TABLE_KNIFE, state, ms, vc, light, 0.7f, 2.5f / 16);
            }
            case FLAG -> {
                renderMarkerPart(JocPartialModels.SEEKER_TABLE_FLAG_POLE, state, ms, vc, light, PIN_SCALE, PIN_DEPTH);
                float swing = Mth.sin(time * 0.05f) * 14 + Mth.sin(time * 0.13f) * 4;
                if (table.getFacing() != Direction.UP)
                    swing -= 90;
                int color = table.getMarkerColor();
                ms.pushPose();
                ms.translate(0, -PIN_DEPTH * PIN_SCALE, 0);
                ms.mulPose(Axis.YP.rotationDegrees(swing));
                ms.scale(PIN_SCALE, PIN_SCALE, PIN_SCALE);
                ms.translate(-0.5, 0, -0.5);
                CachedBuffers.partial(JocPartialModels.SEEKER_TABLE_FLAG_CLOTH, state)
                    .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255)
                    .light(light)
                    .renderInto(ms, vc);
                ms.popPose();
            }
            case ARROW -> renderArrow(ms, buffer, light);
            case TORCH -> {
                renderMarkerPart(JocPartialModels.SEEKER_TABLE_TORCH, state, ms, vc, light, PIN_SCALE, PIN_DEPTH);
                renderMarkerPart(JocPartialModels.SEEKER_TABLE_TORCH_FLAME, state, ms, vc, LightTexture.FULL_BRIGHT, PIN_SCALE, PIN_DEPTH);
                spawnTorchParticles(table, map, pose, halfW + x, ground, halfD + z, level);
            }
        }
        ms.popPose();
    }

    private static void renderArrow(PoseStack ms, MultiBufferSource buffer, int light) {
        ms.pushPose();
        ms.mulPose(Axis.YP.rotationDegrees(ARROW_YAW));
        ms.mulPose(Axis.ZP.rotationDegrees(-ARROW_LEAN));
        ms.mulPose(Axis.XP.rotationDegrees(45));
        ms.scale(0.05625f, 0.05625f, 0.05625f);
        ms.translate(-4, 0, 0);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(ARROW_TEXTURE));
        arrowVertex(ms, vc, -7, -2, -2, 0, 0.15625f, -1, 0, 0, light);
        arrowVertex(ms, vc, -7, -2, 2, 0.15625f, 0.15625f, -1, 0, 0, light);
        arrowVertex(ms, vc, -7, 2, 2, 0.15625f, 0.3125f, -1, 0, 0, light);
        arrowVertex(ms, vc, -7, 2, -2, 0, 0.3125f, -1, 0, 0, light);
        arrowVertex(ms, vc, -7, 2, -2, 0, 0.15625f, 1, 0, 0, light);
        arrowVertex(ms, vc, -7, 2, 2, 0.15625f, 0.15625f, 1, 0, 0, light);
        arrowVertex(ms, vc, -7, -2, 2, 0.15625f, 0.3125f, 1, 0, 0, light);
        arrowVertex(ms, vc, -7, -2, -2, 0, 0.3125f, 1, 0, 0, light);
        for (int i = 0; i < 4; i++) {
            ms.mulPose(Axis.XP.rotationDegrees(90));
            arrowVertex(ms, vc, -8, -2, 0, 0, 0, 0, 1, 0, light);
            arrowVertex(ms, vc, 8, -2, 0, 0.5f, 0, 0, 1, 0, light);
            arrowVertex(ms, vc, 8, 2, 0, 0.5f, 0.15625f, 0, 1, 0, light);
            arrowVertex(ms, vc, -8, 2, 0, 0, 0.15625f, 0, 1, 0, light);
        }
        ms.popPose();
    }

    private static void arrowVertex(PoseStack ms, VertexConsumer vc, float x, float y, float z, float u, float v,
                                    float nx, float ny, float nz, int light) {
        PoseStack.Pose pose = ms.last();
        vc.addVertex(pose, x, y, z)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, nz, ny);
    }

    private static void renderMarkerPart(PartialModel model, BlockState state, PoseStack ms, VertexConsumer vc,
                                         int light, float scale, float depth) {
        ms.pushPose();
        ms.translate(0, -depth * scale, 0);
        ms.scale(scale, scale, scale);
        ms.translate(-0.5, 0, -0.5);
        CachedBuffers.partial(model, state)
            .light(light)
            .renderInto(ms, vc);
        ms.popPose();
    }

    private static void spawnTorchParticles(SeekerTableBlockEntity table, SeekerTableMap map, Pose3dc pose,
                                            double x, double y, double z, Level level) {
        long tick = level.getGameTime();
        if (tick == map.lastParticleTick || tick % 6 != 0)
            return;
        map.lastParticleTick = tick;
        Vec3 flame = table.localToBlock(x, y + 0.45, z);
        if (pose != null)
            flame = pose.transformPosition(flame);
        level.addParticle(ParticleTypes.SMALL_FLAME, flame.x, flame.y, flame.z, 0, 0.004, 0);
        if (level.random.nextInt(3) == 0)
            level.addParticle(ParticleTypes.SMOKE, flame.x, flame.y + 0.05, flame.z, 0, 0.01, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(SeekerTableBlockEntity table) {
        return true;
    }
}
