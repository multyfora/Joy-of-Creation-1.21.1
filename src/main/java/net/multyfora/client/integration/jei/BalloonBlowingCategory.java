package net.multyfora.client.integration.jei;

import java.util.List;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import net.multyfora.client.integration.AnimatedBalloon;
import net.multyfora.content.balloon.BalloonBlowingRecipe;

public class BalloonBlowingCategory extends CreateRecipeCategory<BalloonBlowingRecipe> {

    public BalloonBlowingCategory(Info<BalloonBlowingRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BalloonBlowingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 15, 9)
                .setBackground(getRenderedSlot(), -1, -1)
                .addItemStack(recipe.getInput());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 139, 27)
                .setBackground(getRenderedSlot(), -1, -1)
                .addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(BalloonBlowingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        AllGuiTextures.JEI_ARROW.render(graphics, 85, 32);
        AnimatedBalloon.draw(graphics, recipe.getColor(), 48, 27);
    }

    @Override
    public List<Component> getTooltipStrings(BalloonBlowingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX >= 48 && mouseX <= 100 && mouseY >= 27 && mouseY <= 77) {
            return List.of(Component.translatable("joc.recipe.balloon_blowing.tooltip"));
        }
        return List.of();
    }
}
