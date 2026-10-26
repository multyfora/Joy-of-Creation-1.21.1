package net.multyfora.client.portable_typewriter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;

import java.util.HashSet;
import java.util.Set;

public class TypewriterHeldLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float FORWARD = 8.5F / 16.0F;
    private static final float DOWN = 9.5F / 16.0F;
    private static final float WIDTH = 13.0F / 16.0F;
    private static final float TILT_TOWARDS_PLAYER_DEGREES = 12.0F;

    private static final ThreadLocal<Boolean> renderingTypist = ThreadLocal.withInitial(() -> false);

    public TypewriterHeldLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void setRenderingTypist(boolean value) {
        renderingTypist.set(value);
    }

    public static boolean isRenderingTypist() {
        return renderingTypist.get();
    }

    @Override
    public void render(PoseStack ms, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!TypewriterTypists.isTyping(player)) return;
        ItemStack stack = player.getMainHandItem().getItem() instanceof PortableTypewriterItem
            ? player.getMainHandItem() : player.getOffhandItem();
        if (!(stack.getItem() instanceof PortableTypewriterItem)) return;

        ms.pushPose();
        this.getParentModel().body.translateAndRotate(ms);
        ms.translate(0.0F, DOWN, -FORWARD);
        ms.mulPose(Axis.ZP.rotationDegrees(180.0F));
        ms.mulPose(Axis.XP.rotationDegrees(TILT_TOWARDS_PLAYER_DEGREES));
        float scale = WIDTH / TypewriterKeyboardModel.WIDTH;
        ms.scale(scale, scale, scale);
        ms.translate(0.0F, -TypewriterKeyboardModel.CASE_HEIGHT, 0.0F);
        Set<Integer> boundKeys = new HashSet<>(PortableTypewriterItem.getAllBoundKeys(stack));
        TypewriterKeyboardModel.render(ms, buffers, light, OverlayTexture.NO_OVERLAY, boundKeys, false, true);
        ms.popPose();
    }
}
