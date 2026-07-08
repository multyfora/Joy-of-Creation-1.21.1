package net.multyfora.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.crosssail.SymmetricCrossSailBlock;
import net.multyfora.index.JocBlocks;

public class JocBlockStateProvider extends BlockStateProvider {

    public JocBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, AeronauticsJoyofcreation.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (DyeColor color : DyeColor.values()) {
            registerSymmetricCrossSail(JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get(), color);
        }
    }

    private void registerSymmetricCrossSail(SymmetricCrossSailBlock block, DyeColor color) {
        String colorName = color.getSerializedName();
        String modelName = colorName + "_symmetric_cross_sail";

        ModelFile model = models()
                .withExistingParent(modelName, modLoc("block/symmetric_cross_sail_base"))
                .texture("2", modLoc("block/cross_sail/canvas_" + colorName))
                .texture("4", modLoc("block/cross_sail/side_" + colorName))
                .texture("particle", modLoc("block/cross_sail/canvas_" + colorName));

        getVariantBuilder(block)
                .partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Y)
                .addModels(new ConfiguredModel(model))
                .partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.X)
                .addModels(new ConfiguredModel(model, 90, 90, false))
                .partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Z)
                .addModels(new ConfiguredModel(model, 90, 0, false));
    }
}