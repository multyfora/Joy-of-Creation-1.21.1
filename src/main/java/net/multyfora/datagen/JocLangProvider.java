package net.multyfora.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.common.data.LanguageProvider;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.advancement.JocAdvancement;
import net.multyfora.advancement.JocAdvancements;

import java.util.EnumMap;
import java.util.Map;

public class JocLangProvider extends LanguageProvider {

    private static final Map<DyeColor, String> COLOR_NAMES = new EnumMap<>(DyeColor.class);
    static {
        COLOR_NAMES.put(DyeColor.WHITE, "White");
        COLOR_NAMES.put(DyeColor.ORANGE, "Orange");
        COLOR_NAMES.put(DyeColor.MAGENTA, "Magenta");
        COLOR_NAMES.put(DyeColor.LIGHT_BLUE, "Light Blue");
        COLOR_NAMES.put(DyeColor.YELLOW, "Yellow");
        COLOR_NAMES.put(DyeColor.LIME, "Lime");
        COLOR_NAMES.put(DyeColor.PINK, "Pink");
        COLOR_NAMES.put(DyeColor.GRAY, "Gray");
        COLOR_NAMES.put(DyeColor.LIGHT_GRAY, "Light Gray");
        COLOR_NAMES.put(DyeColor.CYAN, "Cyan");
        COLOR_NAMES.put(DyeColor.PURPLE, "Purple");
        COLOR_NAMES.put(DyeColor.BLUE, "Blue");
        COLOR_NAMES.put(DyeColor.BROWN, "Brown");
        COLOR_NAMES.put(DyeColor.GREEN, "Green");
        COLOR_NAMES.put(DyeColor.RED, "Red");
        COLOR_NAMES.put(DyeColor.BLACK, "Black");
    }

    public JocLangProvider(PackOutput output) {
        super(output, AeronauticsJoyofcreation.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.joc", "Aeronautics: Joy of Creation");
        add("joc.tooltip.unfinished", "[UNFINISHED]");
        add("joc.tooltip.disabled", "Disabled in config");
        add("joc.feature_disabled", "This feature is disabled in the config");

        add("block.joc.seeker", "Seeker");
        add("block.joc.player_direction", "Player Direction");
        add("block.joc.seeker_table", "Seeker Table");
        add("item.joc.map_pin", "Map Pin");
        add("item.joc.map_pin.tooltip.summary", "Right-click a _Seeker Table_'s map to pin a spot, or _throw_ it onto the map to pin wherever it lands. _Arrows_ shot onto the map work too.");
        add("entity.joc.map_pin", "Map Pin");

        add("block.joc.shatter_assembler", "Shatter Assembler");
        add("block.joc.gyroscopic_seat", "Gyroscopic Seat");
        add("joc.shatter_assembler.immovable", "%s can't be shattered");

        add("block.joc.seeker.tooltip.summary", "Tracks a _target_ and emits _redstone signals_ based on the direction to it. Insert a _Spyglass_, _Eye of Ender_ or _Modulating Receiver_ to choose what it seeks. Place _3×3_ or more empty seekers side by side to build a _Seeker Table_.");
        add("block.joc.shatter_assembler.tooltip.summary", "Splits a connected _solid block_ structure into individual _SubLevels_. Hold and drag the _lever_, then release to shatter the assembly.");
        add("block.joc.gyroscopic_seat.tooltip.summary", "A seat that prevents _sublevel rotation_ from affecting the rider, keeping your view stable. _Right-click_ to sit.");

        for (DyeColor color : DyeColor.values()) {
            String colorName = color.getSerializedName();
            add("block.joc." + colorName + "_balloon", COLOR_NAMES.get(color) + " Balloon");
            add("block.joc." + colorName + "_balloon.tooltip.summary", "Provides _lift_ when attached to your airship structure. Multiple balloons generate more lift, so distribute them evenly.");
            add("block.joc." + colorName + "_symmetric_cross_sail", COLOR_NAMES.get(color) + " Symmetric Cross Sail");
            add("block.joc." + colorName + "_symmetric_cross_sail.tooltip.summary", "A sail providing _planar drag_ for airship movement.");
            add("item.joc.deflated_balloon_" + colorName, COLOR_NAMES.get(color) + " Deflated Balloon");
            add("item.joc.deflated_balloon_" + colorName + ".tooltip.summary", "Hold _use_ for 2 seconds to blow this into an inflated _balloon_.");
        }

        add("joc.gui.inventory", "Inventory");

        add("item.joc.portable_typewriter", "Portable Typewriter");
        add("item.joc.portable_typewriter.tooltip.summary", "A handheld _typewriter_ that allows controlling ships from anywhere.");
        add("item.joc.portable_throttle", "Portable Throttle Lever");
        add("item.joc.portable_throttle.tooltip.summary", "Allows sending _analog_ redstone signal wirelessly");
        add("subtitles.joc.balloon_blow", "Balloon being blown");
        add("joc.recipe.balloon_blowing", "Balloon Blowing");
        add("joc.recipe.balloon_blowing.tooltip", "Hold to use for 2 seconds");
        add("emi.category.joc.balloon_blowing", "Balloon Blowing");
        add("screen.joc.portable_throttle.hint", "Click an inventory item, then click a frequency slot to set it");
        add("screen.joc.portable_throttle.saved", "\u00a7aSaved");
        add("screen.joc.portable_typewriter.frequency_label", "Frequency Items");
        add("screen.joc.portable_typewriter.selected_hint", "Click a Redstone Link to bind, or set frequency items above");
        add("screen.joc.portable_typewriter.click_hint", "Click a key to select it for binding");
        add("screen.joc.portable_typewriter.bound_count", "%s keys bound");
        add("item.joc.portable_typewriter.active", "Typing into Redstone Links - right-click to stop");
        add("item.joc.portable_typewriter.inactive", "Typewriter released");
        add("item.joc.portable_typewriter.bind_prompt", "Press a key to bind it to this Redstone Link - right-click to cancel");
        add("item.joc.portable_typewriter.bound", "Bound %s to the Redstone Link");

        add("item.joc.spyglass.carried_target", "Target: %s, %s, %s");

        add("item.joc.seeker.captured_target", "Linking from: %s, %s, %s");
        add("item.joc.seeker.captured", "Captured seeker at %s, %s, %s");
        add("item.joc.seeker.linked", "Seekers linked to %s, %s, %s");
        add("item.joc.seeker.link_self", "Cannot link a seeker to itself");
        add("item.joc.seeker.link_gone", "Target seeker no longer exists");
        add("item.joc.seeker.link_incompatible", "Target seeker has an incompatible module");
        add("joc.seeker_table.formed", "Seeker Table assembled (%s × %s)");
        add("joc.seeker_table.scale_set", "Map scale 1 : %s");
        add("joc.goggles.seeker_table.header", "Seeker Table");
        add("joc.goggles.seeker_table.size", "Size: ");
        add("joc.goggles.seeker_table.scale", "Scale: ");
        add("joc.goggles.seeker_table.marker", "Marker: ");
        add("joc.goggles.seeker_table.no_marker", "Right-click the map to place a marker");
        add("joc.goggles.seeker.following_table", "Following a Seeker Table marker");
        add("item.joc.seeker.captured_table", "Captured seeker table at %s, %s, %s");
        add("item.joc.seeker.linked_table", "Seeker now follows the table's marker");

        add("screen.joc.seeker", "Seeker");
        add("screen.joc.seeker.set", "Set Target");
        add("joc.goggles.seeker.module", "Module: ");
        add("joc.goggles.seeker.module.none", "Empty");
        add("joc.goggles.seeker.module.spyglass", "Spyglass");
        add("joc.goggles.seeker.module.player_dir", "Eye of Ender");
        add("joc.goggles.seeker.module.modulating", "Modulating Receiver");
        add("joc.goggles.seeker.mode", "Mode: ");
        add("joc.goggles.seeker.mode.3d", "3D");
        add("joc.goggles.seeker.mode.2d", "2D");
        add("joc.goggles.seeker.mode.detail_3d", " (6-sided)");
        add("joc.goggles.seeker.mode.detail_2d", " (XZ-plane)");
        add("joc.goggles.seeker.target", "Target: ");
        add("joc.goggles.seeker.no_target", "No target");
        add("joc.goggles.seeker.distance", "Distance: ");
        add("joc.goggles.seeker.distance_unit", " blocks");
        add("joc.goggles.seeker.range", "Range: ");
        add("joc.goggles.seeker.tracking", "Tracking player");
        add("joc.goggles.seeker.idle", "Idle");
        add("joc.goggles.seeker.linked", "Linked seekers: ");
        add("joc.goggles.seeker.linked_pos", "• %s, %s, %s");
        add("joc.goggles.seeker.linked_more", "• …and %s more");
        add("joc.goggles.seeker.lost", "Target lost");        add("screen.joc.seeker.current_pos", "Use Current");
        add("screen.joc.seeker.waypoint_picker", "Select Waypoint");
        add("screen.joc.seeker.no_waypoints", "No waypoints found in Xaero's World Map");
        add("screen.joc.seeker_distance", "Seeker - Distance");

        add("joc.configuration.title", "Aeronautics: Joy of Creation Configs");
        add("joc.configuration.section.joc.common.toml", "Aeronautics: Joy of Creation Configs");
        add("joc.configuration.section.joc.common.toml.title", "Aeronautics: Joy of Creation Configs");

        add("joc.configuration.section.joc.client.toml", "Aeronautics: Joy of Creation Client Configs");
        add("joc.configuration.section.joc.client.toml.title", "Aeronautics: Joy of Creation Client Configs");
        add("joc.configuration.seeker_table", "Seeker Table");
        add("joc.configuration.seeker_table.terrainModel", "3D terrain on the map");

        add("joc.configuration.balloon.liftPerBalloon", "Lift per balloon block");
        add("joc.configuration.balloon.dragCoefficient", "Air drag coefficient for balloons");

        add("joc.configuration.creative_staff.enableCreativeStaff", "Enable creative staff entity grab/release");
        add("joc.configuration.creative_staff.canPickupPlayers", "Allow grabbing other players");
        add("joc.configuration.creative_staff.staffGrabRange", "Max grab range in blocks");
        add("joc.configuration.creative_staff.staffHoldDistance", "Default hold distance in blocks");

        add("joc.configuration.shatter_assembler", "Shatter Assembler");
        add("joc.configuration.shatter_assembler.maxBlocks", "Max blocks shattered at once");

        add("joc.configuration.features", "Features");
        add("joc.configuration.features.balloons", "Balloons");
        add("joc.configuration.features.deflatedBalloons", "Deflated balloons");
        add("joc.configuration.features.symmetricCrossSails", "Symmetric cross sails");
        add("joc.configuration.features.seeker", "Seeker");
        add("joc.configuration.features.shatterAssembler", "Shatter assembler");
        add("joc.configuration.features.gyroscopicSeat", "Gyroscopic seat");
        add("joc.configuration.features.portableThrottle", "Portable throttle");
        add("joc.configuration.features.portableTypewriter", "Portable typewriter");
        add("joc.configuration.features.shearsCutting", "Shears cutting");
        add("joc.configuration.features.multiRope", "Multiple ropes per connector");

        add("joc.ponder.seeker.header", "Using the Seeker");
        add("joc.ponder.seeker.text_1", "The Seeker tracks targets and emits redstone signals based on the direction to the target.");
        add("joc.ponder.seeker.text_2", "Insert a Spyglass to track specific coordinates. The Seeker will emit a redstone signal toward the target.");
        add("joc.ponder.seeker.text_3", "The nixie tubes now show the redstone signal strength, indicating the angle to the target.");

        add("joc.ponder.tag.aeronautics", "Aeronautics Components");
        add("joc.ponder.tag.aeronautics.description", "Components for airships and aeronautical contraptions");

        for (JocAdvancement advancement : JocAdvancements.ENTRIES) {
            add(advancement.titleKey(), advancement.getTitle());
            add(advancement.descKey(), advancement.getDescription());
        }
    }
}
