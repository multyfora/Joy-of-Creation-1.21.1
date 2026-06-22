package net.multyfora.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.multyfora.AeronauticsJoyofcreation;

public class EntityGrabPayloads {
    public static final ResourceLocation START_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "entity_grab_start");
    public static final ResourceLocation STOP_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "entity_grab_stop");
    public static final ResourceLocation GRAB_REQUEST_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "grab_request");

    public record Start(int entityId) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(START_ID);
        public static final StreamCodec<ByteBuf, Start> CODEC = StreamCodec.composite(
                net.minecraft.network.codec.ByteBufCodecs.INT, Start::entityId,
                Start::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Stop() implements CustomPacketPayload {
        public static final Type<Stop> TYPE = new Type<>(STOP_ID);
        public static final StreamCodec<ByteBuf, Stop> CODEC = StreamCodec.unit(new Stop());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record GrabRequest(int entityId) implements CustomPacketPayload {
        public static final Type<GrabRequest> TYPE = new Type<>(GRAB_REQUEST_ID);
        public static final StreamCodec<ByteBuf, GrabRequest> CODEC = StreamCodec.composite(
                net.minecraft.network.codec.ByteBufCodecs.INT, GrabRequest::entityId,
                GrabRequest::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
