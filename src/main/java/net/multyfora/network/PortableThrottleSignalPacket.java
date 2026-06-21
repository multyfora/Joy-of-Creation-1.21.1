package net.multyfora.network;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Couple;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.content.portable_throttle.PortableThrottleServerHandler;

import org.slf4j.Logger;

/**
 * Client-to-server packet: sends the current throttle signal strength to the server.
 * The server then updates the redstone link network with the new power level.
 **/
public class PortableThrottleSignalPacket implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "throttle_signal");
    public static final Type<PortableThrottleSignalPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, PortableThrottleSignalPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.strength,
            PortableThrottleSignalPacket::new
    );

    private final int strength;

    public PortableThrottleSignalPacket(int strength) {
        this.strength = strength;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final Logger LOGGER = AeronauticsJoyofcreation.LOGGER;

    /**
     * Server-side handling: validates the player has a throttle item, reads its frequency,
     * and forwards the signal to the server handler
     **/
    public void handle(net.minecraft.world.entity.player.Player player) {
        LOGGER.info("[THROTTLE_PACKET] SignalPacket.handle ENTER: player={} uuid={} strength={}", player.getName().getString(), player.getUUID(), strength);

        if (!(player instanceof ServerPlayer sp) || sp.isSpectator()) {
            LOGGER.info("[THROTTLE_PACKET] SignalPacket.handle: player is spectator or not ServerPlayer, skipping");
            return;
        }

        // Find the throttle item in mainhand or offhand
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof PortableThrottleItem)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof PortableThrottleItem)) {
                LOGGER.info("[THROTTLE_PACKET] SignalPacket.handle: throttle not found in main or offhand, skipping");
                return;
            }
            LOGGER.info("[THROTTLE_PACKET] SignalPacket.handle: throttle found in offhand");
        } else {
            LOGGER.info("[THROTTLE_PACKET] SignalPacket.handle: throttle found in main hand");
        }

        Couple<Frequency> freq = PortableThrottleItem.getFrequency(heldItem, player.level().registryAccess());
        if (freq == null) {
            LOGGER.info("[THROTTLE_PACKET] SignalPacket.handle: frequency is null, cannot send signal");
            return;
        }
        LOGGER.info("[THROTTLE_PACKET] SignalPacket.handle: freq=({}|{}) transmitting strength={}",
            freq.getFirst().getStack().getHoverName().getString(),
            freq.getSecond().getStack().getHoverName().getString(),
            strength);

        PortableThrottleServerHandler.receiveSignal(
                player.level(), player.blockPosition(), player.getUUID(), freq, strength);
        LOGGER.info("[THROTTLE_PACKET] SignalPacket.handle EXIT");
    }
}
