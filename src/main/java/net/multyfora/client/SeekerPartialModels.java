package net.multyfora.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class SeekerPartialModels {
    public static final PartialModel SEEKER_SPYGLASS =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("joc", "block/seeker_spyglass"));

    public static final PartialModel SEEKER_EYE_OF_ENDER =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("joc", "block/seeker_eye_of_ender"));

    public static final PartialModel SEEKER_MODULATING =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("simulated", "block/modulating_linked_receiver/item"));

    public static void init() {}
}
