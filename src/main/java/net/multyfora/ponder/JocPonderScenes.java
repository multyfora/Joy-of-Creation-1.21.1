package net.multyfora.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;
import net.multyfora.ponder.scenes.*;

public class JocPonderScenes {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(JocBlocks.BALLOONS.values().stream()
                        .map(block -> block.getId())
                        .toArray(ResourceLocation[]::new))
                .addStoryBoard("balloon", BalloonScenes::basic, JocPonderTags.JOC_AERONAUTICS);

        helper.forComponents(JocBlocks.SEEKER.getId())
                .addStoryBoard("seeker", SeekerScenes::basic, JocPonderTags.JOC_AERONAUTICS);

        helper.forComponents(JocBlocks.GYROSCOPIC_SEAT.getId())
                .addStoryBoard("gyroscopic_seat", GyroscopicSeatScenes::basic, JocPonderTags.JOC_AERONAUTICS);

        helper.forComponents(JocBlocks.SHATTER_ASSEMBLER.getId())
                .addStoryBoard("shatter_assembler", ShatterAssemblerScenes::basic, JocPonderTags.JOC_AERONAUTICS);

        helper.forComponents(JocBlocks.SYMMETRIC_CROSS_SAILS.values().stream()
                        .map(block -> block.getId())
                        .toArray(ResourceLocation[]::new))
                .addStoryBoard("cross_sail", CrossSailScenes::basic, JocPonderTags.JOC_AERONAUTICS);

        helper.forComponents(JocItems.PORTABLE_TYPEWRITER.getId())
                .addStoryBoard("portable_typewriter", PortableTypewriterScenes::basic, JocPonderTags.JOC_AERONAUTICS);

        helper.forComponents(JocItems.PORTABLE_THROTTLE.getId())
                .addStoryBoard("portable_throttle", PortableThrottleScenes::basic, JocPonderTags.JOC_AERONAUTICS);
    }
}