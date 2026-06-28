package net.multyfora.register;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.multyfora.client.coordnav.CoordNavMenu;
import net.multyfora.client.coordnav.CoordNavScreen;
import net.multyfora.content.coordnav.CoordNavBlockEntity;
import net.multyfora.content.physics_staff.CreativeStaffCaptureHandler;
import net.multyfora.content.physics_staff.EntityGrabClientState;
import net.multyfora.network.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public class PayloadRegister {
    public static void RegisterPayloads(IEventBus modEventBus) {
        IPayloadHandler<CoordNavPayloads.OpenCoordNavPayload> CoordNavPayloader =
            (payload, context) -> {
                context.enqueueWork(
                    () -> {
                        Player player = context.player();
                        Level level = player.level();
                        BlockEntity blockEntity = level.getBlockEntity( payload.pos() );

                        if( !(blockEntity instanceof CoordNavBlockEntity coordNavBlockEntity) ) {
                            return;
                        }

                        int x = payload.pos().getX();
                        int y = payload.pos().getY();
                        int z = payload.pos().getZ();
                        coordNavBlockEntity.setTarget(x, y, z);

                        CoordNavMenu coordNavMenu = new CoordNavMenu(
                            0, player.getInventory(), coordNavBlockEntity
                        );
                        Minecraft.getInstance().setScreen(
                            new CoordNavScreen( coordNavMenu, player.getInventory() )
                        );
                    }
                );
            }
        ;
        IPayloadHandler<CoordNavPayloads.UpdateCoordPayload> UpdateCoordPayloader =
            (payload, context) -> {
                context.enqueueWork(
                    () -> {
                        Level level = context.player().level();
                        BlockEntity blockEntity = level.getBlockEntity( payload.pos() );
                        if( !(blockEntity instanceof CoordNavBlockEntity coordNavBlockEntity) ) {
                            return;
                        }

                        coordNavBlockEntity.setTarget(
                            payload.x(),
                            payload.y(),
                            payload.z()
                        );
                        coordNavBlockEntity.setChanged();
                        level.sendBlockUpdated(
                            payload.pos(),
                            coordNavBlockEntity.getBlockState(),
                            coordNavBlockEntity.getBlockState(),
                            3
                        );
                    }
                );
            }
        ;

        // Register all custom network payloads (play-to-client and play-to-server)
        modEventBus.addListener(
            net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class,
            (event) -> {
                var registrar = event.registrar("1.0.0");
                // Client-bound: opens the CoordNavScreen GUI when the server tells the client to
                registrar.playToClient(
                    CoordNavPayloads.OpenCoordNavPayload.TYPE,
                    CoordNavPayloads.OpenCoordNavPayload.CODEC,
                    CoordNavPayloader
                );
                // Server-bound: updates the CoordNavBlockEntity's target coordinates from the GUI
                registrar.playToServer(
                    CoordNavPayloads.UpdateCoordPayload.TYPE,
                    CoordNavPayloads.UpdateCoordPayload.CODEC,
                    UpdateCoordPayloader
                );
                // Server-bound: handles typewriter key press/release input
                registrar.playToServer(
                    PortableTypewriterInputPacket.TYPE,
                    PortableTypewriterInputPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                                () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                // Server-bound: binds a keyboard key to a redstone link frequency for the typewriter
                registrar.playToServer(
                    PortableTypewriterBindPacket.TYPE,
                    PortableTypewriterBindPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                // Server-bound: binds the throttle item to a redstone link's frequency
                registrar.playToServer(
                    PortableThrottleBindPacket.TYPE,
                    PortableThrottleBindPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                // Server-bound: configures the throttle's frequency items
                registrar.playToServer(
                    PortableThrottleConfigPacket.TYPE,
                    PortableThrottleConfigPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                // Server-bound: sends throttle signal strength to the server
                registrar.playToServer(
                    PortableThrottleSignalPacket.TYPE,
                    PortableThrottleSignalPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                // Server-bound: sets frequency items for a typewriter key binding directly
                registrar.playToServer(
                    PortableTypewriterSetFreqPacket.TYPE,
                    PortableTypewriterSetFreqPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );

                // Entity grab: client → server stop
                registrar.playToServer(
                    EntityGrabPayloads.Stop.TYPE,
                    EntityGrabPayloads.Stop.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                if( context.player() instanceof ServerPlayer sp ) {
                                    CreativeStaffCaptureHandler.onEntityGrabStopC2S(payload, sp);
                                }
                            }
                        );
                    }
                );

                // Entity grab: client → server grab request (long-range entity pick)
                registrar.playToServer(
                    EntityGrabPayloads.GrabRequest.TYPE,
                    EntityGrabPayloads.GrabRequest.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                if( context.player() instanceof ServerPlayer sp) {
                                    CreativeStaffCaptureHandler.onEntityGrabRequestC2S(payload, sp);
                                }
                            }
                        );
                    }
                );

                // Entity grab: server → client start
                registrar.playToClient(
                    EntityGrabPayloads.Start.TYPE,
                    EntityGrabPayloads.Start.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                EntityGrabClientState.grabbedEntityId = payload.entityId();
                                EntityGrabClientState.holdDistance = payload.holdDistance();
                            }
                        );
                    }
                );

                // Entity grab: client → server set hold distance
                registrar.playToServer(
                    EntityGrabPayloads.SetHoldDistance.TYPE,
                    EntityGrabPayloads.SetHoldDistance.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                if( context.player() instanceof ServerPlayer sp ) {
                                    CreativeStaffCaptureHandler.onSetHoldDistance(payload, sp);
                                }
                            }
                        );
                    }
                );
            }
        );
    }
}
