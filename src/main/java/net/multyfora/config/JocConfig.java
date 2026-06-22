package net.multyfora.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class JocConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_CREATIVE_STAFF;
    public static final ModConfigSpec.BooleanValue CAN_PICKUP_PLAYERS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("creative_staff");

        ENABLE_CREATIVE_STAFF = BUILDER
                .comment("Enable creative staff entity grab/release functionality")
                .define("enableCreativeStaff", true);

        CAN_PICKUP_PLAYERS = BUILDER
                .comment("Allow the creative staff to grab other players")
                .define("canPickupPlayers", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
