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

    // Simple block items that just place the corresponding block.
    // IMPORTANT: use a Supplier lambda here, NOT JocBlocks.BALLOON directly — passing the
    // DeferredBlock field itself forces eager cross-class static initialization, which
    // deadlocks against JocBlocks's own static block referencing JocItems.ITEMS.
    public static final DeferredItem<BlockItem> BALLOON = ITEMS.registerSimpleBlockItem(
            "balloon", () -> JocBlocks.BALLOON.get());
    public static final DeferredItem<BlockItem> SEEKER = ITEMS.registerSimpleBlockItem(
            "seeker", () -> JocBlocks.SEEKER.get());
    public static final DeferredItem<BlockItem> SHATTER_ASSEMBLER = ITEMS.registerSimpleBlockItem(
            "shatter_assembler", () -> JocBlocks.SHATTER_ASSEMBLER.get());

    // Custom handheld items (stack size 1) with special right-click behaviours
    public static final DeferredItem<PortableTypewriterItem> PORTABLE_TYPEWRITER = ITEMS.register("portable_typewriter",
            () -> new PortableTypewriterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<PortableThrottleItem> PORTABLE_THROTTLE = ITEMS.register("portable_throttle",
            () -> new PortableThrottleItem(new Item.Properties().stacksTo(1)));

    // Dummy method to trigger static initialisation
    public static void register() {}
}