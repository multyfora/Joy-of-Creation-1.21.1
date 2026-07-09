package net.multyfora.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopePoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeDataPacket;

import foundry.veil.api.network.handler.ClientPacketContext;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.multyfora.IMultiRopeBehavior;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;

@Mixin(value = ClientboundRopeDataPacket.class, remap = false)
public abstract class ClientboundRopeDataPacketMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void joc$routeExtraStrand(ClientPacketContext context, CallbackInfo ci) {
        ClientboundRopeDataPacket self = (ClientboundRopeDataPacket) (Object) this;

        LocalPlayer player = context.player();
        Level level = player.level();
        BlockEntity blockEntity = level.getBlockEntity(self.ownerPos());
        if (!(blockEntity instanceof SmartBlockEntity smartBlockEntity)) return;

        RopeStrandHolderBehavior ropeHolder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
        if (!(ropeHolder instanceof IMultiRopeBehavior multi)) return;

        // Always cancel for multi-rope holders — handle everything ourselves
        ci.cancel();

        var allClientStrands = multi.joc$getAllClientStrands();
        if (allClientStrands == null) return;

        ClientRopeStrand strand = allClientStrands.get(self.uuid());
        if (strand == null) {
            strand = new ClientRopeStrand(self.uuid());
            multi.joc$putClientStrand(self.uuid(), strand);
        }
        ClientLevelRopeManager.getOrCreate(level).addStrand(strand);

        Vector3dc startPoint = joc$resolveAttachment(level, self.startAttachmentPos());
        Vector3dc endPoint = joc$resolveAttachment(level, self.endAttachmentPos());
        if (startPoint != null) strand.startAttachment = new net.minecraft.world.phys.Vec3(startPoint.x(), startPoint.y(), startPoint.z());
        if (endPoint != null) strand.endAttachment = new net.minecraft.world.phys.Vec3(endPoint.x(), endPoint.y(), endPoint.z());

        strand.setStopped(false);
        ObjectArrayList<ClientRopePoint> points = strand.getPoints();
        var incoming = self.points();

        while (points.size() < incoming.size()) {
            Vector3dc position = incoming.get(incoming.size() - points.size() - 1);
            points.addFirst(new ClientRopePoint(new Vector3d(position), new Vector3d(position), new ObjectArrayList<>()));
        }
        while (points.size() > incoming.size()) {
            points.removeFirst();
        }
        for (int i = 0; i < incoming.size(); i++) {
            points.get(i).snapshots().add(new ClientRopePoint.Snapshot(self.interpolationTick(), incoming.get(i)));
        }

        multi.joc$setOwnedClientStrand(strand);
    }

    @Nullable
    private static Vector3dc joc$resolveAttachment(Level level, @Nullable BlockPos pos) {
        if (pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SmartBlockEntity smartBlockEntity)) return null;
        RopeStrandHolderBehavior holder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
        if (holder == null) return null;
        var p = holder.getAttachmentPoint();
        return new Vector3d(p.x, p.y, p.z);
    }
}
