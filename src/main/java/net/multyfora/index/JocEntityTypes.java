package net.multyfora.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.gyroseat.GyroscopicSeatEntity;

public class JocEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, AeronauticsJoyofcreation.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<?>> GYROSCOPIC_SEAT =
        ENTITY_TYPES.register("gyroscopic_seat", () -> EntityType.Builder.of(GyroscopicSeatEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.35f)
            .noSummon()
            .build("gyroscopic_seat"));
}
