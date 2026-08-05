package net.multyfora.client.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.multyfora.index.JocBlocks;

public class AnimatedBalloon {

    public static void draw(GuiGraphics graphics, DyeColor color, int x, int y) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        AllGuiTextures.JEI_SHADOW.render(graphics, -16, 13);
        poseStack.translate(-2, -15, 0);
        double pulse = (22 + 1.5 * Math.sin(AnimationTickHolder.getRenderTime() / 6.0)) / 16.0;
        GuiGameElement.of(new ItemStack(JocBlocks.BALLOON_ITEMS.get(color).get()))
                .scale(pulse)
                .render(graphics);
        poseStack.popPose();
    }
}
