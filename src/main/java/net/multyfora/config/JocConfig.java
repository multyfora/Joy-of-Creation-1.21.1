package net.multyfora.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class JocConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_CREATIVE_STAFF;
    public static final ModConfigSpec.BooleanValue CAN_PICKUP_PLAYERS;
    public static final ModConfigSpec.DoubleValue STAFF_GRAB_RANGE;
    public static final ModConfigSpec.DoubleValue STAFF_HOLD_DISTANCE;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("creative_staff");

        ENABLE_CREATIVE_STAFF = BUILDER
                .comment("Enable creative staff entity grab/release functionality")
                .define("enableCreativeStaff", true);

        CAN_PICKUP_PLAYERS = BUILDER
                .comment("Allow the creative staff to grab other players")
                .define("canPickupPlayers", false);

        STAFF_GRAB_RANGE = BUILDER
                .comment("Maximum range in blocks for grabbing entities")
                .defineInRange("staffGrabRange", 32.0, 1.0, 128.0);

        STAFF_HOLD_DISTANCE = BUILDER
                .comment("Default distance in blocks to hold grabbed entities from the player")
                .defineInRange("staffHoldDistance", 5.0, 1.0, 32.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
