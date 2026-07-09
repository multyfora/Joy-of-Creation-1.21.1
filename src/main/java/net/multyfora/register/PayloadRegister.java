package net.multyfora.register;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.multyfora.client.seeker.SeekerMenu;
import net.multyfora.client.seeker.SeekerScreen;
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.content.physics_staff.CreativeStaffCaptureHandler;
import net.multyfora.content.physics_staff.EntityGrabClientState;
import net.multyfora.network.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public class PayloadRegister {
    public static void RegisterPayloads(IEventBus modEventBus) {
        IPayloadHandler<SeekerPayloads.OpenSeekerPayload> SeekerPayloader =
            (payload, context) -> {
                context.enqueueWork(
                    () -> {
                        Player player = context.player();
                        Level level = player.level();
                        BlockEntity blockEntity = level.getBlockEntity( payload.pos() );

                        if( !(blockEntity instanceof SeekerBlockEntity seekerBlockEntity) ) {
                            return;
                        }

                        int x = payload.pos().getX();
                        int y = payload.pos().getY();
                        int z = payload.pos().getZ();
                        seekerBlockEntity.setTarget(x, y, z);

                        SeekerMenu seekerMenu = new SeekerMenu(
                            0, player.getInventory(), seekerBlockEntity
                        );
                        Minecraft.getInstance().setScreen(
                            new SeekerScreen( seekerMenu, player.getInventory() )
                        );
                    }
                );
            }
        ;
        IPayloadHandler<SeekerPayloads.ToggleModePayload> ToggleModePayloader =
                (payload, context) -> {
                    context.enqueueWork(
                            () -> {
                                Level level = context.player().level();
                                BlockEntity blockEntity = level.getBlockEntity( payload.pos() );
                                if( !(blockEntity instanceof SeekerBlockEntity seekerBlockEntity) ) {
                                    return;
                                }

                                seekerBlockEntity.setUse3D( payload.use3D() );
                            }
                    );
                }
                ;
        IPayloadHandler<SeekerPayloads.UpdateSeekerPayload> UpdateSeekerPayloader =
            (payload, context) -> {
                context.enqueueWork(
                    () -> {
                        Level level = context.player().level();
                        BlockEntity blockEntity = level.getBlockEntity( payload.pos() );
                        if( !(blockEntity instanceof SeekerBlockEntity seekerBlockEntity) ) {
                            return;
                        }

                        seekerBlockEntity.setTarget(
                            payload.x(),
                            payload.y(),
                            payload.z()
                        );
                        seekerBlockEntity.setChanged();
                        level.sendBlockUpdated(
                            payload.pos(),
                            seekerBlockEntity.getBlockState(),
                            seekerBlockEntity.getBlockState(),
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
                // Client-bound: opens the SeekerScreen GUI when the server tells the client to
                registrar.playToClient(
                    SeekerPayloads.OpenSeekerPayload.TYPE,
                    SeekerPayloads.OpenSeekerPayload.CODEC,
                    SeekerPayloader
                );
                // Server-bound: updates the SeekerBlockEntity's target coordinates from the GUI
                registrar.playToServer(
                    SeekerPayloads.UpdateSeekerPayload.TYPE,
                    SeekerPayloads.UpdateSeekerPayload.CODEC,
                    UpdateSeekerPayloader
                );
                // Server-bound: toggles 2D/3D calculation mode for the Seeker
                registrar.playToServer(
                        SeekerPayloads.ToggleModePayload.TYPE,
                        SeekerPayloads.ToggleModePayload.CODEC,
                        ToggleModePayloader
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

                // Shatter assembler: client → server assemble
                registrar.playToServer(
                    ShatterAssemblePacket.TYPE,
                    ShatterAssemblePacket.STREAM_CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                if( context.player() instanceof ServerPlayer sp ) {
                                    payload.handleServer(sp);
                                }
                            }
                        );
                    }
                );
            }
        );
    }
}
