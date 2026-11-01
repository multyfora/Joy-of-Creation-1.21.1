package net.multyfora.index;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.multyfora.content.shatter_assembler.ShatterAssemblerBlockEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.balloon.BalloonBlockEntity;
import net.multyfora.content.gyroseat.GyroscopicSeatBlockEntity;
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.content.seeker.SeekerTableBlockEntity;

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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeekerBlockEntity>> SEEKER =
        BLOCK_ENTITY_TYPES.register(
            "seeker",
            () -> BlockEntityType.Builder.of(
                SeekerBlockEntity::new, JocBlocks.SEEKER.get()
            ).build(null)
        );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeekerTableBlockEntity>> SEEKER_TABLE =
        BLOCK_ENTITY_TYPES.register(
            "seeker_table",
            () -> BlockEntityType.Builder.of(
                SeekerTableBlockEntity::new, JocBlocks.SEEKER_TABLE.get()
            ).build(null)
        );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GyroscopicSeatBlockEntity>> GYROSCOPIC_SEAT =
        BLOCK_ENTITY_TYPES.register(
            "gyroscopic_seat",
            () -> BlockEntityType.Builder.of(
                GyroscopicSeatBlockEntity::new, JocBlocks.GYROSCOPIC_SEAT.get()
            ).build(null)
        );

}
