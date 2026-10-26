package net.multyfora.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.multyfora.client.portable_typewriter.TypewriterHeldLayer;
import net.multyfora.client.portable_typewriter.TypewriterKeyboardModel;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;

import java.util.HashSet;
import java.util.Set;

public class PortableTypewriterItemRenderer extends BlockEntityWithoutLevelRenderer {

    public PortableTypewriterItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int light, int overlay) {
        boolean thirdPerson = displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (thirdPerson && TypewriterHeldLayer.isRenderingTypist()) {
            return;
        }

        Set<Integer> boundKeys = new HashSet<>(PortableTypewriterItem.getAllBoundKeys(stack));
        boolean live = displayContext.firstPerson();
        boolean legends = displayContext != ItemDisplayContext.GUI && displayContext != ItemDisplayContext.GROUND;

        poseStack.pushPose();
        renderCentered(poseStack, bufferSource, light, overlay, boundKeys, live, legends);
        poseStack.popPose();
    }

    public static void renderCentered(PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                                      Set<Integer> boundKeys, boolean live, boolean legends) {
        float scale = 1.0F / TypewriterKeyboardModel.WIDTH;
        poseStack.translate(0.5F, 0.5F - TypewriterKeyboardModel.HEIGHT * scale / 2.0F, 0.5F);
        poseStack.scale(scale, scale, scale);
        TypewriterKeyboardModel.render(poseStack, bufferSource, light, overlay, boundKeys, live, legends);
    }
}
