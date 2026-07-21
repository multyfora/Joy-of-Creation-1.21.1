package net.multyfora.index;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.multyfora.content.shatter_assembler.ShatterAssemblerBlockEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.balloon.BalloonBlockEntity;
import net.multyfora.content.seeker.SeekerBlockEntity;

import static net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE;

public class JocBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(BLOCK_ENTITY_TYPE, AeronauticsJoyofcreation.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BalloonBlockEntity>> BALLOON =
        BLOCK_ENTITY_TYPES.register(
            "balloon",
            () -> BlockEntityType.Builder.of(
                BalloonBlockEntity::new,
                JocBlocks.BALLOONS.values().stream().map(h -> (Block) h.get()).toArray(Block[]::new)
            ).build(null)
        );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShatterAssemblerBlockEntity>> SHATTER_ASSEMBLER =
            BLOCK_ENTITY_TYPES.register("shatter_assembler",
                    () -> BlockEntityType.Builder.of(
                            ShatterAssemblerBlockEntity::new,
                            JocBlocks.SHATTER_ASSEMBLER.get()
                    ).build(null)
            );
    // Seeker block entity: computes redstone output based on target direction
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeekerBlockEntity>> SEEKER =
        BLOCK_ENTITY_TYPES.register(
            "seeker",
            () -> BlockEntityType.Builder.of(
                SeekerBlockEntity::new, JocBlocks.SEEKER.get()
            ).build(null)
        );

}
