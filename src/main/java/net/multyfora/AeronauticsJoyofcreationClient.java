package net.multyfora;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import net.multyfora.client.balloon.BalloonTetherRenderer;
import net.multyfora.client.coordnav.CoordNavRenderer;
import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;
import net.multyfora.client.portable_typewriter.PortableTypewriterClientHandler;
import net.multyfora.index.JocBlockEntityTypes;

// Client-side only mod entry point: registers renderers and client tick handlers
@Mod(value = AeronauticsJoyofcreation.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID, value = Dist.CLIENT)
public class AeronauticsJoyofcreationClient {
    // Registers the NeoForge configuration screen factory for this mod
    public AeronauticsJoyofcreationClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    // Registers block entity renderers: balloon tether line and coordinate navigator pointer
    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(JocBlockEntityTypes.BALLOON.get(), BalloonTetherRenderer::new);
        event.registerBlockEntityRenderer(JocBlockEntityTypes.COORD_NAV.get(), CoordNavRenderer::new);
    }

    // Client tick: runs the typewriter and throttle input handlers every frame
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        PortableTypewriterClientHandler.tick();
        PortableThrottleClientHandler.tick();
    }
}
