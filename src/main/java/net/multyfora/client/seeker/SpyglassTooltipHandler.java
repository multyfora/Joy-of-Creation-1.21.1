package net.multyfora.client.seeker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.index.JocDataComponents;
import net.multyfora.index.SeekerCapturedTarget;

@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class SpyglassTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!stack.is(Items.SPYGLASS)) return;

        SeekerCapturedTarget carried = stack.get(JocDataComponents.SEEKER_CARRIED_TARGET.get());
        if (carried == null) return;

        event.getToolTip().add(
            Component.translatable(
                "item.joc.spyglass.carried_target",
                carried.pos().getX(), carried.pos().getY(), carried.pos().getZ()
            ).withStyle(ChatFormatting.AQUA)
        );
    }
}