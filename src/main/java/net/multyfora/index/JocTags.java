package net.multyfora.index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class JocTags {
    public static final TagKey<Block> SUPER_LIGHT = BlockTags.create(ResourceLocation.fromNamespaceAndPath("sable", "super_light"));
    public static final TagKey<Block> BOUNCY = BlockTags.create(ResourceLocation.fromNamespaceAndPath("sable", "bouncy"));
    public static final TagKey<Block> LIGHT = BlockTags.create(ResourceLocation.fromNamespaceAndPath("sable", "light"));

    public static final TagKey<Item> DESTROYS_ROPE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("simulated", "destroys_rope"));
}
