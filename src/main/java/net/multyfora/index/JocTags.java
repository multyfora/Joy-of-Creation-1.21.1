package net.multyfora.index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Holder for custom block and item tags used throughout the mod.
 * Tags reference conventions from the Sable and Simulated mods for cross-mod compatibility.
 **/
public class JocTags {
    // sable:super_light - blocks that are treated as very lightweight for ship/sublevel physics
    public static final TagKey<Block> SUPER_LIGHT = BlockTags.create(ResourceLocation.fromNamespaceAndPath("sable", "super_light"));
    // sable:bouncy - blocks that have bounce/spring physics in sublevels
    public static final TagKey<Block> BOUNCY = BlockTags.create(ResourceLocation.fromNamespaceAndPath("sable", "bouncy"));
    // sable:light - blocks that are considered light for sublevel weight calculations
    public static final TagKey<Block> LIGHT = BlockTags.create(ResourceLocation.fromNamespaceAndPath("sable", "light"));

    // simulated:destroys_rope - items that will cut/destroy ropes when used on them
    public static final TagKey<Item> DESTROYS_ROPE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("simulated", "destroys_rope"));
}
