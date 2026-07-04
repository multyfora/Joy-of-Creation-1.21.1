package net.multyfora.index;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;

/**
 * Registry holder for all custom items in the mod.
 * Includes BlockItems for each custom block and standalone items for handheld tools.
 **/
public class JocItems {
    // Reference to the main mod's item DeferredRegister for convenience
    public static final DeferredRegister.Items ITEMS = AeronauticsJoyofcreation.ITEMS;

    // Simple block items that just place the corresponding block
    public static final DeferredItem<BlockItem> BALLOON = ITEMS.registerSimpleBlockItem(
            "balloon", JocBlocks.BALLOON);
    public static final DeferredItem<BlockItem> COORD_NAV = ITEMS.registerSimpleBlockItem(
            "coord_navigator", JocBlocks.COORD_NAV);

    // Custom handheld items (stack size 1) with special right-click behaviours
    public static final DeferredItem<PortableTypewriterItem> PORTABLE_TYPEWRITER = ITEMS.register("portable_typewriter",
            () -> new PortableTypewriterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<PortableThrottleItem> PORTABLE_THROTTLE = ITEMS.register("portable_throttle",
            () -> new PortableThrottleItem(new Item.Properties().stacksTo(1)));

    // Dummy method to trigger static initialisation
    public static void register() {}
}
