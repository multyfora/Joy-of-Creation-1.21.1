package net.multyfora.client.seeker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.index.JocDataComponents;
import net.multyfora.index.JocItems;

@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class SeekerTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!stack.is(JocItems.SEEKER.asItem())) return;

        var captured = stack.get(JocDataComponents.LINKED_SEEKER_POS.get());
        if (captured == null) return;

        event.getToolTip().add(
            Component.translatable(
                "item.joc.seeker.captured_target",
                captured.getX(), captured.getY(), captured.getZ()
            ).withStyle(ChatFormatting.AQUA)
        );
    }
}