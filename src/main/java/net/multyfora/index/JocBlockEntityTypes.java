package net.multyfora.index;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.balloon.BalloonBlockEntity;
import net.multyfora.content.coordnav.CoordNavBlockEntity;

import static net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE;

// Registry holder for all block entity types, linking each BE class to its corresponding block(s)
public class JocBlockEntityTypes {
    // DeferredRegister for block entity types, using the BLOCK_ENTITY_TYPE registry
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(BLOCK_ENTITY_TYPE, AeronauticsJoyofcreation.MODID);

    // Balloon block entity: handles rope tethering and connector position tracking
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BalloonBlockEntity>> BALLOON =
        BLOCK_ENTITY_TYPES.register(
            "balloon",
            () -> BlockEntityType.Builder.of(
                BalloonBlockEntity::new, JocBlocks.BALLOON.get()
            ).build(null)
        );

    // Coordinate Navigator block entity: computes redstone output based on target direction
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoordNavBlockEntity>> COORD_NAV =
        BLOCK_ENTITY_TYPES.register(
            "coord_navigator",
            () -> BlockEntityType.Builder.of(
                CoordNavBlockEntity::new, JocBlocks.COORD_NAV.get()
            ).build(null)
        );

}
