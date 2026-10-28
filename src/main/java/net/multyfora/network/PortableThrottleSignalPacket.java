package net.multyfora.network;

import net.multyfora.config.JocFeatures;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.content.portable_throttle.PortableThrottleServerHandler;

public class PortableThrottleSignalPacket implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "throttle_signal");
    public static final Type<PortableThrottleSignalPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, PortableThrottleSignalPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, p -> p.mainHand,
        ByteBufCodecs.VAR_INT, p -> p.strength,
        PortableThrottleSignalPacket::new
    );

    private final boolean mainHand;
    private final int strength;

    public PortableThrottleSignalPacket(InteractionHand hand, int strength) {
        this(hand == InteractionHand.MAIN_HAND, strength);
    }

    private PortableThrottleSignalPacket(boolean mainHand, int strength) {
        this.mainHand = mainHand;
        this.strength = strength;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) {
            return;
        }

        ItemStack item = player.getItemInHand(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!(item.getItem() instanceof PortableThrottleItem) || !JocFeatures.isEnabled(item)) {
            return;
        }

        PortableThrottleItem.setStrength(item, strength);
        PortableThrottleServerHandler.updatePlayer(serverPlayer);
    }
}
