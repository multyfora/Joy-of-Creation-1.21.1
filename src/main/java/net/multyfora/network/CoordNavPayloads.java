package net.multyfora.network;

import net.minecraft.core.BlockPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.multyfora.AeronauticsJoyofcreation;

// Holds the two custom payload types used by the Coordinate Navigator block:
// OpenCoordNavPayload: sent from server to client to open the coordinate configuration GUI
// UpdateCoordPayload: sent from client to server when the user sets new target coordinates
public final class CoordNavPayloads {
    private CoordNavPayloads() {}

    // Server-to-client: tells the client to open the CoordNavScreen for the given block position
    public record OpenCoordNavPayload(BlockPos pos) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "open_coord_nav");
        public static final Type<OpenCoordNavPayload> TYPE = new Type<>(ID);
        // Stream codec: just a BlockPos (compact long encoding)
        public static final StreamCodec<ByteBuf, OpenCoordNavPayload> CODEC =
                BlockPos.STREAM_CODEC.map(OpenCoordNavPayload::new, OpenCoordNavPayload::pos);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // Client-to-server: sends updated target coordinates for a CoordNavBlockEntity
    public record UpdateCoordPayload(BlockPos pos, double x, double y, double z) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "update_coord");
        public static final Type<UpdateCoordPayload> TYPE = new Type<>(ID);
        // Stream codec: BlockPos + 3 doubles
        public static final StreamCodec<ByteBuf, UpdateCoordPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, UpdateCoordPayload::pos,
                ByteBufCodecs.DOUBLE, UpdateCoordPayload::x,
                ByteBufCodecs.DOUBLE, UpdateCoordPayload::y,
                ByteBufCodecs.DOUBLE, UpdateCoordPayload::z,
                UpdateCoordPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
