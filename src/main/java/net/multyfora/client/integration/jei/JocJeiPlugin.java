package net.multyfora.client.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.portable_throttle.PortableThrottleScreen;
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
        registration.addGhostIngredientHandler(PortableThrottleScreen.class, new ThrottleGhostHandler<>());
    }
}
