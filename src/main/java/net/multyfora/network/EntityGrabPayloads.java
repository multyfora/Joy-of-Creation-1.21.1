package net.multyfora.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.multyfora.AeronauticsJoyofcreation;

public class EntityGrabPayloads {
    public static final ResourceLocation START_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "entity_grab_start");
    public static final ResourceLocation STOP_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "entity_grab_stop");
    public static final ResourceLocation END_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "entity_grab_end");
    public static final ResourceLocation GRAB_REQUEST_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "grab_request");
    public static final ResourceLocation SET_HOLD_DISTANCE_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "set_hold_distance");
    public static final ResourceLocation HELD_POSITION_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "held_position");
    public static final ResourceLocation HELD_RELEASE_ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "held_release");

    public record Start(int entityId, double holdDistance) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(START_ID);
        public static final StreamCodec<ByteBuf, Start> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, Start::entityId,
            ByteBufCodecs.DOUBLE, Start::holdDistance,
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

    public record End() implements CustomPacketPayload {
        public static final Type<End> TYPE = new Type<>(END_ID);
        public static final StreamCodec<ByteBuf, End> CODEC = StreamCodec.unit(new End());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record GrabRequest(int entityId) implements CustomPacketPayload {
        public static final Type<GrabRequest> TYPE = new Type<>(GRAB_REQUEST_ID);
        public static final StreamCodec<ByteBuf, GrabRequest> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, GrabRequest::entityId,
            GrabRequest::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetHoldDistance(int entityId, double distance) implements CustomPacketPayload {
        public static final Type<SetHoldDistance> TYPE = new Type<>(SET_HOLD_DISTANCE_ID);
        public static final StreamCodec<ByteBuf, SetHoldDistance> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SetHoldDistance::entityId,
            ByteBufCodecs.DOUBLE, SetHoldDistance::distance,
            SetHoldDistance::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record HeldPosition(double x, double y, double z) implements CustomPacketPayload {
        public static final Type<HeldPosition> TYPE = new Type<>(HELD_POSITION_ID);
        public static final StreamCodec<ByteBuf, HeldPosition> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, HeldPosition::x,
            ByteBufCodecs.DOUBLE, HeldPosition::y,
            ByteBufCodecs.DOUBLE, HeldPosition::z,
            HeldPosition::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record HeldRelease() implements CustomPacketPayload {
        public static final Type<HeldRelease> TYPE = new Type<>(HELD_RELEASE_ID);
        public static final StreamCodec<ByteBuf, HeldRelease> CODEC = StreamCodec.unit(new HeldRelease());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
