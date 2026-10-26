package net.multyfora.register;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.multyfora.client.ClientPayloadHandlers;
import net.multyfora.content.portable_throttle.PortableThrottleHolders;
import net.multyfora.content.portable_typewriter.PortableTypewriterTypists;
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.content.shears_cut.ShearsCutRemoteState;
import net.multyfora.content.shears_cut.ShearsPreviewServerHandler;
import net.multyfora.content.physics_staff.CreativeStaffCaptureHandler;
import net.multyfora.content.physics_staff.EntityGrabClientState;
import net.multyfora.index.JocDataComponents;
import net.multyfora.index.SeekerCapturedTarget;
import net.multyfora.network.*;
import net.multyfora.network.ShearsCutPayloads.ShearsCutPayload;
import net.multyfora.network.ShearsCutPayloads.ShearsPreviewPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public class PayloadRegister {
    public static void RegisterPayloads(IEventBus modEventBus) {
        IPayloadHandler<SeekerPayloads.ToggleModePayload> ToggleModePayloader =
                (payload, context) -> {
                    context.enqueueWork(
                            () -> {
                                if (!(context.player() instanceof ServerPlayer sp) || sp.isSpectator()) return;
                                if (!sp.serverLevel().isLoaded(payload.pos())) return;
                                if (sp.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) > 256.0) return;
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
        IPayloadHandler<SeekerPayloads.SetSpyglassTargetPayload> SetSpyglassTargetPayloader =
            (payload, context) -> {
                context.enqueueWork(() -> {
                    var player = context.player();
                    if (player == null) return;

                    SeekerCapturedTarget target = new SeekerCapturedTarget(payload.targetPos(), payload.subLevelId(),
                        payload.localX(), payload.localY(), payload.localZ());

                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();

                    if (mainHand.is(Items.SPYGLASS)) {
                        mainHand.set(JocDataComponents.SEEKER_CARRIED_TARGET.get(), target);
                    } else if (offHand.is(Items.SPYGLASS)) {
                        offHand.set(JocDataComponents.SEEKER_CARRIED_TARGET.get(), target);
                    }
                });
            }
        ;
        IPayloadHandler<SeekerPayloads.UpdateSeekerPayload> UpdateSeekerPayloader =
            (payload, context) -> {
                context.enqueueWork(
                    () -> {
                        if (!(context.player() instanceof ServerPlayer sp) || sp.isSpectator()) return;
                        if (!sp.serverLevel().isLoaded(payload.pos())) return;
                        if (sp.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) > 256.0) return;
                        if (!Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
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

        modEventBus.addListener(
            net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class,
            (event) -> {
                var registrar = event.registrar("1.0.0");
                registrar.playToClient(
                    SeekerPayloads.OpenSeekerPayload.TYPE,
                    SeekerPayloads.OpenSeekerPayload.CODEC,
                    ClientPayloadHandlers::handleOpenSeeker
                );
                registrar.playToServer(
                    SeekerPayloads.UpdateSeekerPayload.TYPE,
                    SeekerPayloads.UpdateSeekerPayload.CODEC,
                    UpdateSeekerPayloader
                );
                registrar.playToServer(
                        SeekerPayloads.ToggleModePayload.TYPE,
                        SeekerPayloads.ToggleModePayload.CODEC,
                        ToggleModePayloader
                );
                registrar.playToServer(
                    SeekerPayloads.SetSpyglassTargetPayload.TYPE,
                    SeekerPayloads.SetSpyglassTargetPayload.CODEC,
                    SetSpyglassTargetPayloader
                );

                IPayloadHandler<SeekerDistancePayloads.UpdateSeekerDistancePayload> UpdateSeekerDistancePayloader =
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                if (!(context.player() instanceof ServerPlayer sp2) || sp2.isSpectator()) return;
                                if (!sp2.serverLevel().isLoaded(payload.pos())) return;
                                if (sp2.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) > 256.0) return;
                                if (!Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
                                if (!Double.isFinite(payload.minDistance()) || !Double.isFinite(payload.maxDistance())) return;
                                Level level = context.player().level();
                                BlockEntity blockEntity = level.getBlockEntity( payload.pos() );
                                if( !(blockEntity instanceof SeekerBlockEntity seekerBlockEntity) ) {
                                    return;
                                }
                                seekerBlockEntity.setTarget(payload.x(), payload.y(), payload.z());
                                seekerBlockEntity.setMinMaxDistance(payload.minDistance(), payload.maxDistance());
                                seekerBlockEntity.setChanged();
                                level.sendBlockUpdated(payload.pos(), seekerBlockEntity.getBlockState(), seekerBlockEntity.getBlockState(), 3);
                            }
                        );
                    }
                ;
                registrar.playToClient(
                    SeekerDistancePayloads.OpenSeekerDistancePayload.TYPE,
                    SeekerDistancePayloads.OpenSeekerDistancePayload.CODEC,
                    ClientPayloadHandlers::handleOpenSeekerDistance
                );
                registrar.playToServer(
                    SeekerDistancePayloads.UpdateSeekerDistancePayload.TYPE,
                    SeekerDistancePayloads.UpdateSeekerDistancePayload.CODEC,
                    UpdateSeekerDistancePayloader
                );
                registrar.playToServer(
                    PortableTypewriterInputPacket.TYPE,
                    PortableTypewriterInputPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                                () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                registrar.playToServer(
                    PortableTypewriterBindPacket.TYPE,
                    PortableTypewriterBindPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                registrar.playToServer(
                    PortableThrottleBindPacket.TYPE,
                    PortableThrottleBindPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                registrar.playToServer(
                    PortableThrottleConfigPacket.TYPE,
                    PortableThrottleConfigPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                registrar.playToServer(
                    PortableThrottleSignalPacket.TYPE,
                    PortableThrottleSignalPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );
                registrar.playToServer(
                    PortableTypewriterStatePayloads.SetTyping.TYPE,
                    PortableTypewriterStatePayloads.SetTyping.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                if( context.player() instanceof ServerPlayer sp ) {
                                    PortableTypewriterTypists.setTyping(sp, payload.typing());
                                }
                            }
                        );
                    }
                );
                registrar.playToServer(
                    PortableThrottlePosePayloads.SetPose.TYPE,
                    PortableThrottlePosePayloads.SetPose.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                if( context.player() instanceof ServerPlayer sp ) {
                                    PortableThrottleHolders.setPose(sp, payload.gripping(), payload.strength());
                                }
                            }
                        );
                    }
                );
                registrar.playToClient(
                    PortableThrottlePosePayloads.PoseState.TYPE,
                    PortableThrottlePosePayloads.PoseState.CODEC,
                    ClientPayloadHandlers::handleThrottlePose
                );
                registrar.playToClient(
                    PortableTypewriterStatePayloads.TypingState.TYPE,
                    PortableTypewriterStatePayloads.TypingState.CODEC,
                    ClientPayloadHandlers::handleTypingState
                );
                registrar.playToServer(
                    PortableTypewriterSetFreqPacket.TYPE,
                    PortableTypewriterSetFreqPacket.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> { payload.handle( context.player() ); }
                        );
                    }
                );

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

                registrar.playToClient(
                    EntityGrabPayloads.End.TYPE,
                    EntityGrabPayloads.End.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                EntityGrabClientState.grabbedEntityId = 0;
                            }
                        );
                    }
                );

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

                registrar.playToServer(
                    ShearsCutPayload.TYPE,
                    ShearsCutPayload.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                if( context.player() instanceof ServerPlayer sp ) {
                                    payload.handle(sp);
                                }
                            }
                        );
                    }
                );

                registrar.playBidirectional(
                    ShearsPreviewPayload.TYPE,
                    ShearsPreviewPayload.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(
                            () -> {
                                if (context.flow().isServerbound()) {
                                    if (context.player() instanceof ServerPlayer sp) {
                                        ShearsPreviewServerHandler.handle(payload, sp);
                                    }
                                } else {
                                    ShearsCutRemoteState.handle(payload);
                                }
                            }
                        );
                    }
                );
            }
        );
    }
}
