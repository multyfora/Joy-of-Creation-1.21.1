package net.multyfora.network;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import org.slf4j.Logger;

// Client-to-server packet: binds the Portable Throttle to a Redstone Link block's frequency.
// Sent when the player right-clicks a Redstone Link with the throttle item.
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

    private static final Logger LOGGER = AeronauticsJoyofcreation.LOGGER;

    // Server-side: verifies the player can build, finds the throttle item, reads the
    // Redstone Link's frequency from its LinkBehaviour, and writes it to the throttle item
    public void handle(net.minecraft.world.entity.player.Player player) {
        LOGGER.info("[THROTTLE_PACKET] BindPacket.handle ENTER: player={} linkPos={}", player.getName().getString(), linkPos);

        if (!(player instanceof ServerPlayer sp) || sp.isSpectator() || !player.mayBuild()) {
            LOGGER.info("[THROTTLE_PACKET] BindPacket.handle: cannot build or spectator, skipping");
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof PortableThrottleItem)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof PortableThrottleItem)) {
                LOGGER.info("[THROTTLE_PACKET] BindPacket.handle: throttle not found, skipping");
                return;
            }
        }

        LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(player.level(), linkPos, LinkBehaviour.TYPE);
        if (linkBehaviour == null) {
            LOGGER.info("[THROTTLE_PACKET] BindPacket.handle: no LinkBehaviour at {}", linkPos);
            return;
        }

        Couple<Frequency> frequency = linkBehaviour.getNetworkKey();
        LOGGER.info("[THROTTLE_PACKET] BindPacket.handle: binding freq=({}|{})",
            frequency.getFirst().getStack().getHoverName().getString(),
            frequency.getSecond().getStack().getHoverName().getString());
        PortableThrottleItem.setFrequency(heldItem, frequency, player.level().registryAccess());
        LOGGER.info("[THROTTLE_PACKET] BindPacket.handle EXIT");
    }
}
