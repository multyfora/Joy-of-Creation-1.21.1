package net.multyfora.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeStoppedPacket;

import foundry.veil.api.network.handler.ClientPacketContext;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.multyfora.IMultiRopeBehavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(value = ClientboundRopeStoppedPacket.class, remap = false)
public abstract class ClientboundRopeStoppedPacketMixin {

    @Inject(method = "handle", at = @At("TAIL"))
    private void joc$stopExtraStrands(ClientPacketContext context, CallbackInfo ci) {
        ClientboundRopeStoppedPacket self = (ClientboundRopeStoppedPacket) (Object) this;

        LocalPlayer player = context.player();
        Level level = player.level();
        BlockEntity blockEntity = level.getBlockEntity(self.ownerPos());
        if (!(blockEntity instanceof SmartBlockEntity smartBlockEntity)) return;

        RopeStrandHolderBehavior ropeHolder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
        if (!(ropeHolder instanceof IMultiRopeBehavior multi)) return;

        Map<UUID, ClientRopeStrand> extras = multi.joc$getAllClientStrands();
        if (extras == null) return;

        for (ClientRopeStrand strand : extras.values()) {
            strand.setStopped(true);
        }
    }
}
