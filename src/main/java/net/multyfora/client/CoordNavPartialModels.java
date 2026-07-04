package net.multyfora.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class CoordNavPartialModels {
    public static final PartialModel COORD_NAV_SPYGLASS =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("joc", "block/coord_nav_spyglass"));

    public static final PartialModel COORD_NAV_EYE_OF_ENDER =
//            PartialModel.of(ResourceLocation.fromNamespaceAndPath("joc", "block/seeker_eye_of_ender"));
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("joc", "block/coord_nav_spyglass"));

    public static void init() {}
}