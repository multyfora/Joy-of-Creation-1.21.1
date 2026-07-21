package net.multyfora.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.index.JocBlocks;

public class JocItemModelProvider extends ItemModelProvider {

    public JocItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, AeronauticsJoyofcreation.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (DyeColor color : DyeColor.values()) {
            String colorName = color.getSerializedName();
            withExistingParent(colorName + "_balloon", modLoc("block/balloon"));
        }

        for (DyeColor color : DyeColor.values()) {
            String colorName = color.getSerializedName();
            withExistingParent(colorName + "_symmetric_cross_sail", modLoc("block/" + colorName + "_symmetric_cross_sail"));
        }
    }
}