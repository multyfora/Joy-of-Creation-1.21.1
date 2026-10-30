package net.multyfora.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class JocClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SEEKER_TABLE_TERRAIN;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("seeker_table");

        SEEKER_TABLE_TERRAIN = BUILDER
            .comment("Show the seeker table's map as a 3D terrain model; when off it is drawn as a flat map")
            .define("terrainModel", true)
        ;

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
