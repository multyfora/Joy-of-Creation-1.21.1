package net.multyfora.index;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.multyfora.content.crosssail.CrossSailBlock;
import net.multyfora.content.crosssail.SymmetricCrossSailBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.balloon.BalloonBlock;
import net.multyfora.content.coordnav.CoordNavBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import static net.multyfora.AeronauticsJoyofcreation.BLOCKS;
import static net.multyfora.index.JocItems.ITEMS;

/**
 * Registry holder for all custom blocks in the mod.
 * Blocks are registered via AeronauticsJoyofcreation.BLOCKS DeferredRegister.
 **/
public class JocBlocks {
    // Balloon block: a soft, non-occluding block available in 16 colors
    public static final DeferredBlock<BalloonBlock> BALLOON;
    // Coordinate Navigator block: directional block that emits redstone toward a target
    public static final DeferredBlock<CoordNavBlock> COORD_NAV;

    public static final DeferredHolder<Block, CrossSailBlock> CROSS_SAIL;
    public static final DeferredHolder<Item, BlockItem> CROSS_SAIL_ITEM;

    public static final DeferredHolder<Block, SymmetricCrossSailBlock> SYMMETRIC_CROSS_SAIL;
    public static final DeferredHolder<Item, BlockItem> SYMMETRIC_CROSS_SAIL_ITEM;

    static {
        CROSS_SAIL =
                BLOCKS.register("cross_sail",
                        () -> new CrossSailBlock(
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOL)
                                        .noOcclusion()
                                        .strength(0.5F)
                                        .sound(net.minecraft.world.level.block.SoundType.WOOL)
                        ));
        CROSS_SAIL_ITEM =
                ITEMS.register("cross_sail",
                        () -> new BlockItem(CROSS_SAIL.get(), new Item.Properties()));

        SYMMETRIC_CROSS_SAIL =
                BLOCKS.register("symmetric_cross_sail",
                        () -> new SymmetricCrossSailBlock(
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.WOOL)
                                        .noOcclusion()
                                        .strength(0.5F)
                                        .sound(net.minecraft.world.level.block.SoundType.WOOL)
                        ));
        SYMMETRIC_CROSS_SAIL_ITEM =
                ITEMS.register("symmetric_cross_sail",
                        () -> new BlockItem(SYMMETRIC_CROSS_SAIL.get(), new Item.Properties()));

        // Balloon: wool-like properties, transparent, no suffocation, no view blocking
        BALLOON = BLOCKS.register("balloon",
                () -> new BalloonBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(0.8f)
                        .sound(SoundType.WOOL)
                        .noOcclusion()
                        .isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false)));
        // Coordinate Navigator: metal block with no occlusion
        COORD_NAV = BLOCKS.register("coord_navigator",
            () -> {
                return new CoordNavBlock(
                    BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0f)
                        .sound(SoundType.METAL)
                        .noOcclusion()
                );
            }
        );
    }

    // Dummy method to trigger static initialisation; registers blocks via the static initializer above
    public static void register() {}
}
