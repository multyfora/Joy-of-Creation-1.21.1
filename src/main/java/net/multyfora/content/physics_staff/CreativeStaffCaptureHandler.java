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

import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import dev.simulated_team.simulated.index.SimSoundEvents;
import net.multyfora.mixin.physics_staff.PhysicsStaffServerHandlerMixin;

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
        if (isDraggingContraption(player)) {
            stopContraptionDrag(player);
            return;
        }

        Entity target = event.getTarget();
        if (target instanceof Player && !JocConfig.CAN_PICKUP_PLAYERS.get()) return;
        if (isGrabbed(target.getId())) return;

        double distance = holdDistanceTo(player, target);
        if (distance > 64.0) return;

        releaseSession(player);

        if (!(target.level() instanceof ServerLevel level)) return;
        double holdDist = distance;
        EntityGrabSession session = new EntityGrabSession(target, holdDist, level);
        GRAB_SESSIONS.put(player.getUUID(), session);
        event.setCanceled(true);

        SimSoundEvents.STAFF_IGNITE.playFrom(player, 1.0F, 1.0F);

        spawnGrabParticles(level, target);

        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(
                    sp,
                    new EntityGrabPayloads.Start(target.getId(), session.holdDistance)
            );
        }
    }

    public static void onEntityGrabRequestC2S(EntityGrabPayloads.GrabRequest payload, ServerPlayer player) {
        if (!JocConfig.ENABLE_CREATIVE_STAFF.get()) return;
        if (!isHoldingStaff(player)) return;
        if (isDraggingContraption(player)) {
            stopContraptionDrag(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        Entity target = level.getEntity(payload.entityId());
        if (target == null || !target.isAlive()) return;
        if (target instanceof Player && !JocConfig.CAN_PICKUP_PLAYERS.get()) return;
        if (isGrabbed(target.getId())) return;

        double distance = holdDistanceTo(player, target);
        if (distance > 64.0) return;

        releaseSession(player);

        double holdDist = distance;
        EntityGrabSession session = new EntityGrabSession(target, holdDist, level);
        GRAB_SESSIONS.put(player.getUUID(), session);

        SimSoundEvents.STAFF_IGNITE.playFrom(player, 1.0F, 1.0F);

        spawnGrabParticles(level, target);

        PacketDistributor.sendToPlayer(player, new EntityGrabPayloads.Start(target.getId(), holdDist));
    }

    public static void onEntityGrabStopC2S(EntityGrabPayloads.Stop payload, ServerPlayer player) {
        if (!JocConfig.ENABLE_CREATIVE_STAFF.get()) return;
        releaseSession(player);
    }

    public static void onSetHoldDistance(EntityGrabPayloads.SetHoldDistance payload, ServerPlayer player) {
        EntityGrabSession session = GRAB_SESSIONS.get(player.getUUID());
        if (session != null && session.grabbedEntityId == payload.entityId()) {
            session.holdDistance = Math.clamp(payload.distance(), 0.5, 64.0);
        }
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
        EntityGrabSession session = GRAB_SESSIONS.remove(event.getEntity().getUUID());
        if (session != null) cleanup(session);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (GRAB_SESSIONS.isEmpty()) return;

        Iterator<Map.Entry<UUID, EntityGrabSession>> it = GRAB_SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, EntityGrabSession> entry = it.next();
            UUID playerId = entry.getKey();
            EntityGrabSession session = entry.getValue();

            Player player = session.level.getPlayerByUUID(playerId);
            if (player == null || !player.isAlive() || !isHoldingStaff(player) || !JocConfig.ENABLE_CREATIVE_STAFF.get()) {
                cleanup(session);
                it.remove();
                if (player instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new EntityGrabPayloads.End());
                }
                continue;
            }

            Entity target = session.level.getEntity(session.grabbedEntityId);
            if (target == null || !target.isAlive()) {
                cleanup(session);
                it.remove();
                if (player instanceof ServerPlayer sp2) {
                    PacketDistributor.sendToPlayer(sp2, new EntityGrabPayloads.End());
                }
                continue;
            }

            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 lookVec = player.getLookAngle();
            Vec3 targetPos = eyePos.add(lookVec.scale(session.holdDistance))
                    .subtract(0, target.getBbHeight() / 2.0, 0);

            target.fallDistance = 0;

            if (target instanceof ServerPlayer sp) {
                Vec3 currentPos = sp.position();
                session.lastDelta = currentPos.subtract(session.lastTickPos);
                session.lastTickPos = currentPos;
                sp.setNoGravity(true);
                PacketDistributor.sendToPlayer(sp, new EntityGrabPayloads.HeldPosition(targetPos.x, targetPos.y, targetPos.z));
                continue;
            }

            Vec3 currentPos = target.position();
            Vec3 delta = targetPos.subtract(currentPos);
            Vec3 newPos = currentPos.add(delta.scale(0.35));

            session.lastDelta = newPos.subtract(session.lastTickPos);
            session.lastTickPos = newPos;

            target.setPos(newPos.x, newPos.y, newPos.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            target.noPhysics = true;
        }
    }

    private static void releaseSession(Player player) {
        EntityGrabSession session = GRAB_SESSIONS.remove(player.getUUID());
        if (session != null) {
            cleanup(session);
            fling(session, player);
            if (player instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new EntityGrabPayloads.End());
            }
        }
    }

    private static void cleanup(EntityGrabSession session) {
        Entity target = session.level.getEntity(session.grabbedEntityId);
        if (target == null) return;

        if (target instanceof ServerPlayer sp) {
            sp.setNoGravity(session.wasNoGravity);
            PacketDistributor.sendToPlayer(sp, new EntityGrabPayloads.HeldRelease());
        } else {
            target.noPhysics = false;
        }
    }

    private static void fling(EntityGrabSession session, Player player) {
        Entity target = session.level.getEntity(session.grabbedEntityId);
        if (target == null) return;

        target.setDeltaMovement(session.lastDelta);
        target.hurtMarked = true;

        SimSoundEvents.STAFF_EXTINGUISH.playFrom(player, 1.0F, 1.0F);

        spawnGrabParticles(session.level, target);
    }

    private static void spawnGrabParticles(ServerLevel level, Entity target) {
        level.sendParticles(ParticleTypes.END_ROD,
                target.getX(), target.getY(), target.getZ(),
                10, 0.1, 0.1, 0.1, 0.0);
    }

    private static double holdDistanceTo(Player player, Entity target) {
        return player.getEyePosition(1.0F).distanceTo(target.getBoundingBox().getCenter());
    }

    private static boolean isGrabbed(int entityId) {
        for (EntityGrabSession session : GRAB_SESSIONS.values()) {
            if (session.grabbedEntityId == entityId) return true;
        }
        return false;
    }

    private static boolean isDraggingContraption(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        PhysicsStaffServerHandler handler = PhysicsStaffServerHandler.get(serverLevel);
        return ((PhysicsStaffServerHandlerMixin) handler).joc$getDraggingSessions().containsKey(player.getUUID());
    }

    private static void stopContraptionDrag(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        PhysicsStaffServerHandler.get(serverLevel).stopDragging(player.getUUID());
    }

    private static class EntityGrabSession {
        final int grabbedEntityId;
        double holdDistance;
        final ServerLevel level;
        final boolean wasNoGravity;
        Vec3 lastTickPos;
        Vec3 lastDelta;

        EntityGrabSession(Entity target, double holdDistance, ServerLevel level) {
            this.grabbedEntityId = target.getId();
            this.holdDistance = holdDistance;
            this.level = level;
            this.wasNoGravity = target.isNoGravity();
            this.lastTickPos = target.position();
            this.lastDelta = Vec3.ZERO;
        }
    }
}
