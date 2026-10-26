package net.multyfora.content.portable_throttle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.multyfora.network.PortableThrottlePosePayloads.PoseState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PortableThrottleHolders {
    private record Pose(boolean gripping, int strength) {}

    private static final Map<UUID, Pose> POSES = new HashMap<>();

    private PortableThrottleHolders() {}

    public static void setPose(ServerPlayer player, boolean gripping, int strength) {
        if (!isHoldingThrottle(player)) return;
        Pose pose = new Pose(gripping, Mth.clamp(strength, 0, 15));
        if (pose.equals(POSES.put(player.getUUID(), pose))) return;
        PacketDistributor.sendToPlayersTrackingEntity(player, new PoseState(player.getId(), pose.gripping(), pose.strength()));
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && POSES.containsKey(player.getUUID())
                && !isHoldingThrottle(player)) {
            POSES.remove(player.getUUID());
            PacketDistributor.sendToPlayersTrackingEntity(player, new PoseState(player.getId(), false, 0));
        }
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (target instanceof Player holder && event.getEntity() instanceof ServerPlayer tracker) {
            Pose pose = POSES.get(holder.getUUID());
            if (pose != null) {
                PacketDistributor.sendToPlayer(tracker, new PoseState(holder.getId(), pose.gripping(), pose.strength()));
            }
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        POSES.remove(event.getEntity().getUUID());
    }

    private static boolean isHoldingThrottle(Player player) {
        return player.getMainHandItem().getItem() instanceof PortableThrottleItem
            || player.getOffhandItem().getItem() instanceof PortableThrottleItem;
    }
}
