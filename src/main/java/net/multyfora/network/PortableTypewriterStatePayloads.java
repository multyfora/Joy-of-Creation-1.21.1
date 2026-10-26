package net.multyfora.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.multyfora.AeronauticsJoyofcreation;

public class PortableTypewriterStatePayloads {

    public record SetTyping(boolean typing) implements CustomPacketPayload {
        public static final Type<SetTyping> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "pt_set_typing"));
        public static final StreamCodec<ByteBuf, SetTyping> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetTyping::typing,
            SetTyping::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TypingState(int entityId, boolean typing) implements CustomPacketPayload {
        public static final Type<TypingState> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "pt_typing_state"));
        public static final StreamCodec<ByteBuf, TypingState> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TypingState::entityId,
            ByteBufCodecs.BOOL, TypingState::typing,
            TypingState::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
