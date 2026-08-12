package net.multyfora.content.shears_cut;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.multyfora.network.ShearsCutPayloads.ShearsPreviewPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;

/** Relays a player's cut preview to everyone within render distance (excluding the cutter). */
public final class ShearsPreviewServerHandler {

    private ShearsPreviewServerHandler() {}

    public static void handle(ShearsPreviewPayload payload, ServerPlayer sender) {
        broadcast(sender.serverLevel(), payload, sender);
    }

    public static void broadcast(ServerLevel level, ShearsPreviewPayload payload, ServerPlayer sender) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;
        SubLevel containing = container.getSubLevel(payload.subLevelId());
        if (!(containing instanceof ServerSubLevel subLevel) || subLevel.isRemoved()) return;

        Vec3 worldPoint1 = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(payload.point1()));
        double range = level.getServer().getPlayerList().getViewDistance() * 16.0;
        double rangeSqr = range * range;

        for (ServerPlayer player : level.players()) {
            if (player == sender) continue;
            if (player.position().distanceToSqr(worldPoint1) <= rangeSqr) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }
}
