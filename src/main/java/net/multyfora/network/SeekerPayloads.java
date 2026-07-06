package net.multyfora.network;

import net.minecraft.core.BlockPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.multyfora.AeronauticsJoyofcreation;

public final class SeekerPayloads {
    private SeekerPayloads() {}

    // Server-to-client: tells the client to open the SeekerScreen for the given block position
    public record OpenSeekerPayload(BlockPos pos) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "open_seeker");
        public static final Type<OpenSeekerPayload> TYPE = new Type<>(ID);
        public static final StreamCodec<ByteBuf, OpenSeekerPayload> CODEC =
                BlockPos.STREAM_CODEC.map(OpenSeekerPayload::new, OpenSeekerPayload::pos);
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // Client-to-server: sends updated target coordinates for a SeekerBlockEntity
    public record UpdateSeekerPayload(BlockPos pos, double x, double y, double z) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "update_seeker");
        public static final Type<UpdateSeekerPayload> TYPE = new Type<>(ID);
        public static final StreamCodec<ByteBuf, UpdateSeekerPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, UpdateSeekerPayload::pos,
                ByteBufCodecs.DOUBLE, UpdateSeekerPayload::x,
                ByteBufCodecs.DOUBLE, UpdateSeekerPayload::y,
                ByteBufCodecs.DOUBLE, UpdateSeekerPayload::z,
                UpdateSeekerPayload::new
        );
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // Client-to-server: toggles between 3D (6-sided) and 2D (XZ-plane, 4-sided) calculation modes
    public record ToggleModePayload(BlockPos pos, boolean use3D) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "toggle_seeker_mode");
        public static final Type<ToggleModePayload> TYPE = new Type<>(ID);
        public static final StreamCodec<ByteBuf, ToggleModePayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ToggleModePayload::pos,
                ByteBufCodecs.BOOL, ToggleModePayload::use3D,
                ToggleModePayload::new
        );
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
