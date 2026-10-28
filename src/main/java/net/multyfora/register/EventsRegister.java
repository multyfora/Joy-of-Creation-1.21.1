package net.multyfora.register;

import net.multyfora.config.FeatureGuards;
import net.multyfora.content.physics_staff.CreativeStaffCaptureHandler;
import net.multyfora.content.portable_throttle.PortableThrottleHolders;
import net.multyfora.content.portable_throttle.PortableThrottleServerHandler;
import net.multyfora.content.portable_typewriter.PortableTypewriterServerHandler;
import net.multyfora.content.portable_typewriter.PortableTypewriterTypists;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public class EventsRegister {
    public static void registerEvents() {
        FeatureGuards.register();

        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (!event.getLevel().isClientSide()) {
                PortableTypewriterServerHandler.tick(event.getLevel());
                PortableThrottleServerHandler.tick(event.getLevel());
            }
        });

        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post.class, PortableTypewriterTypists::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking.class, PortableTypewriterTypists::onStartTracking);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.class, PortableTypewriterTypists::onLogout);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post.class, PortableThrottleHolders::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking.class, PortableThrottleHolders::onStartTracking);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.class, PortableThrottleHolders::onLogout);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post.class, PortableThrottleServerHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.class, PortableThrottleServerHandler::onLogout);

        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.ServerTickEvent.Post.class, CreativeStaffCaptureHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract.class, CreativeStaffCaptureHandler::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty.class, CreativeStaffCaptureHandler::onRightClickEmpty);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock.class, CreativeStaffCaptureHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.class, CreativeStaffCaptureHandler::onPlayerLogout);
    }
}
