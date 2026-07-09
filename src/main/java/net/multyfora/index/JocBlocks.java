package net.multyfora.index;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.multyfora.content.crosssail.SymmetricCrossSailBlock;
import net.multyfora.content.shatter_assembler.ShatterAssemblerBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.multyfora.content.balloon.BalloonBlock;
import net.multyfora.content.seeker.SeekerBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Map;

import static net.multyfora.AeronauticsJoyofcreation.BLOCKS;
import static net.multyfora.index.JocItems.ITEMS;

/**
 * Registry holder for all custom blocks in the mod.
 * Blocks are registered via AeronauticsJoyofcreation.BLOCKS DeferredRegister.
 **/
public class JocBlocks {
    // Balloon block: a soft, non-occluding block available in 16 colors
    public static final DeferredBlock<BalloonBlock> BALLOON;
    // Seeker block: directional block that emits redstone toward a target
    public static final DeferredBlock<SeekerBlock> SEEKER;

    public static final Map<DyeColor, DeferredBlock<SymmetricCrossSailBlock>> SYMMETRIC_CROSS_SAILS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredItem<BlockItem>> SYMMETRIC_CROSS_SAIL_ITEMS = new EnumMap<>(DyeColor.class);
    public static final DeferredBlock<ShatterAssemblerBlock> SHATTER_ASSEMBLER;
    static {
        for (DyeColor color : DyeColor.values()) {
            String symName = color.getSerializedName() + "_symmetric_cross_sail";
            DeferredBlock<SymmetricCrossSailBlock> symBlock = BLOCKS.register(symName,
                    () -> new SymmetricCrossSailBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOL)
                                    .noOcclusion()
                                    .strength(0.5F)
                                    .sound(SoundType.WOOL),
                            color
                    ));
            SYMMETRIC_CROSS_SAILS.put(color, symBlock);
            SYMMETRIC_CROSS_SAIL_ITEMS.put(color, ITEMS.register(symName,
                    () -> new BlockItem(SYMMETRIC_CROSS_SAILS.get(color).get(), new Item.Properties())));
        }
        SHATTER_ASSEMBLER = BLOCKS.register("shatter_assembler",
                () -> new ShatterAssemblerBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(0.5F)
                        .sound(SoundType.METAL)
                        .noOcclusion()
                )
        );
        // Balloon: wool-like properties, transparent, no suffocation, no view blocking
        BALLOON = BLOCKS.register("balloon",
                () -> new BalloonBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(0.8f)
                        .sound(SoundType.WOOL)
                        .noOcclusion()
                        .isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false)));
        // Seeker: metal block with no occlusion
        SEEKER = BLOCKS.register("seeker",
            () -> {
                return new SeekerBlock(
                    BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0f)
                        .sound(SoundType.NETHERITE_BLOCK)
                        .noOcclusion()
                );
            }
        );
    }

    // Dummy method to trigger static initialisation; registers blocks via the static initializer above
    public static void register() {}
}
