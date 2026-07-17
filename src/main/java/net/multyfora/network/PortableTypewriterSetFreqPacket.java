package net.multyfora.network;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;
import net.multyfora.index.JocItems;

import static net.multyfora.network.NetworkUtils.getItemIfValid;

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

    public void handle(Player player) {
        ItemStack item = getItemIfValid(
            player,
            JocItems.PORTABLE_TYPEWRITER.asItem()
        );
        if(item == null) {
            return;
        }

        var registries = player.level().registryAccess();
        ItemStack first = ItemStack.parseOptional(registries, firstItem);
        ItemStack second = ItemStack.parseOptional(registries, secondItem);
        if( first.isEmpty() && second.isEmpty() ) {
            PortableTypewriterItem.clearKeyBinding(item, glfwKeyCode);
            return;
        }

        Couple<Frequency> freq = Couple.create(
            Frequency.of(first),
            Frequency.of(second)
        );
        PortableTypewriterItem.setKeyBinding(item, glfwKeyCode, freq, registries);
    }
}
