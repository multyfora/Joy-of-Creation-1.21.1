package net.multyfora.client.balloon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.multyfora.content.balloon.DeflatedBalloonItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = "joc", value = Dist.CLIENT)
public class DeflatedBalloonHandAnimation {

    private static final float APPROACH_FRACTION = 0.22f;
    private static final float INFLATE_FRACTION = -0.28f;
    private static final float MAX_INFLATE_SCALE = 1.35f;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof DeflatedBalloonItem))
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isUsingItem()
                || !mc.player.getUseItem().is(stack.getItem())
                || mc.player.getUsedItemHand() != event.getHand())
            return;

        float duration = stack.getUseDuration(mc.player);
        float ticksUsing = duration - mc.player.getUseItemRemainingTicks()
                + AnimationTickHolder.getPartialTicks();
        float progress = duration > 0 ? Mth.clamp(ticksUsing / duration, 0f, 1f) : 1f;

        float approach = smoothstep(Mth.clamp(progress / APPROACH_FRACTION, 0f, 1f));

        float inflateStart = 1f - INFLATE_FRACTION;
        float inflate = progress <= inflateStart ? 0f
                : smoothstep(Mth.clamp((progress - inflateStart) / INFLATE_FRACTION, 0f, 1f));
        float scale = Mth.lerp(inflate, 1.0f, MAX_INFLATE_SCALE);

        PoseStack poseStack = event.getPoseStack();
        poseStack.translate(-0.5f, 0.30f * approach, -0.25f * approach);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90f));
        poseStack.mulPose(Axis.XP.rotationDegrees(90f * approach));
//        poseStack.scale(scale, scale, scale);
    }

    private static float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }
}