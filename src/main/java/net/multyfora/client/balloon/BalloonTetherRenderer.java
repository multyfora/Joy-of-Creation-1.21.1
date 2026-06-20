package net.multyfora.client.balloon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import net.multyfora.content.balloon.BalloonBlockEntity;

// Client-side renderer for balloons: draws a brown tether line from the balloon
// to its connected connector block. The line is segmented for a rope-like appearance.
public class BalloonTetherRenderer implements BlockEntityRenderer<BalloonBlockEntity> {
    // Brown color for the rope
    private static final Vec3 ROPE_BROWN = new Vec3(0.5, 0.35, 0.15);

    public BalloonTetherRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(BalloonBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockPos connectorPos = be.getConnectorPos();
        if (connectorPos == null) return;

        Vec3 from = Vec3.atCenterOf(be.getBlockPos());
        Vec3 to = Vec3.atCenterOf(connectorPos);
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        if (dist < 0.5) return;

        // Number of segments scales with distance for a consistent look
        float segments = (float) Math.ceil(dist * 2);
        float segLen = 1.0f / segments;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        Vec3 color = ROPE_BROWN;
        float alpha = 0.6f;

        // Offset the pose stack so we can draw in world coordinates
        poseStack.pushPose();
        poseStack.translate(-from.x, -from.y, -from.z);

        // Draw line segments from balloon to connector
        for (int i = 0; i < segments; i++) {
            float t0 = i * segLen;
            float t1 = (i + 1) * segLen;
            Vec3 p0 = from.add(diff.scale(t0));
            Vec3 p1 = from.add(diff.scale(t1));

            consumer.addVertex(poseStack.last(), (float) p0.x, (float) p0.y, (float) p0.z)
                    .setColor((float) color.x, (float) color.y, (float) color.z, alpha)
                    .setNormal(0.0f, 1.0f, 0.0f)
                    .setLight(packedLight);
            consumer.addVertex(poseStack.last(), (float) p1.x, (float) p1.y, (float) p1.z)
                    .setColor((float) color.x, (float) color.y, (float) color.z, alpha)
                    .setNormal(0.0f, 1.0f, 0.0f)
                    .setLight(packedLight);
        }

        poseStack.popPose();
    }
}
