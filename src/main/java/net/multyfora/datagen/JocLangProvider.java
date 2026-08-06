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

        add("block.joc.seeker", "Seeker");
        add("block.joc.player_direction", "Player Direction");

        add("block.joc.seeker.tooltip.summary", "Tracks a _target_ and emits _redstone signals_ based on the direction to it. Insert a _Spyglass_, _Eye of Ender_ or _Modulating Receiver_ to choose what it seeks.");
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
        add("subtitles.joc.balloon_pop", "Balloon pops");
        add("joc.recipe.balloon_blowing", "Balloon Blowing");
        add("joc.recipe.balloon_blowing.tooltip", "Hold to use for 2 seconds");
        add("emi.category.joc.balloon_blowing", "Balloon Blowing");
        add("screen.joc.portable_throttle.hint", "Click an inventory item, then click a frequency slot to set it");
        add("screen.joc.portable_throttle.saved", "\u00a7aSaved");
        add("screen.joc.portable_throttle.bound_strength", "Sending signal strength %s");
        add("screen.joc.portable_typewriter.frequency_label", "Frequency Items");
        add("screen.joc.portable_typewriter.selected_hint", "Click a Redstone Link to bind, or set frequency items above");
        add("screen.joc.portable_typewriter.click_hint", "Click a key to select it for binding");
        add("screen.joc.portable_typewriter.bound_count", "%s keys bound");

        add("item.joc.spyglass.carried_target", "Target: %s, %s, %s");

        add("item.joc.seeker.captured_target", "Linking from: %s, %s, %s");
        add("item.joc.seeker.captured", "Captured seeker at %s, %s, %s");
        add("item.joc.seeker.linked", "Seekers linked to %s, %s, %s");
        add("item.joc.seeker.link_self", "Cannot link a seeker to itself");
        add("item.joc.seeker.link_gone", "Target seeker no longer exists");
        add("item.joc.seeker.link_incompatible", "Target seeker has an incompatible module");

        add("screen.joc.seeker", "Seeker");
        add("screen.joc.seeker.set", "Set Target");
        add("screen.joc.seeker.current_pos", "Use Current");
        add("screen.joc.seeker.waypoint_picker", "Select Waypoint");
        add("screen.joc.seeker.no_waypoints", "No waypoints found in Xaero's World Map");
        add("screen.joc.seeker_distance", "Seeker - Distance");

        add("joc.configuration.title", "Aeronautics: Joy of Creation Configs");
        add("joc.configuration.section.joc.common.toml", "Aeronautics: Joy of Creation Configs");
        add("joc.configuration.section.joc.common.toml.title", "Aeronautics: Joy of Creation Configs");

        add("joc.configuration.balloon.liftPerBalloon", "Lift per balloon block");
        add("joc.configuration.balloon.dragCoefficient", "Air drag coefficient for balloons");

        add("joc.configuration.creative_staff.enableCreativeStaff", "Enable creative staff entity grab/release");
        add("joc.configuration.creative_staff.canPickupPlayers", "Allow grabbing other players");
        add("joc.configuration.creative_staff.staffGrabRange", "Max grab range in blocks");
        add("joc.configuration.creative_staff.staffHoldDistance", "Default hold distance in blocks");

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
