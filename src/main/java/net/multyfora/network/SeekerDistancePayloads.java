package net.multyfora.network;

import net.minecraft.core.BlockPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.multyfora.AeronauticsJoyofcreation;

public final class SeekerDistancePayloads {
    private SeekerDistancePayloads() {}

    public record OpenSeekerDistancePayload(BlockPos pos) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "open_seeker_distance");
        public static final Type<OpenSeekerDistancePayload> TYPE = new Type<>(ID);
        public static final StreamCodec<ByteBuf, OpenSeekerDistancePayload> CODEC =
                BlockPos.STREAM_CODEC.map(OpenSeekerDistancePayload::new, OpenSeekerDistancePayload::pos);
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UpdateSeekerDistancePayload(BlockPos pos, double x, double y, double z, int minDistance, int maxDistance) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "update_seeker_distance");
        public static final Type<UpdateSeekerDistancePayload> TYPE = new Type<>(ID);
        public static final StreamCodec<ByteBuf, UpdateSeekerDistancePayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, UpdateSeekerDistancePayload::pos,
                ByteBufCodecs.DOUBLE, UpdateSeekerDistancePayload::x,
                ByteBufCodecs.DOUBLE, UpdateSeekerDistancePayload::y,
                ByteBufCodecs.DOUBLE, UpdateSeekerDistancePayload::z,
                ByteBufCodecs.INT, UpdateSeekerDistancePayload::minDistance,
                ByteBufCodecs.INT, UpdateSeekerDistancePayload::maxDistance,
                UpdateSeekerDistancePayload::new
        );
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
