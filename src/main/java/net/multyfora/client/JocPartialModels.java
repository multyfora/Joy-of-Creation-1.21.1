package net.multyfora.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.multyfora.AeronauticsJoyofcreation;

public class JocPartialModels {
    public static final PartialModel THROTTLE_BODY = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "item/portable_throttle_body"));
    public static final PartialModel THROTTLE_BODY_POWERED = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "item/portable_throttle_body_powered"));
    public static final PartialModel THROTTLE_KNOB = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "item/portable_throttle_knob"));
    public static final PartialModel SHATTER_SAW = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/shatter_assembler/saw"));

    public static void init() {}
}