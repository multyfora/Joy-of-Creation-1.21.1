package net.multyfora.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.crosssail.SymmetricCrossSailBlock;
import net.multyfora.content.gyroseat.GyroscopicSeatBlock;
import net.multyfora.content.shatter_assembler.ShatterAssemblerBlock;
import net.multyfora.index.JocBlocks;

public class JocBlockStateProvider extends BlockStateProvider {

    public JocBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, AeronauticsJoyofcreation.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (DyeColor color : DyeColor.values()) {
            String colorName = color.getSerializedName();
            ModelFile balloonModel = models()
                    .withExistingParent("block/balloon_" + colorName, modLoc("block/balloon"))
                    .texture("0", modLoc("block/balloon_" + colorName))
                    .texture("particle", modLoc("block/balloon_" + colorName));

            getVariantBuilder(JocBlocks.BALLOONS.get(color).get())
                    .partialState().addModels(new ConfiguredModel(balloonModel));
        }

        for (DyeColor color : DyeColor.values()) {
            registerSymmetricCrossSail(JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get(), color);
        }

        registerShatterAssembler();
        registerGyroscopicSeat();
    }

    private void registerShatterAssembler() {
        ModelFile model = models()
                .getBuilder("block/shatter_assembler")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("simulated", "block/physics_assembler/block")));

        getVariantBuilder(JocBlocks.SHATTER_ASSEMBLER.get()).forAllStates(state -> {
            AttachFace face = state.getValue(ShatterAssemblerBlock.FACE);
            Direction facing = state.getValue(ShatterAssemblerBlock.FACING);
            int x = switch (face) {
                case FLOOR -> 0;
                case WALL -> 90;
                case CEILING -> 180;
            };
            int y = ((int) facing.toYRot() + (face == AttachFace.CEILING ? 0 : 180)) % 360;
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
        });
    }

    private void registerGyroscopicSeat() {
        ModelFile frame = models().getExistingFile(modLoc("block/gyroscopic_seat/frame"));
        ModelFile part = models()
                .getBuilder("block/gyroscopic_seat/part")
                .texture("particle", modLoc("block/gyroscopic_seat"));

        getVariantBuilder(JocBlocks.GYROSCOPIC_SEAT.get()).forAllStatesExcept(state ->
                ConfiguredModel.builder()
                        .modelFile(state.getValue(GyroscopicSeatBlock.PART) == 0 ? frame : part)
                        .build(),
                GyroscopicSeatBlock.WATERLOGGED);
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