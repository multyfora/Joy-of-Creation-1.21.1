package net.multyfora.client.integration.jei;

import java.util.List;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.portable_throttle.PortableThrottleLinkScreen;
import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;
import net.multyfora.content.balloon.BalloonBlowingRecipe;
import net.multyfora.index.JocItems;
import net.multyfora.index.JocRecipeTypes;

@JeiPlugin
public class JocJeiPlugin implements IModPlugin {

    private CreateRecipeCategory<BalloonBlowingRecipe> balloonBlowingCategory;

    private CreateRecipeCategory<BalloonBlowingRecipe> balloonBlowingCategory() {
        if (balloonBlowingCategory == null) {
            balloonBlowingCategory = new CreateRecipeCategory.Builder<>(BalloonBlowingRecipe.class)
                    .addTypedRecipes(JocRecipeTypes.BALLOON_BLOWING_TYPE::get)
                    .itemIcon(JocItems.DEFLATED_BALLOONS.get(DyeColor.WHITE).get())
                    .emptyBackground(177, 53)
                    .build(ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "balloon_blowing"),
                            BalloonBlowingCategory::new);
        }
        return balloonBlowingCategory;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(balloonBlowingCategory());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        balloonBlowingCategory().registerRecipes(registration);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(PortableTypewriterScreen.class, new TypewriterGhostHandler<>());
        registration.addGhostIngredientHandler(PortableThrottleLinkScreen.class, new ThrottleGhostHandler<>());

        registration.addGuiContainerHandler(PortableTypewriterScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(PortableTypewriterScreen screen) {
                return screen.getExclusionAreas();
            }
        });
        registration.addGuiContainerHandler(PortableThrottleLinkScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(PortableThrottleLinkScreen screen) {
                return screen.getExclusionAreas();
            }
        });
    }
}
