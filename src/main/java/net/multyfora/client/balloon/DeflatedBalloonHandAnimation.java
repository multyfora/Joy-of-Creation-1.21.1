package net.multyfora.client.balloon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.multyfora.content.balloon.DeflatedBalloonItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = "joc", value = Dist.CLIENT)
public class DeflatedBalloonHandAnimation {

    private static final float HAND_X = 0.56f, HAND_Y = -0.52f, HAND_Z = -0.72f;
    private static final float MOUTH_X = 0.08f, MOUTH_Y = -0.38f, MOUTH_Z = -0.60f;
    private static final float EQUIP_DROP = 0.6f;
    private static final float TURN_DEGREES = 14f;
    private static final float DEFLATE_TICKS = 6f;
    private static final float FINISHED = 0.97f;

    private static float lastProgress;
    private static float lastRenderTime;
    private static InteractionHand lastHand;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!(event.getItemStack().getItem() instanceof DeflatedBalloonItem balloon))
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        float now = AnimationTickHolder.getRenderTime();
        float progress = BalloonInflation.progress(player, event.getPartialTick());
        if (progress >= 0f) {
            if (player.getUsedItemHand() != event.getHand())
                return;
            lastProgress = progress;
            lastRenderTime = now;
            lastHand = event.getHand();
        } else {
            if (event.getHand() != lastHand || lastProgress >= FINISHED)
                return;
            progress = lastProgress * (1f - (now - lastRenderTime) / DEFLATE_TICKS);
            if (progress <= 0f)
                return;
        }

        event.setCanceled(true);

        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.LEFT ? -1f : 1f;
        float raise = BalloonInflation.raise(progress);

        PoseStack ms = event.getPoseStack();
        ms.pushPose();
        ms.translate(side * Mth.lerp(raise, HAND_X, MOUTH_X),
                Mth.lerp(raise, HAND_Y, MOUTH_Y) - event.getEquipProgress() * EQUIP_DROP,
                Mth.lerp(raise, HAND_Z, MOUTH_Z));
        ms.mulPose(Axis.YP.rotationDegrees(side * -TURN_DEGREES));
        BalloonInflation.renderBlock(ms, event.getMultiBufferSource(), event.getPackedLight(), balloon.getColor(), progress);
        ms.popPose();
    }
}
