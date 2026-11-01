package net.multyfora.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;
import net.multyfora.ponder.scenes.BalloonScenes;
import net.multyfora.ponder.scenes.CrossSailScenes;
import net.multyfora.ponder.scenes.GyroscopicSeatScenes;
import net.multyfora.ponder.scenes.SeekerScenes;
import net.multyfora.ponder.scenes.SeekerTableScenes;
import net.multyfora.ponder.scenes.ShatterAssemblerScenes;

import java.util.ArrayList;
import java.util.List;

public class JocPonderScenes {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        List<ResourceLocation> balloons = new ArrayList<>();
        JocBlocks.BALLOONS.values().forEach(block -> balloons.add(block.getId()));
        JocItems.DEFLATED_BALLOONS.values().forEach(item -> balloons.add(item.getId()));
        helper.forComponents(balloons)
                .addStoryBoard("balloon/intro", BalloonScenes::intro, JocPonderTags.JOC_AERONAUTICS)
                .addStoryBoard("balloon/pop", BalloonScenes::pop);

        List<ResourceLocation> sails = new ArrayList<>();
        JocBlocks.SYMMETRIC_CROSS_SAILS.values().forEach(block -> sails.add(block.getId()));
        helper.forComponents(sails)
                .addStoryBoard("cross_sail/intro", CrossSailScenes::placing, JocPonderTags.JOC_AERONAUTICS)
                .addStoryBoard("cross_sail/drag", CrossSailScenes::drag);

        helper.forComponents(JocBlocks.SEEKER.getId())
                .addStoryBoard("seeker/intro", SeekerScenes::intro, JocPonderTags.JOC_AERONAUTICS)
                .addStoryBoard("seeker/eye", SeekerScenes::eye)
                .addStoryBoard("seeker/modulating", SeekerScenes::modulating)
                .addStoryBoard("seeker/linking", SeekerScenes::linking)
                .addStoryBoard("seeker/logic_core", SeekerScenes::logicCore);

        helper.forComponents(JocBlocks.SEEKER.getId(), JocItems.MAP_PIN.getId())
                .addStoryBoard("seeker/table", SeekerTableScenes::intro, JocPonderTags.JOC_AERONAUTICS)
                .addStoryBoard("seeker/table", SeekerTableScenes::following)
                .addStoryBoard("seeker/table_wall", SeekerTableScenes::wall);

        helper.forComponents(JocBlocks.GYROSCOPIC_SEAT.getId())
                .addStoryBoard("gyroscopic_seat/intro", GyroscopicSeatScenes::intro, JocPonderTags.JOC_AERONAUTICS);

        helper.forComponents(JocBlocks.SHATTER_ASSEMBLER.getId())
                .addStoryBoard("shatter_assembler/intro", ShatterAssemblerScenes::intro, JocPonderTags.JOC_AERONAUTICS)
                .addStoryBoard("shatter_assembler/solid", ShatterAssemblerScenes::solid);
    }
}
