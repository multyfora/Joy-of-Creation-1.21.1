package net.multyfora.network;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Couple;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.content.portable_throttle.PortableThrottleServerHandler;
import net.multyfora.index.JocItems;

import static net.multyfora.network.NetworkUtils.getItemIfValid;

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

    /**
     * Server-side handling: validates the player has a throttle item, reads its frequency,
     * and forwards the signal to the server handler
     **/
    public void handle(net.minecraft.world.entity.player.Player player) {
        ItemStack item = getItemIfValid(
            player,
            JocItems.PORTABLE_THROTTLE.asItem()
        );
        if(item == null) {
            return;
        }

        Couple<Frequency> freq = PortableThrottleItem.getFrequency(item, player.level().registryAccess());
        if(freq == null) {
            return;
        }

        PortableThrottleServerHandler.receiveSignal(
            player.level(), player.blockPosition(), player.getUUID(),
            freq, strength
        );
    }
}
