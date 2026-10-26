package net.multyfora.network;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.shatter_assembler.ShatterAssemblerBlock;
import net.multyfora.content.shatter_assembler.ShatterAssemblerBlockEntity;
import net.multyfora.content.shatter_assembler.ShatterLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.Sable;

import java.util.Collection;

public record ShatterAssemblePacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ShatterAssemblePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "shatter_assemble"));
    public static final StreamCodec<FriendlyByteBuf, ShatterAssemblePacket> STREAM_CODEC = CustomPacketPayload.codec(ShatterAssemblePacket::write, ShatterAssemblePacket::new);

    public ShatterAssemblePacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleServer(ServerPlayer player) {
        if (player.isSpectator()) return;
        Level level = player.level();
        if (!level.isLoaded(this.pos)) return;
        Vec3 worldPos = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(this.pos));
        if (player.distanceToSqr(worldPos) > 64.0) return;
        BlockEntity be = level.getBlockEntity(this.pos);

        if (be instanceof ShatterAssemblerBlockEntity assembler) {
            Direction facing = ShatterAssemblerBlock.getStickyFacing(assembler.getBlockState());
            BlockPos targetPos = this.pos.relative(facing);
            BlockState targetState = level.getBlockState(targetPos);
            if (!targetState.isAir() && !ShatterLogic.canAssemble(level, targetPos, targetState)) {
                player.displayClientMessage(Component.translatable(
                        "joc.shatter_assembler.immovable", targetState.getBlock().getName()), true);
                return;
            }

            Collection<BlockPos> connectedBlocks = ShatterLogic.getConnectedBlocks(level, targetPos);

            if (!connectedBlocks.isEmpty()) {
                ShatterLogic.shatterStructure(level, connectedBlocks);
            }
        }
    }
}
