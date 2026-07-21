package net.multyfora.content.shears_cut;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.multyfora.AeronauticsJoyofcreation;
import org.joml.Vector3f;

@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class ShearsCutRenderer {

    private static final RenderType CUT_PLANE = RenderType.create(
            "joc_shears_cut_plane",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    private static final RandomSource RNG = RandomSource.create();
    private static final float BORDER_THICKNESS = 0.06f;
    private static final float CORE_LINE_THICKNESS = 0.015f;
    private static final float GLOW_MARGIN_INNER = 0.15f;
    private static final float GLOW_MARGIN_OUTER = 0.35f;
    private static final float GRID_SPACING = 0.5f;
    private static final float GRID_LINE_THICKNESS = 0.008f;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        renderFlash(event, mc);

        if (ShearsCutState.getMode() != ShearsCutState.Mode.PLACING) return;

        boolean holdingShears = mc.player.getMainHandItem().is(net.minecraft.world.item.Items.SHEARS)
                || mc.player.getOffhandItem().is(net.minecraft.world.item.Items.SHEARS);
        if (!holdingShears) {
            ShearsCutState.reset();
            return;
        }

        BlockPos p1 = ShearsCutState.getPoint1();
        if (p1 == null) return;

        SubLevel sub = Sable.HELPER.getContaining(mc.level, p1);
        if (sub == null || sub.isRemoved()) {
            ShearsCutState.reset();
            return;
        }

        BlockPos p2 = ShearsCutState.getCursorPos();
        if (p2 == null) return;

        Direction orientation = ShearsCutState.getOrientation();
        if (orientation == null) return;

        ShearsCutState.PlaneGeometry geom = ShearsCutState.computeGeometry(p1, p2, orientation);
        Vec3 u = geom.u();
        Vec3 v = geom.v();
        Vec3 center = geom.center().add(Vec3.atLowerCornerOf(orientation.getNormal()).scale(0.01));
        double hu = geom.hu();
        double hv = geom.hv();

        Vec3 camPos = event.getCamera().getPosition();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(CUT_PLANE);
        PoseStack.Pose pose = event.getPoseStack().last();

        float time = (System.currentTimeMillis() % 6000L) / 6000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * Math.PI * 2.0);
        float fastPulse = 0.5f + 0.5f * (float) Math.sin(time * Math.PI * 8.0);

        renderRect(consumer, pose, mc, camPos, center, u, v, hu + GLOW_MARGIN_OUTER, hv + GLOW_MARGIN_OUTER,
                1.0f, 0.12f, 0.08f, 0.035f + 0.02f * pulse);
        renderRect(consumer, pose, mc, camPos, center, u, v, hu + GLOW_MARGIN_INNER, hv + GLOW_MARGIN_INNER,
                1.0f, 0.2f, 0.1f, 0.05f + 0.04f * pulse);

        renderRect(consumer, pose, mc, camPos, center, u, v, hu - BORDER_THICKNESS, hv - BORDER_THICKNESS,
                0.15f, 0.02f, 0.02f, 0.08f);

        renderGrid(consumer, pose, mc, camPos, center, u, v, hu - BORDER_THICKNESS, hv - BORDER_THICKNESS, time);

        renderShimmerRect(consumer, pose, mc, camPos, center, u, v, hu - BORDER_THICKNESS, hv - BORDER_THICKNESS, time);

        renderFrame(consumer, pose, mc, camPos, center, u, v, hu, hv, BORDER_THICKNESS,
                0.06f, 0.02f, 0.02f, 0.9f);
        renderFrame(consumer, pose, mc, camPos, center, u, v, hu - BORDER_THICKNESS, hv - BORDER_THICKNESS, CORE_LINE_THICKNESS,
                1.0f, 0.35f + 0.25f * fastPulse, 0.15f, 0.9f);

        renderCornerBrackets(consumer, pose, mc, camPos, center, u, v, hu, hv, fastPulse);

        mc.renderBuffers().bufferSource().endBatch(CUT_PLANE);

        spawnMotes(mc, center, u, v, hu, hv);
    }

    private static void renderFlash(RenderLevelStageEvent event, Minecraft mc) {
        float progress = ShearsCutState.getFlashProgress();
        if (progress < 0) return;

        Vec3 center = ShearsCutState.getFlashWorldCenter();
        Vec3 u = ShearsCutState.getFlashWorldU();
        Vec3 v = ShearsCutState.getFlashWorldV();
        if (center == null || u == null || v == null) return;

        double hu = ShearsCutState.getFlashHu();
        double hv = ShearsCutState.getFlashHv();

        Vec3 camPos = event.getCamera().getPosition();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(CUT_PLANE);
        PoseStack.Pose pose = event.getPoseStack().last();

        float meetT = 0.4f;
        if (progress < meetT) {
            float t = progress / meetT;
            double curHu = hu * (1f - t);
            double curHv = hv * (1f - t);
            double thickness = 0.08 + 0.10 * (1f - t);
            float alpha = 1f - t * 0.3f;

            renderFlashFrame(consumer, pose, center, u, v, camPos, curHu, curHv, thickness + 0.15,
                    1f, 0.6f, 0.45f, 0.18f * alpha);
            renderFlashFrame(consumer, pose, center, u, v, camPos, curHu, curHv, thickness,
                    1f, 0.8f, 0.7f, 0.8f * alpha);
        } else {
            float t = (progress - meetT) / (1f - meetT);
            float alpha = (1f - t);
            alpha = alpha * alpha;

            double burstHu = Math.min(hu, hv) * 0.25 * t + 0.05;
            double burstHv = burstHu;

            Vec3 c0 = center.add(u.scale(-burstHu)).add(v.scale(-burstHv)).subtract(camPos);
            Vec3 c1 = center.add(u.scale(burstHu)).add(v.scale(-burstHv)).subtract(camPos);
            Vec3 c2 = center.add(u.scale(burstHu)).add(v.scale(burstHv)).subtract(camPos);
            Vec3 c3 = center.add(u.scale(-burstHu)).add(v.scale(burstHv)).subtract(camPos);
            quad(consumer, pose, c0, c1, c2, c3, 1f, 0.75f, 0.65f, 0.85f * alpha);

            double glowHu = burstHu + 0.4 * alpha;
            double glowHv = burstHv + 0.4 * alpha;
            Vec3 g0 = center.add(u.scale(-glowHu)).add(v.scale(-glowHv)).subtract(camPos);
            Vec3 g1 = center.add(u.scale(glowHu)).add(v.scale(-glowHv)).subtract(camPos);
            Vec3 g2 = center.add(u.scale(glowHu)).add(v.scale(glowHv)).subtract(camPos);
            Vec3 g3 = center.add(u.scale(-glowHu)).add(v.scale(glowHv)).subtract(camPos);
            quad(consumer, pose, g0, g1, g2, g3, 1f, 0.6f, 0.45f, 0.22f * alpha);
        }

        mc.renderBuffers().bufferSource().endBatch(CUT_PLANE);
    }

    private static void renderFlashFrame(VertexConsumer consumer, PoseStack.Pose pose, Vec3 center, Vec3 u, Vec3 v, Vec3 camPos,
                                         double hu, double hv, double thickness,
                                         float r, float g, float b, float a) {
        renderFlashStrip(consumer, pose, center, u, v, camPos, -hu, hu, hv - thickness, hv, r, g, b, a);
        renderFlashStrip(consumer, pose, center, u, v, camPos, -hu, hu, -hv, -hv + thickness, r, g, b, a);
        renderFlashStrip(consumer, pose, center, u, v, camPos, -hu, -hu + thickness, -hv, hv, r, g, b, a);
        renderFlashStrip(consumer, pose, center, u, v, camPos, hu - thickness, hu, -hv, hv, r, g, b, a);
    }

    private static void renderFlashStrip(VertexConsumer consumer, PoseStack.Pose pose, Vec3 center, Vec3 u, Vec3 v, Vec3 camPos,
                                         double u0, double u1, double v0, double v1,
                                         float r, float g, float b, float a) {
        Vec3 c0 = center.add(u.scale(u0)).add(v.scale(v0)).subtract(camPos);
        Vec3 c1 = center.add(u.scale(u1)).add(v.scale(v0)).subtract(camPos);
        Vec3 c2 = center.add(u.scale(u1)).add(v.scale(v1)).subtract(camPos);
        Vec3 c3 = center.add(u.scale(u0)).add(v.scale(v1)).subtract(camPos);
        quad(consumer, pose, c0, c1, c2, c3, r, g, b, a);
    }

    private static Vec3 toWorld(Minecraft mc, Vec3 camPos, Vec3 local) {
        Vec3 world = Sable.HELPER.projectOutOfSubLevel(mc.level, local);
        return world.subtract(camPos);
    }

    private static void renderRect(VertexConsumer consumer, PoseStack.Pose pose, Minecraft mc, Vec3 camPos,
                                   Vec3 center, Vec3 u, Vec3 v, double hu, double hv,
                                   float r, float g, float b, float a) {
        Vec3 c0 = center.add(u.scale(-hu)).add(v.scale(-hv));
        Vec3 c1 = center.add(u.scale(hu)).add(v.scale(-hv));
        Vec3 c2 = center.add(u.scale(hu)).add(v.scale(hv));
        Vec3 c3 = center.add(u.scale(-hu)).add(v.scale(hv));
        quad(consumer, pose, toWorld(mc, camPos, c0), toWorld(mc, camPos, c1),
                toWorld(mc, camPos, c2), toWorld(mc, camPos, c3), r, g, b, a);
    }

    private static void renderShimmerRect(VertexConsumer consumer, PoseStack.Pose pose, Minecraft mc, Vec3 camPos,
                                          Vec3 center, Vec3 u, Vec3 v, double hu, double hv, float sweep) {
        Vec3 c0 = center.add(u.scale(-hu)).add(v.scale(-hv));
        Vec3 c1 = center.add(u.scale(hu)).add(v.scale(-hv));
        Vec3 c2 = center.add(u.scale(hu)).add(v.scale(hv));
        Vec3 c3 = center.add(u.scale(-hu)).add(v.scale(hv));

        float a0 = shimmerAlpha(0f, sweep);
        float a1 = shimmerAlpha(0.33f, sweep);
        float a2 = shimmerAlpha(0.66f, sweep);
        float a3 = shimmerAlpha(1f, sweep);

        Vec3 w0 = toWorld(mc, camPos, c0), w1 = toWorld(mc, camPos, c1),
                w2 = toWorld(mc, camPos, c2), w3 = toWorld(mc, camPos, c3);

        consumer.addVertex(pose, (float) w0.x, (float) w0.y, (float) w0.z).setColor(1.0f, 0.15f, 0.1f, a0);
        consumer.addVertex(pose, (float) w1.x, (float) w1.y, (float) w1.z).setColor(1.0f, 0.2f, 0.1f, a1);
        consumer.addVertex(pose, (float) w2.x, (float) w2.y, (float) w2.z).setColor(1.0f, 0.15f, 0.1f, a2);
        consumer.addVertex(pose, (float) w3.x, (float) w3.y, (float) w3.z).setColor(1.0f, 0.2f, 0.1f, a3);
    }

    private static float shimmerAlpha(float phase, float sweep) {
        float d = Math.abs(phase - sweep);
        d = Math.min(d, 1f - d);
        float bump = Math.max(0f, 1f - d * 4f);
        return 0.08f + 0.14f * bump;
    }

    private static void renderGrid(VertexConsumer consumer, PoseStack.Pose pose, Minecraft mc, Vec3 camPos,
                                   Vec3 center, Vec3 u, Vec3 v, double hu, double hv, float time) {
        float scroll = (time * 0.4f) % GRID_SPACING;
        float r = 1.0f, g = 0.3f, b = 0.15f, a = 0.10f;

        for (float x = (float) -hu + scroll; x < hu; x += GRID_SPACING) {
            renderStrip(consumer, pose, mc, camPos, center, u, v,
                    x - GRID_LINE_THICKNESS, x + GRID_LINE_THICKNESS, -hv, hv, r, g, b, a);
        }
        for (float y = (float) -hv + scroll; y < hv; y += GRID_SPACING) {
            renderStrip(consumer, pose, mc, camPos, center, u, v,
                    -hu, hu, y - GRID_LINE_THICKNESS, y + GRID_LINE_THICKNESS, r, g, b, a);
        }
    }

    private static void renderFrame(VertexConsumer consumer, PoseStack.Pose pose, Minecraft mc, Vec3 camPos,
                                    Vec3 center, Vec3 u, Vec3 v, double hu, double hv, double thickness,
                                    float r, float g, float b, float a) {
        renderStrip(consumer, pose, mc, camPos, center, u, v, -hu, hu, hv - thickness, hv, r, g, b, a);
        renderStrip(consumer, pose, mc, camPos, center, u, v, -hu, hu, -hv, -hv + thickness, r, g, b, a);
        renderStrip(consumer, pose, mc, camPos, center, u, v, -hu, -hu + thickness, -hv, hv, r, g, b, a);
        renderStrip(consumer, pose, mc, camPos, center, u, v, hu - thickness, hu, -hv, hv, r, g, b, a);
    }

    private static void renderCornerBrackets(VertexConsumer consumer, PoseStack.Pose pose, Minecraft mc, Vec3 camPos,
                                             Vec3 center, Vec3 u, Vec3 v, double hu, double hv, float pulse) {
        double armLen = Math.min(hu, hv) * 0.35;
        double armThick = 0.03 + 0.015 * pulse;
        float r = 1.0f, g = 0.4f + 0.3f * pulse, b = 0.2f, a = 0.95f;

        double[][] corners = { {-hu, -hv, 1, 1}, {hu, -hv, -1, 1}, {hu, hv, -1, -1}, {-hu, hv, 1, -1} };
        for (double[] c : corners) {
            double cu = c[0], cv = c[1], su = c[2], sv = c[3];
            renderStrip(consumer, pose, mc, camPos, center, u, v,
                    cu, cu + su * armLen, cv, cv + sv * armThick, r, g, b, a);
            renderStrip(consumer, pose, mc, camPos, center, u, v,
                    cu, cu + su * armThick, cv, cv + sv * armLen, r, g, b, a);
        }
    }

    private static void renderStrip(VertexConsumer consumer, PoseStack.Pose pose, Minecraft mc, Vec3 camPos,
                                    Vec3 center, Vec3 u, Vec3 v, double u0, double u1, double v0, double v1,
                                    float r, float g, float b, float a) {
        Vec3 c0 = center.add(u.scale(u0)).add(v.scale(v0));
        Vec3 c1 = center.add(u.scale(u1)).add(v.scale(v0));
        Vec3 c2 = center.add(u.scale(u1)).add(v.scale(v1));
        Vec3 c3 = center.add(u.scale(u0)).add(v.scale(v1));
        quad(consumer, pose, toWorld(mc, camPos, c0), toWorld(mc, camPos, c1),
                toWorld(mc, camPos, c2), toWorld(mc, camPos, c3), r, g, b, a);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                             float r, float g, float b, float a) {
        consumer.addVertex(pose, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r, g, b, a);
        consumer.addVertex(pose, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a);
        consumer.addVertex(pose, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a);
        consumer.addVertex(pose, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a);
    }

    private static void spawnMotes(Minecraft mc, Vec3 center, Vec3 u, Vec3 v, double hu, double hv) {
        if (mc.level == null) return;
        if (RNG.nextFloat() > 0.35f) return;

        double su = (RNG.nextDouble() * 2 - 1) * hu;
        double sv = (RNG.nextDouble() * 2 - 1) * hv;
        Vec3 local = center.add(u.scale(su)).add(v.scale(sv));
        Vec3 world = Sable.HELPER.projectOutOfSubLevel(mc.level, local);

        float roll = RNG.nextFloat();
        Vector3f color;
        float scale;
        if (roll < 0.08f) {
            color = new Vector3f(1.0f, 0.95f, 0.85f);
            scale = 0.3f + RNG.nextFloat() * 0.2f;
        } else if (roll < 0.5f) {
            color = new Vector3f(1.0f, 0.25f, 0.15f);
            scale = 0.55f + RNG.nextFloat() * 0.35f;
        } else {
            color = new Vector3f(1.0f, 0.55f, 0.2f);
            scale = 0.5f + RNG.nextFloat() * 0.3f;
        }
        DustParticleOptions dust = new DustParticleOptions(color, scale);

        mc.level.addParticle(dust, world.x, world.y, world.z,
                (RNG.nextDouble() - 0.5) * 0.01, 0.006 + RNG.nextDouble() * 0.006, (RNG.nextDouble() - 0.5) * 0.01);
    }
}