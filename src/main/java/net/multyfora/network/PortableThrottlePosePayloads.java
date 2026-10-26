package net.multyfora.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.multyfora.AeronauticsJoyofcreation;

public class PortableThrottlePosePayloads {

    public record SetPose(boolean gripping, int strength) implements CustomPacketPayload {
        public static final Type<SetPose> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "throttle_set_pose"));
        public static final StreamCodec<ByteBuf, SetPose> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetPose::gripping,
            ByteBufCodecs.VAR_INT, SetPose::strength,
            SetPose::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PoseState(int entityId, boolean gripping, int strength) implements CustomPacketPayload {
        public static final Type<PoseState> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "throttle_pose_state"));
        public static final StreamCodec<ByteBuf, PoseState> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PoseState::entityId,
            ByteBufCodecs.BOOL, PoseState::gripping,
            ByteBufCodecs.VAR_INT, PoseState::strength,
            PoseState::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
