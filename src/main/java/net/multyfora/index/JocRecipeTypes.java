package net.multyfora.index;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.balloon.BalloonBlowingRecipe;

public class JocRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, AeronauticsJoyofcreation.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, AeronauticsJoyofcreation.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BalloonBlowingRecipe>> BALLOON_BLOWING_TYPE =
            RECIPE_TYPES.register("balloon_blowing", () -> new RecipeType<>() {});

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BalloonBlowingRecipe>> BALLOON_BLOWING_SERIALIZER =
            RECIPE_SERIALIZERS.register("balloon_blowing", BalloonBlowingRecipe.Serializer::new);

    public static void register() {}
}
