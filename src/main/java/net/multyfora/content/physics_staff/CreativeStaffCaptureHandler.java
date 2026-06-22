package net.multyfora.content.physics_staff;

import net.multyfora.config.JocConfig;
import net.multyfora.network.EntityGrabPayloads;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import dev.simulated_team.simulated.index.SimSoundEvents;

import java.util.*;

public class CreativeStaffCaptureHandler {
    private static final Map<UUID, EntityGrabSession> GRAB_SESSIONS = new HashMap<>();

    public static boolean isHoldingStaff(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.getItem() instanceof dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem;
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!JocConfig.ENABLE_CREATIVE_STAFF.get()) return;

        Player player = event.getEntity();
        if (!isHoldingStaff(player)) return;

        Entity target = event.getTarget();
        if (target instanceof Player && !JocConfig.CAN_PICKUP_PLAYERS.get()) return;
        if (isGrabbed(target.getId())) return;

        double distance = player.distanceTo(target);
        if (distance > JocConfig.STAFF_GRAB_RANGE.get()) return;

        releaseSession(player);

        ServerLevel level = (ServerLevel) target.level();
        double holdDist = Math.min(distance, JocConfig.STAFF_HOLD_DISTANCE.get());
        EntityGrabSession session = new EntityGrabSession(target.getId(), holdDist, level, target.position());
        GRAB_SESSIONS.put(player.getUUID(), session);
        event.setCanceled(true);

        // Sound
        SimSoundEvents.STAFF_IGNITE.playFrom(player, 1.0F, 1.0F);

        // END_ROD particles at entity position
        var random = level.getRandom();
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.END_ROD,
                    target.getX() + (random.nextDouble() * 0.2 - 0.1),
                    target.getY() + (random.nextDouble() * 0.2 - 0.1),
                    target.getZ() + (random.nextDouble() * 0.2 - 0.1),
                    0, 0, 0);
        }

        // Send start payload to client
        PacketDistributor.sendToPlayer(
                (ServerPlayer) player,
                new EntityGrabPayloads.Start(target.getId())
        );
    }

    public static void onEntityGrabRequestC2S(EntityGrabPayloads.GrabRequest payload, ServerPlayer player) {
        if (!JocConfig.ENABLE_CREATIVE_STAFF.get()) return;
        if (!isHoldingStaff(player)) return;

        ServerLevel level = player.serverLevel();
        Entity target = level.getEntity(payload.entityId());
        if (target == null || !target.isAlive()) return;
        if (target instanceof Player && !JocConfig.CAN_PICKUP_PLAYERS.get()) return;
        if (isGrabbed(target.getId())) return;

        double distance = player.distanceTo(target);
        if (distance > JocConfig.STAFF_GRAB_RANGE.get()) return;

        releaseSession(player);

        double holdDist = Math.min(distance, JocConfig.STAFF_HOLD_DISTANCE.get());
        EntityGrabSession session = new EntityGrabSession(target.getId(), holdDist, level, target.position());
        GRAB_SESSIONS.put(player.getUUID(), session);

        SimSoundEvents.STAFF_IGNITE.playFrom(player, 1.0F, 1.0F);

        var random = level.getRandom();
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.END_ROD,
                    target.getX() + (random.nextDouble() * 0.2 - 0.1),
                    target.getY() + (random.nextDouble() * 0.2 - 0.1),
                    target.getZ() + (random.nextDouble() * 0.2 - 0.1),
                    0, 0, 0);
        }

        PacketDistributor.sendToPlayer(player, new EntityGrabPayloads.Start(target.getId()));
    }

    public static void onEntityGrabStopC2S(EntityGrabPayloads.Stop payload, ServerPlayer player) {
        if (!JocConfig.ENABLE_CREATIVE_STAFF.get()) return;
        releaseSession(player);
    }

    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getLevel().isClientSide()) return;
        if (!JocConfig.ENABLE_CREATIVE_STAFF.get()) return;

        releaseSession(event.getEntity());
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!JocConfig.ENABLE_CREATIVE_STAFF.get()) return;

        Player player = event.getEntity();
        if (GRAB_SESSIONS.containsKey(player.getUUID())) {
            releaseSession(player);
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GRAB_SESSIONS.remove(event.getEntity().getUUID());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (GRAB_SESSIONS.isEmpty()) return;

        Iterator<Map.Entry<UUID, EntityGrabSession>> it = GRAB_SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, EntityGrabSession> entry = it.next();
            UUID playerId = entry.getKey();
            EntityGrabSession session = entry.getValue();

            Player player = session.level.getPlayerByUUID(playerId);
            if (player == null || !player.isAlive() || !isHoldingStaff(player)) {
                cleanup(session);
                it.remove();
                continue;
            }

            Entity target = session.level.getEntity(session.grabbedEntityId);
            if (target == null || !target.isAlive()) {
                it.remove();
                continue;
            }

            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 lookVec = player.getLookAngle();
            Vec3 targetPos = eyePos.add(lookVec.scale(session.holdDistance));

            Vec3 currentPos = target.position();
            Vec3 delta = targetPos.subtract(currentPos);
            Vec3 newPos = currentPos.add(delta.scale(0.35));

            session.lastDelta = newPos.subtract(session.lastTickPos);
            session.lastTickPos = newPos;

            target.setPos(newPos.x, newPos.y, newPos.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0;
            target.hurtMarked = true;
            target.noPhysics = true;
        }
    }

    private static void releaseSession(Player player) {
        EntityGrabSession session = GRAB_SESSIONS.remove(player.getUUID());
        if (session != null) {
            cleanup(session);
            fling(session, player);
        }
    }

    private static void cleanup(EntityGrabSession session) {
        Entity target = session.level.getEntity(session.grabbedEntityId);
        if (target != null) {
            target.noPhysics = false;
        }
    }

    private static void fling(EntityGrabSession session, Player player) {
        Entity target = session.level.getEntity(session.grabbedEntityId);
        if (target == null) return;

        target.setDeltaMovement(session.lastDelta);

        // Sound
        SimSoundEvents.STAFF_EXTINGUISH.playFrom(player, 1.0F, 1.0F);

        // Particles at entity position
        var random = session.level.getRandom();
        for (int i = 0; i < 10; i++) {
            session.level.addParticle(ParticleTypes.END_ROD,
                    target.getX() + (random.nextDouble() * 0.2 - 0.1),
                    target.getY() + (random.nextDouble() * 0.2 - 0.1),
                    target.getZ() + (random.nextDouble() * 0.2 - 0.1),
                    0, 0, 0);
        }
    }

    private static boolean isGrabbed(int entityId) {
        for (EntityGrabSession session : GRAB_SESSIONS.values()) {
            if (session.grabbedEntityId == entityId) return true;
        }
        return false;
    }

    private static class EntityGrabSession {
        final int grabbedEntityId;
        double holdDistance;
        final ServerLevel level;
        Vec3 lastTickPos;
        Vec3 lastDelta;

        EntityGrabSession(int grabbedEntityId, double holdDistance, ServerLevel level, Vec3 startPos) {
            this.grabbedEntityId = grabbedEntityId;
            this.holdDistance = holdDistance;
            this.level = level;
            this.lastTickPos = startPos;
            this.lastDelta = Vec3.ZERO;
        }
    }
}
