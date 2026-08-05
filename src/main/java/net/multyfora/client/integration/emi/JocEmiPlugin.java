package net.multyfora.client.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.portable_throttle.PortableThrottleLinkScreen;
import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;
import net.multyfora.content.balloon.BalloonBlowingRecipe;
import net.multyfora.index.JocItems;

@EmiEntrypoint
public class JocEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        EmiRecipeCategory balloonBlowing = new EmiRecipeCategory(
                ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "balloon_blowing"),
                EmiStack.of(JocItems.DEFLATED_BALLOONS.get(DyeColor.WHITE).get()));
        registry.addCategory(balloonBlowing);
        for (DyeColor color : DyeColor.values()) {
            registry.addRecipe(new BalloonBlowingEmiRecipe(balloonBlowing, new BalloonBlowingRecipe(color)));
        }

        registry.addDragDropHandler(PortableTypewriterScreen.class, new TypewriterDragHandler());
        registry.addDragDropHandler(PortableThrottleLinkScreen.class, new ThrottleDragHandler());

        registry.addExclusionArea(PortableTypewriterScreen.class, (screen, consumer) -> {
            for (var area : screen.getExclusionAreas()) {
                consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
        });
        registry.addExclusionArea(PortableThrottleLinkScreen.class, (screen, consumer) -> {
            for (var area : screen.getExclusionAreas()) {
                consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
        });
    }
}
