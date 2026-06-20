package net.multyfora.network;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;

// Client-to-server packet: binds a selected keyboard key to a Redstone Link block's frequency.
// Sent when the player right-clicks a Redstone Link while having a key selected in the typewriter GUI.
public class PortableTypewriterBindPacket implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "pt_bind");
    public static final Type<PortableTypewriterBindPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, PortableTypewriterBindPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.glfwKeyCode,
            BlockPos.STREAM_CODEC, p -> p.linkPos,
            PortableTypewriterBindPacket::new
    );

    // The GLFW key code to bind, and the position of the Redstone Link to bind to
    private final int glfwKeyCode;
    private final BlockPos linkPos;

    public PortableTypewriterBindPacket(int glfwKeyCode, BlockPos linkPos) {
        this.glfwKeyCode = glfwKeyCode;
        this.linkPos = linkPos;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Server-side: validates the player, finds the typewriter, reads the Redstone Link's
    // frequency from its LinkBehaviour, and writes the binding on the typewriter item
    public void handle(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer sp) || sp.isSpectator() || !player.mayBuild()) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof PortableTypewriterItem)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof PortableTypewriterItem))
                return;
        }

        LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(player.level(), linkPos, LinkBehaviour.TYPE);
        if (linkBehaviour == null) return;

        Couple<Frequency> frequency = linkBehaviour.getNetworkKey();
        PortableTypewriterItem.setKeyBinding(heldItem, glfwKeyCode, frequency, player.level().registryAccess());
    }
}
