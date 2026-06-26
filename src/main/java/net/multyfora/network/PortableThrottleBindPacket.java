package net.multyfora.network;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.multyfora.index.JocItems;

import static net.multyfora.network.NetworkUtils.getItemIfValid;

/**
 * Client-to-server packet: binds the Portable Throttle to a Redstone Link block's frequency.
 * Sent when the player right-clicks a Redstone Link with the throttle item.
 **/
public class PortableThrottleBindPacket implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "throttle_bind");
    public static final Type<PortableThrottleBindPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, PortableThrottleBindPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, p -> p.linkPos,
        PortableThrottleBindPacket::new
    );

    // Position of the Redstone Link block being bound to
    private final BlockPos linkPos;

    public PortableThrottleBindPacket(BlockPos linkPos) {
        this.linkPos = linkPos;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Server-side: verifies the player can build, finds the throttle item, reads the
     * Redstone Link's frequency from its LinkBehaviour, and writes it to the throttle item
     **/
    public void handle(net.minecraft.world.entity.player.Player player) {
        ItemStack item = getItemIfValid(
            player,
            JocItems.PORTABLE_THROTTLE.asItem()
        );
        if(item == null) {
            return;
        }

        LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(player.level(), linkPos, LinkBehaviour.TYPE);
        if(linkBehaviour == null) {
            return;
        }

        Couple<Frequency> frequency = linkBehaviour.getNetworkKey();
        PortableThrottleItem.setFrequency(
            item, frequency,
            player.level().registryAccess()
        );
    }
}
