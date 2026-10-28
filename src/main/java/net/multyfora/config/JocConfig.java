package net.multyfora.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class JocConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_CREATIVE_STAFF;
    public static final ModConfigSpec.BooleanValue CAN_PICKUP_PLAYERS;

    public static final ModConfigSpec.DoubleValue LIFT_PER_BALLOON;

    public static final ModConfigSpec.IntValue SHATTER_ASSEMBLER_MAX_BLOCKS;

    private static final Map<String, ModConfigSpec.BooleanValue> FEATURE_VALUES = new LinkedHashMap<>();
    public static final Map<String, ModConfigSpec.BooleanValue> FEATURES = Collections.unmodifiableMap(FEATURE_VALUES);

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("creative_staff");

        ENABLE_CREATIVE_STAFF = BUILDER
            .comment("Enable creative staff entity grab/release functionality")
            .define("enableCreativeStaff", true)
        ;

        CAN_PICKUP_PLAYERS = BUILDER
            .comment("Allow the creative staff to grab other players")
            .define("canPickupPlayers", false)
        ;

        BUILDER.pop();

        BUILDER.push("balloon");

        LIFT_PER_BALLOON = BUILDER
            .comment("Number of blocks' worth of mass each balloon supports at full air pressure; lift scales linearly with air pressure as aeronautics envelopes do, so balloons decelerate as they climb and stop where the air is too thin")
            .defineInRange("liftPerBalloon", 1.5, 0.0, 100.0)
        ;

        BUILDER.pop();

        BUILDER.push("shatter_assembler");

        SHATTER_ASSEMBLER_MAX_BLOCKS = BUILDER
            .comment("Maximum number of connected blocks the shatter assembler will break apart in one activation")
            .defineInRange("maxBlocks", 512, 1, 65536)
        ;

        BUILDER.pop();

        BUILDER
            .comment(
                "Toggle individual blocks, items and features. Everything is enabled by default.",
                "A disabled block or item stays registered (so existing worlds don't lose it), but its recipes are removed,",
                "it is hidden from the creative tab and JEI/EMI, and it can no longer be placed or used."
            )
            .push("features");

        feature("balloons", "Balloon blocks (all colors)");
        feature("deflatedBalloons", "Deflated balloon items and blowing them up into balloons");
        feature("symmetricCrossSails", "Symmetric cross sail blocks (all colors)");
        feature("seeker", "Seeker block");
        feature("shatterAssembler", "Shatter assembler block");
        feature("gyroscopicSeat", "Gyroscopic seat block");
        feature("portableThrottle", "Portable throttle item");
        feature("portableTypewriter", "Portable typewriter item");
        feature("shearsCutting", "Cutting physics structures apart with shears");
        feature("multiRope", "Attaching more than one rope to a single rope connector");

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static void feature(String key, String comment) {
        FEATURE_VALUES.put(key, BUILDER.comment("Enable: " + comment).define(key, true));
    }

    public static boolean isFeatureEnabled(String key) {
        ModConfigSpec.BooleanValue value = FEATURE_VALUES.get(key);
        if (value == null) return true;
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
