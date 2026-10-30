package net.multyfora.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.multyfora.AeronauticsJoyofcreation;

public class JocPartialModels {
    public static final PartialModel THROTTLE_BODY = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "item/portable_throttle_body"));
    public static final PartialModel THROTTLE_ANTENNA = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "item/portable_throttle_antenna"));
    public static final PartialModel THROTTLE_ANTENNA_POWERED = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "item/portable_throttle_antenna_powered"));
    public static final PartialModel THROTTLE_KNOB = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "item/portable_throttle_knob"));
    public static final PartialModel SHATTER_SAW = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/shatter_assembler/saw"));
    public static final PartialModel GYRO_OUTER_RING = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/gyroscopic_seat/outer_ring"));
    public static final PartialModel GYRO_INNER_RING = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/gyroscopic_seat/inner_ring"));
    public static final PartialModel GYRO_BALL = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/gyroscopic_seat/ball"));
    public static final PartialModel SEEKER_TABLE_PIN = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/seeker_table/pin"));
    public static final PartialModel SEEKER_TABLE_COMPASS = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/seeker_table/compass"));
    public static final PartialModel SEEKER_TABLE_HERE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/seeker_table/here"));
    public static final PartialModel SEEKER_TABLE_KNIFE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/seeker_table/pins/knife"));
    public static final PartialModel SEEKER_TABLE_FLAG_POLE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/seeker_table/pins/flag_pole"));
    public static final PartialModel SEEKER_TABLE_FLAG_CLOTH = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/seeker_table/pins/flag_cloth"));
    public static final PartialModel SEEKER_TABLE_TORCH = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/seeker_table/pins/torch"));
    public static final PartialModel SEEKER_TABLE_TORCH_FLAME = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "block/seeker_table/pins/torch_flame"));

    public static void init() {}
}