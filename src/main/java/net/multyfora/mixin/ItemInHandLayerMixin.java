package net.multyfora.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.multyfora.content.balloon.DeflatedBalloonItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void joc$hideBalloonBeingBlown(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                           PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (stack.getItem() instanceof DeflatedBalloonItem && entity.isUsingItem() && entity.getUseItem() == stack)
            ci.cancel();
    }
}
