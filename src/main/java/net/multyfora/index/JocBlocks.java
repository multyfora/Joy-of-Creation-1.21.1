package net.multyfora.index;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.balloon.BalloonBlock;
import net.multyfora.content.coordnav.CoordNavBlock;
import net.multyfora.content.playerdir.PlayerDirectionBlock;

// Registry holder for all custom blocks in the mod.
// Blocks are registered via AeronauticsJoyofcreation.BLOCKS DeferredRegister.
public class JocBlocks {
    // Balloon block: a soft, non-occluding block available in 16 colors
    public static final DeferredBlock<BalloonBlock> BALLOON;
    // Coordinate Navigator block: directional block that emits redstone toward a target
    public static final DeferredBlock<CoordNavBlock> COORD_NAV;
    // Player Direction block: directional block that tracks player look direction
    public static final DeferredBlock<PlayerDirectionBlock> PLAYER_DIRECTION;

    static {
        // Balloon: wool-like properties, transparent, no suffocation, no view blocking
        BALLOON = AeronauticsJoyofcreation.BLOCKS.register("balloon",
                () -> new BalloonBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(0.8f)
                        .sound(SoundType.WOOL)
                        .noOcclusion()
                        .isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false)));
        // Coordinate Navigator: metal block with no occlusion
        COORD_NAV = AeronauticsJoyofcreation.BLOCKS.register("coord_navigator",
                () -> new CoordNavBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0f)
                        .sound(SoundType.METAL)
                        .noOcclusion()));
        // Player Direction block: metal block with no occlusion
        PLAYER_DIRECTION = AeronauticsJoyofcreation.BLOCKS.register("player_direction",
                () -> new PlayerDirectionBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0f)
                        .sound(SoundType.METAL)
                        .noOcclusion()));
    }

    // Dummy method to trigger static initialisation; registers blocks via the static initializer above
    public static void register() {}
}
