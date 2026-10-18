package net.multyfora.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.index.JocBlocks;
import net.multyfora.ponder.scenes.SeekerScenes;

public class JocPonderScenes {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(JocBlocks.SEEKER.getId())
                .addStoryBoard("seeker", SeekerScenes::basic, JocPonderTags.JOC_AERONAUTICS);
    }
}