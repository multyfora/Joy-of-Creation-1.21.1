package net.multyfora.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import dev.simulated_team.simulated.network.packets.RopeBreakPacket;

import foundry.veil.api.network.handler.ServerPacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.multyfora.IMultiRopeBehavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RopeBreakPacket.class, remap = false)
public abstract class RopeBreakPacketMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void joc$breakSpecificStrand(ServerPacketContext context, CallbackInfo ci) {
        RopeBreakPacket self = (RopeBreakPacket) (Object) this;

        ServerPlayer player = context.player();
        Level level = player.level();

        ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
        ServerRopeStrand strand = manager.getStrand(self.uuid());
        if (strand == null) return;

        RopeAttachment start = strand.getAttachment(RopeAttachmentPoint.START);
        if (start == null) return;

        BlockPos blockAttachment = start.blockAttachment();
        BlockEntity blockEntity = level.getBlockEntity(blockAttachment);
        if (!(blockEntity instanceof SmartBlockEntity smartBlockEntity)) return;

        RopeStrandHolderBehavior holder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
        if (!(holder instanceof IMultiRopeBehavior multi)) return;

        if (multi.joc$getMaxRopeAttachments() <= 1) return;

        if (multi.joc$destroyRopeByUUID(self.uuid(), player, !player.hasInfiniteMaterials())) {
            ci.cancel();
        }
    }
}