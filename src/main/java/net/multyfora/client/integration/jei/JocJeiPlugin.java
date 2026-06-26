package net.multyfora.client.integration.jei;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.portable_throttle.PortableThrottleLinkScreen;
import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;

@JeiPlugin
public class JocJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "jei_plugin");
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
