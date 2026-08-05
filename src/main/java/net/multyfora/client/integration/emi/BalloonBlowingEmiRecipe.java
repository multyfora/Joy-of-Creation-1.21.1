package net.multyfora.client.integration.emi;

import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.DrawableWidget;
import dev.emi.emi.api.widget.FillingArrowWidget;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.integration.AnimatedBalloon;
import net.multyfora.content.balloon.BalloonBlowingRecipe;

public class BalloonBlowingEmiRecipe extends BasicEmiRecipe {

    private final BalloonBlowingRecipe recipe;

    public BalloonBlowingEmiRecipe(EmiRecipeCategory category, BalloonBlowingRecipe recipe) {
        super(category, ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID,
                "balloon_blowing/" + recipe.getColor().getSerializedName()), 177, 53);
        this.recipe = recipe;
        this.inputs.add(EmiStack.of(recipe.getInput()));
        this.outputs.add(EmiStack.of(recipe.getOutput()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.add(new SlotWidget(inputs.get(0), 15, 9));
        widgets.add(new FillingArrowWidget(94, 29, 2000));
        widgets.add(new SlotWidget(outputs.get(0), 139, 27));
        widgets.add(new DrawableWidget(48, 27, 52, 50,
                (draw, mouseX, mouseY, delta) -> AnimatedBalloon.draw(draw, recipe.getColor(), 0, 0))
                .tooltip((mouseX, mouseY) -> List.of(ClientTooltipComponent.create(
                        Component.translatable("joc.recipe.balloon_blowing.tooltip").getVisualOrderText()))));
    }
}
