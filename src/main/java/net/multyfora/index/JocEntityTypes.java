package net.multyfora.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.gyroseat.GyroscopicSeatEntity;
import net.multyfora.content.seeker.MapPinEntity;

public class JocEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, AeronauticsJoyofcreation.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<?>> GYROSCOPIC_SEAT =
        ENTITY_TYPES.register("gyroscopic_seat", () -> EntityType.Builder.of(GyroscopicSeatEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.35f)
            .noSummon()
            .build("gyroscopic_seat"));

    public static final DeferredHolder<EntityType<?>, EntityType<MapPinEntity>> MAP_PIN =
        ENTITY_TYPES.register("map_pin", () -> EntityType.Builder.<MapPinEntity>of(MapPinEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build("map_pin"));
}
