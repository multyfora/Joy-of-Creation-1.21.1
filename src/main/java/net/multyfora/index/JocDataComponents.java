package net.multyfora.index;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.component.DataComponentType;

import net.multyfora.AeronauticsJoyofcreation;

public class JocDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AeronauticsJoyofcreation.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> SEEKER_CARRIED_TARGET =
        DATA_COMPONENTS.register("seeker_carried_target",
            () -> DataComponentType.<BlockPos>builder()
                .persistent(BlockPos.CODEC)
                .networkSynchronized(BlockPos.STREAM_CODEC)
                .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LINKED_SEEKER_POS =
        DATA_COMPONENTS.register("linked_seeker_pos",
            () -> DataComponentType.<BlockPos>builder()
                .persistent(BlockPos.CODEC)
                .networkSynchronized(BlockPos.STREAM_CODEC)
                .build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
