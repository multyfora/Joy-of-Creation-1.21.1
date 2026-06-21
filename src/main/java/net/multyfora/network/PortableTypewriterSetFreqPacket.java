package net.multyfora.network;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;

public class PortableTypewriterSetFreqPacket implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "pt_set_freq");
    public static final Type<PortableTypewriterSetFreqPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, PortableTypewriterSetFreqPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.glfwKeyCode,
            ByteBufCodecs.COMPOUND_TAG, p -> p.firstItem,
            ByteBufCodecs.COMPOUND_TAG, p -> p.secondItem,
            PortableTypewriterSetFreqPacket::new
    );

    private final int glfwKeyCode;
    private final CompoundTag firstItem;
    private final CompoundTag secondItem;

    public PortableTypewriterSetFreqPacket(int glfwKeyCode, CompoundTag firstItem, CompoundTag secondItem) {
        this.glfwKeyCode = glfwKeyCode;
        this.firstItem = firstItem;
        this.secondItem = secondItem;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer sp) || sp.isSpectator()) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof PortableTypewriterItem)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof PortableTypewriterItem)) return;
        }

        var registries = player.level().registryAccess();
        ItemStack first = ItemStack.parseOptional(registries, firstItem);
        ItemStack second = ItemStack.parseOptional(registries, secondItem);
        if (first.isEmpty() || second.isEmpty()) {
            PortableTypewriterItem.clearKeyBinding(heldItem, glfwKeyCode);
            return;
        }

        Couple<Frequency> freq = Couple.create(Frequency.of(first), Frequency.of(second));
        PortableTypewriterItem.setKeyBinding(heldItem, glfwKeyCode, freq, registries);
    }
}
