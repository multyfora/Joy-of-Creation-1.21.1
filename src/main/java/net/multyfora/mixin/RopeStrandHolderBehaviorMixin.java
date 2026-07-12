package net.multyfora.mixin;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeDataPacket;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.multyfora.IMultiRopeBehavior;
import net.multyfora.mixin.ClientboundRopeDataPacketMixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//TODO: BROKEN and I am NOT fixing this in the near future

/**
 * Core mixin to RopeStrandHolderBehavior (Simulated mod) that extends the single-rope
 * system to support multiple simultaneous rope attachments on a single block.
 * Tracks multiple rope IDs (allAttachedIDs) and multiple owned server strands
 * (allOwnedStrands), intercepts rope creation and destruction, handles per-rope
 * destruction, and provides serialization for the extended state.
 **/
@Mixin(value = RopeStrandHolderBehavior.class, remap = false)
public abstract class RopeStrandHolderBehaviorMixin implements IMultiRopeBehavior {

    // Shadowed fields from the original RopeStrandHolderBehavior
    @Shadow @Nullable private UUID attachedRopeID;
    @Shadow private boolean strandOwner;
    @Shadow @Nullable private ServerRopeStrand ownedServerStrand;
    @Shadow @Nullable private ClientRopeStrand ownedClientStrand;
    @Shadow public boolean renderAttached;
    @Shadow public abstract boolean isAttached();
    @Shadow public abstract @Nullable ServerRopeStrand getOwnedStrand();
    @Shadow public abstract @Nullable ServerRopeStrand getAttachedStrand();
    @Shadow public abstract Vec3 getAttachmentPoint();
    @Shadow public abstract SubLevelPhysicsSystem getPhysicsSystem();
    @Shadow public abstract void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket);
    @Shadow public abstract void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket);
    @Shadow protected abstract void addServerStrand(ServerRopeStrand strand);
    @Shadow public abstract @Nullable Level getLevel();
    @Shadow public abstract VeilPacketManager.PacketSink getStrandPacketSink();

    // Extended tracking lists for multiple ropes
    @Unique
    private final List<UUID> joc$allAttachedIDs = new ArrayList<>(); // All rope UUIDs attached to this holder

    @Unique
    private final Map<UUID, ClientRopeStrand> joc$allClientStrands = new Object2ObjectOpenHashMap<>();
    @Unique
    private final List<ServerRopeStrand> joc$allOwnedStrands = new ArrayList<>(); // All server strands owned by this holder

    @Unique
    private int joc$maxRopeAttachments = 1; // Maximum ropes; 1 by default (single rope behaviour)

    // Temporary state saved during rope creation to transfer attachments
    @Unique @Nullable private UUID joc$preExistingID;
    @Unique @Nullable private ServerRopeStrand joc$preExistingStrand;
    @Unique @Nullable private UUID joc$targetPreExistingID;
    // Gets the SmartBlockEntity from this behaviour (the behaviour itself is cast from this mixin)
    @Unique
    private SmartBlockEntity joc$be() {
        return ((BlockEntityBehaviour) (Object) this).blockEntity;
    }

    // Gets the SmartBlockEntity from another behaviour instance
    @Unique
    private static SmartBlockEntity joc$be(RopeStrandHolderBehavior behavior) {
        return behavior.blockEntity;
    }

    @Unique
    private void joc$tryResolveOwnedStrands() {
        if (joc$allAttachedIDs.isEmpty()) return;
        SmartBlockEntity be = joc$be();
        Level level = be.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(serverLevel);
        BlockPos myPos = be.getBlockPos();
        for (UUID id : joc$allAttachedIDs) {
            if (joc$allOwnedStrands.stream().anyMatch(s -> s.getUUID().equals(id))) continue;
            ServerRopeStrand strand = manager.getStrand(id);
            if (strand == null) continue;
            RopeAttachment start = strand.getAttachment(RopeAttachmentPoint.START);
            if (start != null && myPos.equals(start.blockAttachment())) {
                joc$allOwnedStrands.add(strand);
            }
        }
    }

    // IMultiRopeBehavior implementation

    @Override
    public void joc$setMaxRopeAttachments(int max) {
        this.joc$maxRopeAttachments = max;
    }

    @Override
    public boolean joc$canAcceptAnotherRope() {
        return joc$allAttachedIDs.size() < joc$maxRopeAttachments;
    }

    @Override
    public @Nullable UUID joc$getAttachedRopeID() {
        return attachedRopeID;
    }

    @Override
    public void joc$clearAttachedRopeID() {
        this.attachedRopeID = null;
    }

    @Override
    public void joc$onRopeCreated(UUID ropeID, @Nullable UUID previousRopeID) {
        if( ropeID != null && !joc$allAttachedIDs.contains(ropeID) ) {
            joc$allAttachedIDs.add(ropeID);
        }
        if( previousRopeID != null && !joc$allAttachedIDs.contains(previousRopeID) ) {
            joc$allAttachedIDs.add(previousRopeID);
        }
    }

    @Override
    public void joc$onRopeDestroyed(UUID ropeID) {
        joc$allAttachedIDs.remove(ropeID);
    }

    /**
     * Rope creation interception
     * Saves the current state before createRope runs so we can restore/reference it after
     **/
    @Inject(
            method = "createRope",
            at = @At("HEAD")
    )
    private void joc$savePreCreateState(RopeStrandHolderBehavior target, boolean dropItem, CallbackInfoReturnable<Boolean> cir) {
        if( 1 < joc$maxRopeAttachments && attachedRopeID != null ) {
            joc$preExistingID = attachedRopeID;
            joc$preExistingStrand = ownedServerStrand;
        } else {
            joc$preExistingID = null;
            joc$preExistingStrand = null;
        }
        if(target instanceof IMultiRopeBehavior multiTarget) {
            // Clear the target's attached rope ID temporarily so createRope doesn't see it as occupied
            joc$targetPreExistingID = multiTarget.joc$getAttachedRopeID();
            multiTarget.joc$clearAttachedRopeID();
        } else {
            joc$targetPreExistingID = null;
        }
    }

    /**
     * Redirects the destroyRope call inside createRope to skip it for multi-rope holders
     * (we handle destruction ourselves to preserve other rope attachments)
     **/
    @Redirect(
            method = "createRope",
            at = @At(value = "INVOKE", target = "Ldev/simulated_team/simulated/content/blocks/rope/RopeStrandHolderBehavior;destroyRope(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/phys/Vec3;Z)V")
    )
    private void joc$skipDestroyForMulti(RopeStrandHolderBehavior instance, @Nullable ServerPlayer player, @Nullable Vec3 pos, boolean dropItems) {
        if(joc$maxRopeAttachments <= 1) {
            instance.destroyRope(player, pos, dropItems);
        }
    }

    // After createRope succeeds: update tracking lists, notify balloon, and restore state
    @Inject(
            method = "createRope",
            at = @At("RETURN")
    )
    private void joc$postCreateRope(RopeStrandHolderBehavior target, boolean dropItem, CallbackInfoReturnable<Boolean> cir) {
        if( !cir.getReturnValue() ) {
            return;
        }

        // Notify the target that a rope was created, registering it in the multi-rope list
        if(target instanceof IMultiRopeBehavior multiTarget) {
            multiTarget.joc$onRopeCreated(attachedRopeID, joc$targetPreExistingID);
            joc$be(target).notifyUpdate();
        }

        if(joc$maxRopeAttachments <= 1) {
            return;
        }

        // Add the new rope ID and strand to the extended tracking lists
        if( attachedRopeID != null && !joc$allAttachedIDs.contains(attachedRopeID) ) {
            joc$allAttachedIDs.add(attachedRopeID);
        }
        if( joc$preExistingID != null && !joc$allAttachedIDs.contains(joc$preExistingID) ) {
            joc$allAttachedIDs.add(joc$preExistingID);
        }

        if( ownedServerStrand != null && !joc$allOwnedStrands.contains(ownedServerStrand) ) {
            joc$allOwnedStrands.add(ownedServerStrand);
        }
        if( joc$preExistingStrand != null && !joc$allOwnedStrands.contains(joc$preExistingStrand) ) {
            joc$allOwnedStrands.add(joc$preExistingStrand);
        }

        joc$preExistingID = null;
        joc$preExistingStrand = null;
        joc$targetPreExistingID = null;
    }

    /**
     * Intercepts destroyRope to provide multi-rope destruction logic.
     * If there are multiple ropes, cancels the original destroy and handles it ourselves.
     **/
    @Inject(
            method = "destroyRope",
            at = @At("HEAD"),
            cancellable = true
    )
    private void joc$interceptDestroyRope(ServerPlayer player, Vec3 ropeDropPos, boolean returnItem, CallbackInfo ci) {
        if( joc$allAttachedIDs.size() <= 1 && joc$preExistingID == null ) {
            return; // Single rope: let the original method handle it
        }

        // If we're not the strand owner and have no owned strands, cancel (nothing to destroy)
        if(
                !this.strandOwner
                        || ( ownedServerStrand == null && joc$allOwnedStrands.isEmpty() )
        ) {
            ci.cancel();
            return;
        }

        // If a drop position is specified, try to find which specific strand to destroy
        if(ropeDropPos != null) {
            ServerRopeStrand found = null;
            for(ServerRopeStrand s : joc$allOwnedStrands) {
                RopeAttachment endAttach = s.getAttachment(RopeAttachmentPoint.END);
                if(endAttach != null && endAttach.blockAttachment() != null) {
                    Vec3 endCenter = Vec3.atCenterOf(endAttach.blockAttachment());
                    if(endCenter.distanceToSqr(ropeDropPos) < 2.0) {
                        found = s;
                        break;
                    }
                }
            }
            if(found != null) {
                // Destroy only the found strand
                joc$destroySingleStrand(found, player, ropeDropPos);
                ci.cancel();
                return;
            }
        }

        // No specific strand found; destroy all ropes
        joc$destroyAllRopes(player, ropeDropPos);
        ci.cancel();
    }

    // Destroys a single rope strand while preserving all others attached to this holder
    @Unique
    private void joc$destroySingleStrand(ServerRopeStrand strand, @Nullable ServerPlayer player, @Nullable Vec3 dropPos) {
        Level level = joc$be().getLevel();
        if(level == null) {
            return;
        }

        // Clean up the end attachment (e.g., target holder)
        RopeAttachment endAttach = strand.getAttachment(RopeAttachmentPoint.END);
        if(endAttach != null) {
            BlockPos targetPos = endAttach.blockAttachment();
            if(targetPos != null) {
                if(level.getBlockEntity(targetPos) instanceof SmartBlockEntity sbe) {
                    RopeStrandHolderBehavior beh = sbe.getBehaviour(RopeStrandHolderBehavior.TYPE);
                    if(beh != null) {
                        if (beh instanceof IMultiRopeBehavior multiBeh) {
                            multiBeh.joc$onRopeDestroyed(strand.getUUID());
                        } else {
                            beh.detachRope();
                        }
                        joc$be(beh).notifyUpdate();
                    }
                }
            }
        }

        // Remove the strand from physics and the server-level rope manager
        if(strand.isActive()) {
            strand.updatePose();
        }
        this.getPhysicsSystem().removeObject(strand);
        ServerLevelRopeManager.getOrCreate(level).removeStrand(strand.getUUID());

        // Remove from tracking lists
        joc$allAttachedIDs.remove(strand.getUUID());
        joc$allOwnedStrands.remove(strand);

        // If the destroyed strand was the primary one, promote another to primary
        if( strand.getUUID().equals(attachedRopeID) ) {
            attachedRopeID = joc$allAttachedIDs.isEmpty() ? null : joc$allAttachedIDs.getFirst();
            ownedServerStrand = joc$allOwnedStrands.isEmpty() ? null : joc$allOwnedStrands.getFirst();
            if(joc$allOwnedStrands.isEmpty()) {
                strandOwner = false;
            }
        }
        if( joc$allAttachedIDs.isEmpty() ) {
            strandOwner = false;
        }

        joc$be().notifyUpdate();
    }

    // Destroys all rope strands owned by this holder
    @Unique
    private void joc$destroyAllRopes(@Nullable ServerPlayer player, @Nullable Vec3 dropPos) {
        Level level = joc$be().getLevel();
        if(level == null) {
            return;
        }

        // Clean up all end attachments and remove all strands
        for( ServerRopeStrand strand : List.copyOf(joc$allOwnedStrands) ) {
            RopeAttachment endAttach = strand.getAttachment(RopeAttachmentPoint.END);
            if(endAttach != null) {
                BlockPos targetPos = endAttach.blockAttachment();
                if(targetPos != null) {
                    if(level.getBlockEntity(targetPos) instanceof SmartBlockEntity sbe) {
                        RopeStrandHolderBehavior beh = sbe.getBehaviour(RopeStrandHolderBehavior.TYPE);
                        if(beh != null) {
                            if (beh instanceof IMultiRopeBehavior multiBeh) {
                                multiBeh.joc$onRopeDestroyed(strand.getUUID());
                            } else {
                                beh.detachRope();
                            }
                            joc$be(beh).notifyUpdate();
                        }
                    }
                }
            }
            if( strand.isActive() ) {
                strand.updatePose();
            }
            this.getPhysicsSystem().removeObject(strand);
            ServerLevelRopeManager.getOrCreate(level).removeStrand(strand.getUUID());
        }

        // Clear all tracking state
        joc$allAttachedIDs.clear();
        joc$allOwnedStrands.clear();
        attachedRopeID = null;
        strandOwner = false;
        ownedServerStrand = null;
        joc$be().notifyUpdate();
    }

    // Clears extended lists when the rope is fully detached
    @Inject(method = "detachRope", at = @At("HEAD"))
    private void joc$clearListsOnDetach(CallbackInfo ci) {
        joc$allAttachedIDs.clear();
        joc$allOwnedStrands.clear();
    }

    /**
     * Tick handler for extra strands (server side): ensures they are active in the physics
     * system, syncs their point data to tracking players, and validates their end
     * attachments still exist
     **/
    @Inject(method = "tick", at = @At("RETURN"))
    private void joc$tickExtraStrands(CallbackInfo ci) {
        if( joc$allOwnedStrands.isEmpty() && !joc$allAttachedIDs.isEmpty() ) {
            joc$tryResolveOwnedStrands();
        }
        if( joc$allOwnedStrands.isEmpty() ) {
            return;
        }
        if( joc$be().getLevel() == null ) {
            return;
        }
        if( !(joc$be().getLevel() instanceof ServerLevel serverLevel) ) {
            return;
        }

        SubLevelPhysicsSystem system = this.getPhysicsSystem();
        if(system == null) {
            return;
        }

        for(int i = joc$allOwnedStrands.size() - 1; 0 <= i; i--) {
            ServerRopeStrand strand = joc$allOwnedStrands.get(i);
            // Skip the primary strand (ticked by original code)
            if(strand == this.ownedServerStrand) {
                continue;
            }

            // Activate the strand in the physics system if it's not already active
            if( !strand.isActive() ) {
                boolean loaded = strand.areAttachmentsLoaded(serverLevel);
                if(!loaded) {
                    continue;
                }
                system.addObject(strand);
            }

            if( strand.isActive() ) {
                strand.updatePose();
            }

            if (strand.needsSync()) {
                ClientboundRopeDataPacket packet = joc$buildPacketFor(strand);
                if (packet != null) {
                    this.getStrandPacketSink().sendPacket(packet);
                    strand.justSynced();
                }
            }

            // Validate end attachment: if the target block no longer has a rope holder, destroy the strand
            RopeAttachment endAttach = strand.getAttachment(RopeAttachmentPoint.END);
            if(endAttach != null) {
                BlockPos targetPos = endAttach.blockAttachment();
                if(targetPos != null) {
                    BlockEntity be = serverLevel.getBlockEntity(targetPos);
                    if(
                            !(be instanceof SmartBlockEntity)
                                    || (  (SmartBlockEntity)be ).getBehaviour(RopeStrandHolderBehavior.TYPE  ) == null
                    ) {
                        joc$destroySingleStrand(strand, null, Vec3.atCenterOf(targetPos));
                    }
                }
            }
        }
    }

    @Unique
    private @Nullable ClientboundRopeDataPacket joc$buildPacketFor(ServerRopeStrand strand) {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return null;

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(serverLevel);
        if (container == null) return null;
        SubLevelTrackingSystem trackingSystem = container.trackingSystem();

        RopeAttachment startAttachment = strand.getAttachment(RopeAttachmentPoint.START);
        RopeAttachment endAttachment = strand.getAttachment(RopeAttachmentPoint.END);

        return new ClientboundRopeDataPacket(
                trackingSystem.getInterpolationTick(),
                joc$be().getBlockPos(),
                strand.getUUID(),
                new ObjectArrayList<>(strand.getPoints()),
                startAttachment != null ? startAttachment.blockAttachment() : null,
                endAttachment != null ? endAttachment.blockAttachment() : null
        );
    }

    // IMultiRopeBehavior: client-side strand map access

    @Override
    public @Nullable Map<UUID, ClientRopeStrand> joc$getAllClientStrands() {
        return joc$allClientStrands;
    }

    @Override
    public void joc$putClientStrand(UUID uuid, ClientRopeStrand strand) {
        joc$allClientStrands.put(uuid, strand);
    }

    @Override
    public void joc$removeClientStrand(UUID uuid) {
        ClientRopeStrand removed = joc$allClientStrands.remove(uuid);
        if (removed != null) {
            Level level = getLevel();
            if (level != null) {
                ClientLevelRopeManager.getOrCreate(level).removeStrand(uuid);
            }
        }
    }

    @Override
    public @Nullable ClientRopeStrand joc$getOwnedClientStrand() {
        return this.ownedClientStrand;
    }

    @Override
    public void joc$setOwnedClientStrand(@Nullable ClientRopeStrand strand) {
        this.ownedClientStrand = strand;
    }

    @Override
    public boolean joc$isStrandOwner() {
        return this.strandOwner;
    }

    @Override
    public void joc$setStrandOwner(boolean owned) {
        this.strandOwner = owned;
    }

    // Client-side tick: keeps extra strands registered in ClientLevelRopeManager each frame
    @Inject(method = "tick", at = @At("RETURN"))
    private void joc$tickExtraClientStrands(CallbackInfo ci) {
        Level level = getLevel();
        if (level == null || !level.isClientSide) return;
        if (joc$allClientStrands.isEmpty()) return;

        ClientLevelRopeManager manager = ClientLevelRopeManager.getOrCreate(level);
        for (ClientRopeStrand strand : joc$allClientStrands.values()) {
            manager.addStrand(strand);
        }
    }

    /**
     * NBT serialization
     * Writes the extended multi-rope data to NBT: all attached rope UUIDs and max attachments
     **/
    @Inject(
            method = "write",
            at = @At("RETURN")
    )
    private void joc$writeExtra(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if( joc$allAttachedIDs.isEmpty() ) {
            return;
        }
        // Pack UUIDs into an int array (4 ints per UUID: hi>>32, hi, lo>>32, lo)
        int[] uuids = new int[joc$allAttachedIDs.size() * 4];
        int idx = 0;
        for (UUID id : joc$allAttachedIDs) {
            uuids[idx++] = (int)(id.getMostSignificantBits() >> 32);
            uuids[idx++] = (int)id.getMostSignificantBits();
            uuids[idx++] = (int)(id.getLeastSignificantBits() >> 32);
            uuids[idx++] = (int)id.getLeastSignificantBits();
        }
        nbt.putIntArray("joc:extra_rope_ids", uuids);
        nbt.putInt("joc:max_rope_attachments", joc$maxRopeAttachments);
    }

    // Reads the extended multi-rope data from NBT
    @Inject(
            method = "read",
            at = @At("RETURN")
    )
    private void joc$readExtra(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        joc$allAttachedIDs.clear();
        if( nbt.contains("joc:extra_rope_ids") ) {
            int[] uuids = nbt.getIntArray("joc:extra_rope_ids");
            for(int i = 0; i < uuids.length; i += 4) {
                long hi = ((long)uuids[i] << 32) | (uuids[i+1] & 0xFFFFFFFFL);
                long lo = ((long)uuids[i+2] << 32) | (uuids[i+3] & 0xFFFFFFFFL);
                joc$allAttachedIDs.add(new UUID(hi, lo));
            }
        }
        if( nbt.contains("joc:max_rope_attachments") ) {
            joc$maxRopeAttachments = nbt.getInt("joc:max_rope_attachments");
        }
        joc$allOwnedStrands.clear();
        if (!joc$allAttachedIDs.isEmpty()) {
            joc$tryResolveOwnedStrands();
        }
    }

    /**
     * Cleanup
     * On unload: clean up all extra-owned strands from the physics system, and (client-side)
     * remove all extra client strands from the render manager
     **/
    @Inject(
            method = "unload",
            at = @At("HEAD")
    )
    private void joc$cleanupOnUnload(CallbackInfo ci) {
        if( !joc$allOwnedStrands.isEmpty() ) {
            Level level = joc$be().getLevel();
            for( ServerRopeStrand strand : List.copyOf(joc$allOwnedStrands) ) {
                if(strand == ownedServerStrand) {
                    continue;
                }
                if( strand.isActive() ) {
                    strand.updatePose();
                }
                this.getPhysicsSystem().removeObject(strand);
                if(level != null) {
                    ServerLevelRopeManager.getOrCreate(level).removeStrand( strand.getUUID() );
                }
            }
            joc$allOwnedStrands.clear();
            joc$allAttachedIDs.clear();
        }

        Level lvl = getLevel();
        if (lvl != null && lvl.isClientSide && !joc$allClientStrands.isEmpty()) {
            ClientLevelRopeManager manager = ClientLevelRopeManager.getOrCreate(lvl);
            for (UUID uuid : joc$allClientStrands.keySet()) {
                manager.removeStrand(uuid);
            }
            joc$allClientStrands.clear();
        }
    }

    // On destroy: destroy all extra-owned strands
    @Inject(
            method = "destroy",
            at = @At("HEAD")
    )
    private void joc$cleanupOnDestroy(CallbackInfo ci) {
        if( !joc$allOwnedStrands.isEmpty() ) {
            joc$destroyAllRopes(null, null);
        }
    }
}