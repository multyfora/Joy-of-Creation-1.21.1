package net.multyfora;

import net.multyfora.content.portable_throttle.PortableThrottleRenderHandler;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public class AeronauticsJoyofcreationClient {
    public static final PortableThrottleRenderHandler PORTABLE_THROTTLE_RENDER_HANDLER = new PortableThrottleRenderHandler();

    public AeronauticsJoyofcreationClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        PORTABLE_THROTTLE_RENDER_HANDLER.registerListeners(NeoForge.EVENT_BUS);
    }
}
