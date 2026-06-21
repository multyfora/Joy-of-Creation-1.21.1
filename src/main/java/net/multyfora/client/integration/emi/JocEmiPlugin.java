package net.multyfora.client.integration.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.multyfora.client.portable_throttle.PortableThrottleScreen;
import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;

public class JocEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(PortableTypewriterScreen.class, new TypewriterDragHandler());
        registry.addDragDropHandler(PortableThrottleScreen.class, new ThrottleDragHandler());
    }
}
