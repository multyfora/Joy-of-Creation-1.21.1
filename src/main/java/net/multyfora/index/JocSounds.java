package net.multyfora.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.AeronauticsJoyofcreation;

public class JocSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, AeronauticsJoyofcreation.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SEEKER_ITEM_IN =
            SOUNDS.register("seeker_item_in",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "seeker_item_in")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SEEKER_ITEM_OUT =
            SOUNDS.register("seeker_item_out",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "seeker_item_out")));

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}