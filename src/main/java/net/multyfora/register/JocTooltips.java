package net.multyfora.register;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.world.item.Item;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class JocTooltips {
    private static final TooltipModifier UNFINISHED = event -> event.getToolTip().add(1,
            net.minecraft.network.chat.Component.translatable("joc.tooltip.unfinished")
                    .withStyle(net.minecraft.ChatFormatting.RED));

    public static void register() {
        List<Item> items = new ArrayList<>();
        items.add(JocItems.SEEKER.get());
        items.add(JocItems.SHATTER_ASSEMBLER.get());
        items.add(JocItems.GYROSCOPIC_SEAT.get());
        items.add(JocItems.PORTABLE_TYPEWRITER.get());
        items.add(JocItems.PORTABLE_THROTTLE.get());
        JocItems.DEFLATED_BALLOONS.values().forEach(item -> items.add(item.get()));
        JocBlocks.BALLOON_ITEMS.values().forEach(item -> items.add(item.get()));
        JocBlocks.SYMMETRIC_CROSS_SAIL_ITEMS.values().forEach(item -> items.add(item.get()));

        for (Item item : items) {
            TooltipModifier base = new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE);
            TooltipModifier modifier = isUnfinished(item) ? base.andThen(UNFINISHED) : base;
            TooltipModifier.REGISTRY.register(item, modifier);
        }
        AeronauticsJoyofcreation.LOGGER.info("[JocTooltips] registered {} modifiers; seeker key={}, exists={}", items.size(),
                AeronauticsJoyofcreation.MODID + ":block.joc.seeker.tooltip.summary",
                net.minecraft.client.resources.language.I18n.exists("block.joc.seeker.tooltip.summary"));
    }

    private static boolean isUnfinished(Item item) {
        return item == JocItems.SHATTER_ASSEMBLER.get()
                || item == JocItems.GYROSCOPIC_SEAT.get()
                || item == JocItems.PORTABLE_TYPEWRITER.get();
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void debugTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (event.getItemStack().getItemHolder().getKey().location().getNamespace().equals(AeronauticsJoyofcreation.MODID)) {
            Item item = event.getItemStack().getItem();
            TooltipModifier modifier = TooltipModifier.REGISTRY.get(item);
            boolean exists = net.minecraft.client.resources.language.I18n.exists(item.getDescriptionId() + ".tooltip.summary");
            AeronauticsJoyofcreation.LOGGER.info("[JocTooltips] tooltip event for {} modifier={} descKeyExists={} lines={} shift={}",
                    item.getDescriptionId(), modifier != null, exists, event.getToolTip().size(),
                    net.minecraft.client.gui.screens.Screen.hasShiftDown());
        }
    }

    private static void registerModifier(Item item) {
        TooltipModifier.REGISTRY.register(item,
                new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));
    }
}
