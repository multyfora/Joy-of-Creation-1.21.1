package net.multyfora.register;

import net.multyfora.content.physics_staff.CreativeStaffCaptureHandler;
import net.multyfora.content.portable_throttle.PortableThrottleServerHandler;
import net.multyfora.content.portable_typewriter.PortableTypewriterServerHandler;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public class EventsRegister {
    public static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, JocCommands::register);

        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (!event.getLevel().isClientSide()) {
                PortableTypewriterServerHandler.tick(event.getLevel());
                PortableThrottleServerHandler.tick(event.getLevel());
            }
        });

        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.ServerTickEvent.Post.class, CreativeStaffCaptureHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract.class, CreativeStaffCaptureHandler::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty.class, CreativeStaffCaptureHandler::onRightClickEmpty);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock.class, CreativeStaffCaptureHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.class, CreativeStaffCaptureHandler::onPlayerLogout);
    }
}
