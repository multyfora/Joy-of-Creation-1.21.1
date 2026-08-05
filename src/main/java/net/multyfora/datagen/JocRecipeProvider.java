package net.multyfora.datagen;

import com.simibubi.create.AllItems;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;

import com.simibubi.create.AllBlocks;

import net.minecraft.world.item.Items;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.balloon.BalloonBlowingRecipe;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;

import java.util.concurrent.CompletableFuture;

public class JocRecipeProvider extends RecipeProvider {

    public JocRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,
                        JocBlocks.SYMMETRIC_CROSS_SAILS.get(DyeColor.WHITE).get())
                .requires(SimBlocks.WHITE_SYMMETRIC_SAIL.get())
                .requires(SimBlocks.WHITE_SYMMETRIC_SAIL.get())
                .unlockedBy("has_sail", has(SimBlocks.WHITE_SYMMETRIC_SAIL.get()))
                .save(output);

        for (DyeColor color : DyeColor.values()) {
            if (color == DyeColor.WHITE) continue;

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,
                            JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get())
                    .requires(JocBlocks.SYMMETRIC_CROSS_SAILS.get(DyeColor.WHITE).get())
                    .requires(DyeItem.byColor(color))
                    .group("joc_symmetric_cross_sail")
                    .unlockedBy("has_white_cross_sail",
                            has(JocBlocks.SYMMETRIC_CROSS_SAILS.get(DyeColor.WHITE).get()))
                    .save(output);
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE,
                JocBlocks.SEEKER.get())
                .define('i', AllBlocks.INDUSTRIAL_IRON_BLOCK)
                .define('m', SimBlocks.REDSTONE_MAGNET)
                .define('b', AllItems.BRASS_INGOT)
                .define('r', Items.REDSTONE)
                .pattern("imi")
                .pattern("rbr")
                .pattern("imi")
                .unlockedBy("has_brass", has(AllItems.BRASS_INGOT.get()))
                .save(output);

        for (DyeColor color : DyeColor.values()) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,
                            JocItems.DEFLATED_BALLOONS.get(color).get())
                    .requires(Items.DRIED_KELP)
                    .requires(Items.STRING)
                    .requires(DyeItem.byColor(color))
                    .group("joc_deflated_balloon")
                    .unlockedBy("has_dried_kelp", has(Items.DRIED_KELP))
                    .save(output);
        }

        for (DyeColor color : DyeColor.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID,
                    "balloon_blowing/" + color.getSerializedName());
            output.accept(id, new BalloonBlowingRecipe(color), null);
        }
    }
}