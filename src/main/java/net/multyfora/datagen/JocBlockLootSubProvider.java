package net.multyfora.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

import net.multyfora.index.JocBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JocBlockLootSubProvider extends BlockLootSubProvider {

    protected JocBlockLootSubProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (DyeColor color : DyeColor.values()) {
            dropSelf(JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get());
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            blocks.add(JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get());
        }
        return blocks;
    }
}