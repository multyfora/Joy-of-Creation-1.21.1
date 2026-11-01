package net.multyfora.register;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.world.item.Item;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;

import java.util.ArrayList;
import java.util.List;

public class JocTooltips {
    public static void register() {
        List<Item> items = new ArrayList<>();
        items.add(JocItems.SEEKER.get());
        items.add(JocItems.MAP_PIN.get());
        items.add(JocItems.SHATTER_ASSEMBLER.get());
        items.add(JocItems.GYROSCOPIC_SEAT.get());
        items.add(JocItems.PORTABLE_TYPEWRITER.get());
        items.add(JocItems.PORTABLE_THROTTLE.get());
        items.add(JocItems.CHAIN_COUPLING.get());
        JocItems.DEFLATED_BALLOONS.values().forEach(item -> items.add(item.get()));
        JocBlocks.BALLOON_ITEMS.values().forEach(item -> items.add(item.get()));
        JocBlocks.SYMMETRIC_CROSS_SAIL_ITEMS.values().forEach(item -> items.add(item.get()));

        for (Item item : items) {
            TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));
        }
    }
}
