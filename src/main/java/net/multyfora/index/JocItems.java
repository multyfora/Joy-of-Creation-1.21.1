package net.multyfora.index;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;
import net.multyfora.content.seeker.SeekerBlockItem;

public class JocItems {
    public static final DeferredRegister.Items ITEMS = AeronauticsJoyofcreation.ITEMS;

    public static final DeferredItem<SeekerBlockItem> SEEKER = ITEMS.register("seeker",
            () -> new SeekerBlockItem(JocBlocks.SEEKER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SHATTER_ASSEMBLER = ITEMS.registerSimpleBlockItem(
            "shatter_assembler", () -> JocBlocks.SHATTER_ASSEMBLER.get());
    public static final DeferredItem<BlockItem> GYROSCOPIC_SEAT = ITEMS.registerSimpleBlockItem(
            "gyroscopic_seat", () -> JocBlocks.GYROSCOPIC_SEAT.get());

    public static final DeferredItem<Item> DEFLATED_BALLOON = ITEMS.register("deflated_balloon",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<PortableTypewriterItem> PORTABLE_TYPEWRITER = ITEMS.register("portable_typewriter",
            () -> new PortableTypewriterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<PortableThrottleItem> PORTABLE_THROTTLE = ITEMS.register("portable_throttle",
            () -> new PortableThrottleItem(new Item.Properties().stacksTo(1)));

    public static void register() {}
}