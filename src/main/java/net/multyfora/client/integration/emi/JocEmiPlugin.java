package net.multyfora.client.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.multyfora.client.portable_throttle.PortableThrottleLinkScreen;
import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;

@EmiEntrypoint
public class JocEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
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
