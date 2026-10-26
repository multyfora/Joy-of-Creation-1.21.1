package net.multyfora.content.portable_typewriter;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.multyfora.network.PortableTypewriterStatePayloads.TypingState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PortableTypewriterTypists {
    private static final Set<UUID> TYPISTS = new HashSet<>();

    private PortableTypewriterTypists() {}

    public static void setTyping(ServerPlayer player, boolean typing) {
        typing &= isHoldingTypewriter(player);
        boolean changed = typing ? TYPISTS.add(player.getUUID()) : TYPISTS.remove(player.getUUID());
        if (changed) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new TypingState(player.getId(), typing));
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && TYPISTS.contains(player.getUUID())
                && !isHoldingTypewriter(player)) {
            setTyping(player, false);
        }
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (target instanceof Player typist && TYPISTS.contains(typist.getUUID())
                && event.getEntity() instanceof ServerPlayer tracker) {
            PacketDistributor.sendToPlayer(tracker, new TypingState(typist.getId(), true));
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TYPISTS.remove(event.getEntity().getUUID());
    }

    private static boolean isHoldingTypewriter(Player player) {
        return player.getMainHandItem().getItem() instanceof PortableTypewriterItem
            || player.getOffhandItem().getItem() instanceof PortableTypewriterItem;
    }
}
