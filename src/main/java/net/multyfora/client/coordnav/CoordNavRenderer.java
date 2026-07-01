package net.multyfora.client.coordnav;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

import net.multyfora.content.SpaceUtils;
import net.multyfora.content.coordnav.CoordNavBlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Client-side renderer for the Coordinate Navigator: draws a green pointer line
 * from the block toward its configured target position.
 **/
public class CoordNavRenderer implements BlockEntityRenderer<CoordNavBlockEntity> {

    public CoordNavRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
        CoordNavBlockEntity coordNavBlockEntity, float partialTick,
        @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
        int packedLight, int packedOverlay
    ) {
        if( coordNavBlockEntity.getTargetPosition(false) == null ) {
            return;
        }

        Vec3 from = SpaceUtils.getProjectedSelfPos(
            coordNavBlockEntity.getSubLevel(),
            coordNavBlockEntity.getWorldPosition()
        );
        Vec3 target = coordNavBlockEntity.getTargetPosition(true);
        if(target == null) {
            return;
        }

        Vec3 diff = target.subtract(from);
        double dist = diff.length();
        if(dist < 0.5) {
            return;
        }

        Vec3 dir = diff.normalize();
        // Cap the pointer length at 1.5 blocks so it's visible even at long range
        double pointerLen = Math.min(dist, 1.5);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        Vec3 color = new Vec3(0.0, 1.0, 0.0);
        float alpha = 1.0f;

        poseStack.pushPose();
        poseStack.translate(-from.x, -from.y, -from.z);

        Vec3 p0 = from;
        Vec3 p1 = from.add( dir.scale(pointerLen) );

        consumer
            .addVertex(poseStack.last(), (float)p0.x, (float)p0.y, (float)p0.z)
            .setColor( (float)color.x, (float)color.y, (float)color.z, alpha )
            .setNormal(0.0f, 1.0f, 0.0f)
            .setLight(packedLight)
        ;
        consumer
            .addVertex(poseStack.last(), (float)p1.x, (float)p1.y, (float)p1.z)
            .setColor( (float)color.x, (float)color.y, (float)color.z, alpha )
            .setNormal(0.0f, 1.0f, 0.0f)
            .setLight(packedLight)
        ;

        poseStack.popPose();
    }

    // Render even when the block entity is off-screen so the pointer is always visible
    @Override
    public boolean shouldRenderOffScreen(@NotNull CoordNavBlockEntity blockEntity) {
        return true;
    }
}
