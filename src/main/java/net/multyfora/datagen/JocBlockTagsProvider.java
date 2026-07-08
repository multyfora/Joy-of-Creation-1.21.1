package net.multyfora.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.index.JocBlocks;

import java.util.concurrent.CompletableFuture;

public class JocBlockTagsProvider extends BlockTagsProvider {

    public JocBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, AeronauticsJoyofcreation.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var mineableAxe = tag(BlockTags.MINEABLE_WITH_AXE);

        for (DyeColor color : DyeColor.values()) {
            mineableAxe.add(JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get());
        }
    }
}