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
    private static final float GLOW_MARGIN = 0.25f;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

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

        Vec3 u = ShearsCutState.axisU(orientation);
        Vec3 v = ShearsCutState.axisV(orientation);
        Vec3 origin = ShearsCutState.planeOrigin(p1, orientation);

        int minX = Math.min(p1.getX(), p2.getX());
        int maxX = Math.max(p1.getX(), p2.getX()) + 1;
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;
        int minY = Math.min(p1.getY(), p2.getY());
        int maxY = Math.max(p1.getY(), p2.getY()) + 1;

        Vec3 center, u2 = u, v2 = v;
        double hu, hv;
        switch (orientation) {
            case UP, DOWN -> {
                center = new Vec3((minX + maxX) / 2.0, origin.y, (minZ + maxZ) / 2.0);
                hu = (maxX - minX) / 2.0; hv = (maxZ - minZ) / 2.0;
            }
            case NORTH, SOUTH -> {
                center = new Vec3((minX + maxX) / 2.0, (minY + maxY) / 2.0, origin.z);
                hu = (maxX - minX) / 2.0; hv = (maxY - minY) / 2.0;
            }
            default -> {
                center = new Vec3(origin.x, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
                hu = (maxZ - minZ) / 2.0; hv = (maxY - minY) / 2.0;
            }
        }
        center = center.add(Vec3.atLowerCornerOf(orientation.getNormal()).scale(0.01));
        double margin = 0.15;
        hu += margin;
        hv += margin;

        Vec3 camPos = event.getCamera().getPosition();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(CUT_PLANE);
        PoseStack.Pose pose = event.getPoseStack().last();

        float time = (System.currentTimeMillis() % 4000L) / 4000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * Math.PI * 2.0);

        float glowAlpha = 0.05f + 0.03f * pulse;
        renderRect(consumer, pose, mc, camPos, center, u, v, hu + GLOW_MARGIN, hv + GLOW_MARGIN,
                1.0f, 0.15f, 0.1f, glowAlpha);

        renderFrame(consumer, pose, mc, camPos, center, u, v, hu, hv, BORDER_THICKNESS,
                0.08f, 0.02f, 0.02f, 0.85f);

        renderShimmerRect(consumer, pose, mc, camPos, center, u, v, hu - BORDER_THICKNESS, hv - BORDER_THICKNESS, time);

        mc.renderBuffers().bufferSource().endBatch(CUT_PLANE);

        spawnMotes(mc, center, u, v, hu, hv);
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

        consumer.addVertex(pose, (float) w0.x, (float) w0.y, (float) w0.z).setColor(1.0f, 0.1f, 0.1f, a0);
        consumer.addVertex(pose, (float) w1.x, (float) w1.y, (float) w1.z).setColor(1.0f, 0.15f, 0.1f, a1);
        consumer.addVertex(pose, (float) w2.x, (float) w2.y, (float) w2.z).setColor(1.0f, 0.1f, 0.1f, a2);
        consumer.addVertex(pose, (float) w3.x, (float) w3.y, (float) w3.z).setColor(1.0f, 0.15f, 0.1f, a3);
    }

    private static float shimmerAlpha(float phase, float sweep) {
        float d = Math.abs(phase - sweep);
        d = Math.min(d, 1f - d);
        float bump = Math.max(0f, 1f - d * 4f);
        return 0.10f + 0.18f * bump;
    }

    private static void renderFrame(VertexConsumer consumer, PoseStack.Pose pose, Minecraft mc, Vec3 camPos,
                                    Vec3 center, Vec3 u, Vec3 v, double hu, double hv, double thickness,
                                    float r, float g, float b, float a) {
        renderStrip(consumer, pose, mc, camPos, center, u, v, -hu, hu, hv - thickness, hv, r, g, b, a);
        renderStrip(consumer, pose, mc, camPos, center, u, v, -hu, hu, -hv, -hv + thickness, r, g, b, a);
        renderStrip(consumer, pose, mc, camPos, center, u, v, -hu, -hu + thickness, -hv, hv, r, g, b, a);
        renderStrip(consumer, pose, mc, camPos, center, u, v, hu - thickness, hu, -hv, hv, r, g, b, a);
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

        boolean warm = RNG.nextBoolean();
        Vector3f color = warm ? new Vector3f(1.0f, 0.25f, 0.15f) : new Vector3f(1.0f, 0.55f, 0.2f);
        float scale = 0.55f + RNG.nextFloat() * 0.35f;
        DustParticleOptions dust = new DustParticleOptions(color, scale);

        mc.level.addParticle(dust, world.x, world.y, world.z,
                (RNG.nextDouble() - 0.5) * 0.01, 0.006 + RNG.nextDouble() * 0.006, (RNG.nextDouble() - 0.5) * 0.01);
    }
}