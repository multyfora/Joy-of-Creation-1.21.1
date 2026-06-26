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
import net.multyfora.content.portable_throttle.PortableThrottleItem;

/**
 * Client-to-server packet: sends updated frequency configuration for the Portable Throttle.
 * Carries two ItemStack NBT compounds representing the two items that define the frequency pair.
 **/
public class PortableThrottleConfigPacket implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "throttle_config");
    public static final Type<PortableThrottleConfigPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, PortableThrottleConfigPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, p -> p.firstItem,
            ByteBufCodecs.COMPOUND_TAG, p -> p.secondItem,
            PortableThrottleConfigPacket::new
    );

    // NBT serialization of the two frequency-defining item stacks
    private final CompoundTag firstItem;
    private final CompoundTag secondItem;

    public PortableThrottleConfigPacket(CompoundTag firstItem, CompoundTag secondItem) {
        this.firstItem = firstItem;
        this.secondItem = secondItem;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Server-side: finds the throttle item in the player's hand, deserialises the two item stacks,
     * builds a frequency pair, and writes it to the item's NBT
     **/
    public void handle(net.minecraft.world.entity.player.Player player) {

        if (!(player instanceof ServerPlayer sp) || sp.isSpectator()) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof PortableThrottleItem)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof PortableThrottleItem)) {
                return;
            }
        }

        var registries = player.level().registryAccess();
        ItemStack first = ItemStack.parseOptional(registries, firstItem);
        ItemStack second = ItemStack.parseOptional(registries, secondItem);
        if (first.isEmpty() || second.isEmpty()) {
            return;
        }

        Couple<Frequency> freq = Couple.create(Frequency.of(first), Frequency.of(second));
        PortableThrottleItem.setFrequency(heldItem, freq, registries);
    }
}
