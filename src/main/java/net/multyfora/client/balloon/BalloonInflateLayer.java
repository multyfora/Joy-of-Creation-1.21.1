package net.multyfora.client.balloon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.multyfora.content.balloon.DeflatedBalloonItem;

public class BalloonInflateLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float MOUTH_DOWN = 2.5F / 16.0F;
    private static final float MOUTH_FORWARD = 4.5F / 16.0F;

    public BalloonInflateLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ms, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        float progress = BalloonInflation.progress(player, partialTick);
        if (progress < 0F || !(player.getUseItem().getItem() instanceof DeflatedBalloonItem balloon)) return;

        ms.pushPose();
        this.getParentModel().getHead().translateAndRotate(ms);
        ms.translate(0.0F, -MOUTH_DOWN, -MOUTH_FORWARD);
        ms.mulPose(Axis.ZP.rotationDegrees(180.0F));
        BalloonInflation.renderBlock(ms, buffers, light, balloon.getColor(), progress);
        ms.popPose();
    }
}
