package net.multyfora.mixin;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeTrackingSystem;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.multyfora.IMultiRopeBehavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The base tracking system builds update packets from the holder's primary strand only,
 * so it corrupts extra strands' client data. Non-primary strands are now synced by
 * RopeStrandHolderBehavior's own tick; this keeps them out of the base sync path while
 * still managing their stop transition (the stop packet is position-based and stops
 * every client strand at that position).
 **/
@Mixin(value = ServerRopeTrackingSystem.class, remap = false)
public abstract class ServerRopeTrackingSystemMixin {

    @Shadow private ServerLevel level;

    @Shadow private ServerLevelRopeManager getRopeManager() {
        throw new AssertionError();
    }

    private boolean joc$isExtraOfMulti(ServerRopeStrand strand) {
        RopeAttachment attachment = strand.getAttachment(RopeAttachmentPoint.START);
        if (attachment == null) return false;
        BlockPos block = attachment.blockAttachment();
        RopeStrandHolderBehavior holder = RopeStrandHolderBehavior.get(this.level.getBlockEntity(block), RopeStrandHolderBehavior.TYPE);
        if (holder == null) return false;
        return holder instanceof IMultiRopeBehavior && strand != holder.getOwnedStrand();
    }

    @Inject(method = "neededPlayers", at = @At("HEAD"), cancellable = true)
    private void joc$neededPlayers(CallbackInfoReturnable<Iterable<UUID>> cir) {
        Collection<ServerRopeStrand> strands = this.getRopeManager().getAllStrands();
        if (strands.isEmpty()) {
            cir.setReturnValue(List.of());
            return;
        }

        final Set<UUID> players = new ObjectOpenHashSet<>();

        for (final ServerRopeStrand strand : strands) {
            if (!strand.isActive()) continue;
            if (joc$isExtraOfMulti(strand)) continue;

            strand.updatePose();
            if (!strand.needsSync() && strand.networkingStopped) {
                continue;
            }

            final RopeAttachment attachment = strand.getAttachment(RopeAttachmentPoint.START);
            final BlockPos block = attachment.blockAttachment();
            final RopeStrandHolderBehavior holder = RopeStrandHolderBehavior.get(this.level.getBlockEntity(block), RopeStrandHolderBehavior.TYPE);

            if (holder == null) {
                continue;
            }

            for (final ServerPlayer player : holder.getStrandTrackingPlayers()) {
                players.add(player.getUUID());
            }
        }

        cir.setReturnValue(players);
    }

    @Inject(method = "sendTrackingData", at = @At("HEAD"), cancellable = true)
    private void joc$sendTrackingData(int interpolationTick, CallbackInfo ci) {
        final ServerLevelRopeManager ropeManager = this.getRopeManager();

        for (final ServerRopeStrand strand : ropeManager.getAllStrands()) {
            if (!strand.isActive()) continue;

            final RopeAttachment attachment = strand.getAttachment(RopeAttachmentPoint.START);
            if (attachment == null) continue;
            final BlockPos block = attachment.blockAttachment();
            final RopeStrandHolderBehavior holder = RopeStrandHolderBehavior.get(this.level.getBlockEntity(block), RopeStrandHolderBehavior.TYPE);
            if (holder == null) continue;

            if (joc$isExtraOfMulti(strand)) {
                if (!strand.needsSync() && !strand.networkingStopped) {
                    strand.networkingStopped = true;
                    holder.getStrandPacketSink().sendPacket(holder.makeStopPacket());
                }
                continue;
            }

            if (strand.needsSync()) {
                strand.networkingStopped = false;
                holder.getStrandPacketSink().sendPacket(holder.makeUpdatePacket());
                strand.justSynced();
            } else if (!strand.networkingStopped) {
                strand.networkingStopped = true;
                holder.getStrandPacketSink().sendPacket(holder.makeStopPacket());
            }
        }

        ci.cancel();
    }
}