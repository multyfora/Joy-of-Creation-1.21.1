package net.multyfora.network;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Couple;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;
import net.multyfora.content.portable_typewriter.PortableTypewriterServerHandler;
import net.multyfora.index.JocItems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.multyfora.network.NetworkUtils.getItemIfValid;

public class PortableTypewriterInputPacket implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "pt_input");
    public static final Type<PortableTypewriterInputPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, PortableTypewriterInputPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT), p -> p.keyCodes,
        ByteBufCodecs.BOOL, p -> p.pressed,
        PortableTypewriterInputPacket::new
    );

    private final List<Integer> keyCodes;
    private final boolean pressed;

    public PortableTypewriterInputPacket(Collection<Integer> keyCodes, boolean pressed) {
        this.keyCodes = List.copyOf(keyCodes);
        this.pressed = pressed;
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
        List<Couple<Frequency>> frequencies = new ArrayList<>();
        for(int keyCode : keyCodes) {
            Couple<Frequency> freq = PortableTypewriterItem.getKeyBinding(item, keyCode, registries);
            if(freq != null) {
                frequencies.add(freq);
            }
        }

        if( frequencies.isEmpty() ) {
            return;
        }
        PortableTypewriterServerHandler.receivePressed(
            player.level(), player.blockPosition(), player.getUUID(),
            frequencies, pressed
        );
    }
}
