package net.multyfora.content.balloon;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;
import net.multyfora.index.JocRecipeTypes;

public class BalloonBlowingRecipe implements Recipe<RecipeInput> {

    private static final Codec<DyeColor> DYE_COLOR_CODEC = Codec.STRING.flatXmap(
            name -> Optional.ofNullable(DyeColor.byName(name, null))
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown dye color: " + name)),
            color -> DataResult.success(color.getSerializedName()));

    public static final MapCodec<BalloonBlowingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    DYE_COLOR_CODEC.fieldOf("color").forGetter(BalloonBlowingRecipe::getColor))
                    .apply(instance, BalloonBlowingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BalloonBlowingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(name -> DyeColor.byName(name, DyeColor.WHITE),
                            DyeColor::getSerializedName),
                    BalloonBlowingRecipe::getColor, BalloonBlowingRecipe::new);

    private final DyeColor color;

    public BalloonBlowingRecipe(DyeColor color) {
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    public ItemStack getInput() {
        return new ItemStack(JocItems.DEFLATED_BALLOONS.get(color).get());
    }

    public ItemStack getOutput() {
        return new ItemStack(JocBlocks.BALLOON_ITEMS.get(color).get());
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return getOutput();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return getOutput();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.of(getInput()));
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return JocRecipeTypes.BALLOON_BLOWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return JocRecipeTypes.BALLOON_BLOWING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<BalloonBlowingRecipe> {

        @Override
        public MapCodec<BalloonBlowingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BalloonBlowingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
