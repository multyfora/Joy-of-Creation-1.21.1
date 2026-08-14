package net.multyfora.compat.createthrusters.mixin;

import com.rieno.gadgetsandgizmos.compat.simulated.SimulatedRopeCompat;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import dev.simulated_team.simulated.service.SimConfigService;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.IMultiRopeBehavior;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Replaces SimulatedRopeCompat.createRopeWithAttachments (used by the EntityLauncher
 * anchoring path) when either endpoint is a JOC multi-rope holder. The base
 * implementation rejects any already-attached target, destroys the start side's
 * existing ropes, and writes holder state through reflection, bypassing JOC's
 * extended bookkeeping. This JOC-aware version allows JOC holders with remaining
 * capacity, preserves their existing strands, and registers the new strand through
 * IMultiRopeBehavior so destruction/tracking stay consistent. All other cases pass
 * through to the untouched original implementation.
 **/
@Mixin(value = SimulatedRopeCompat.class, remap = false)
public abstract class SimulatedRopeCompatMixin {

    @Inject(
            method = "createRope",
            at = @At("HEAD"),
            remap = false
    )
    private static void joc$logCreateRope(RopeStrandHolderBehavior a, RopeStrandHolderBehavior b, boolean dropItem, CallbackInfoReturnable<Boolean> cir) {
        AeronauticsJoyofcreation.LOGGER.info("[JOC-COMPAT] createRope a={} b={} drop={}",
                a != null ? a.blockEntity.getBlockPos() : "null",
                b != null ? b.blockEntity.getBlockPos() : "null",
                dropItem);
    }

    @Inject(
            method = "createRopeWithAttachments",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void joc$createRopeWithAttachments(
            RopeStrandHolderBehavior holderA,
            RopeStrandHolderBehavior holderB,
            ServerLevel level,
            Vec3 vecA,
            Vec3 vecB,
            UUID uuidA,
            BlockPos posA,
            UUID uuidB,
            BlockPos posB,
            boolean dropItem,
            CallbackInfoReturnable<Boolean> cir
    ) {
        IMultiRopeBehavior multiA = holderA instanceof IMultiRopeBehavior ma ? ma : null;
        IMultiRopeBehavior multiB = holderB instanceof IMultiRopeBehavior mb ? mb : null;
        boolean jocManaged = (multiA != null && multiA.joc$getMaxRopeAttachments() > 1)
                || (multiB != null && multiB.joc$getMaxRopeAttachments() > 1);
        if (!jocManaged) {
            return;
        }

        cir.setReturnValue(joc$createJocAware(
                holderA, holderB, level, vecA, vecB, uuidA, posA, uuidB, posB, dropItem,
                multiA, multiB
        ));
    }

    private static boolean joc$createJocAware(
            RopeStrandHolderBehavior holderA,
            RopeStrandHolderBehavior holderB,
            ServerLevel level,
            Vec3 vecA,
            Vec3 vecB,
            UUID uuidA,
            BlockPos posA,
            UUID uuidB,
            BlockPos posB,
            boolean dropItem,
            @Nullable IMultiRopeBehavior multiA,
            @Nullable IMultiRopeBehavior multiB
    ) {
        if (holderA == null || holderB == null || holderA == holderB
                || level == null || vecA == null || vecB == null
                || uuidA == null || posB == null) {
            return false;
        }
        if (multiB == null && holderB.isAttached()) {
            return false;
        }

        double distance = vecA.distanceTo(vecB);
        if (!Double.isFinite(distance)) return false;
        double maxRange = (Double) SimConfigService.INSTANCE.server().blocks.maxRopeRange.get();
        if (distance > maxRange) return false;

        // The base implementation destroys the start side's ropes first; JOC multi
        // holders keep their existing strands and add alongside them instead.
        if (multiA == null && holderA.isAttached()) {
            SimulatedRopeCompat.destroyRope(holderA, null, vecA, dropItem);
        }

        if (multiA != null ? !multiA.joc$canAcceptAnotherRope() : holderA.isAttached()) return false;
        if (multiB != null ? !multiB.joc$canAcceptAnotherRope() : holderB.isAttached()) return false;
        if (joc$isAlreadyConnected(holderA, holderB, level)) return false;

        ServerRopeStrand strand = new ServerRopeStrand(UUID.randomUUID(), joc$buildInitialPoints(vecA, vecB));
        strand.updateFirstSegmentExtension(distance - Mth.floor(distance));
        strand.addAttachment(level, RopeAttachmentPoint.START,
                new RopeAttachment(RopeAttachmentPoint.START, uuidA, posA.immutable()));
        strand.addAttachment(level, RopeAttachmentPoint.END,
                new RopeAttachment(RopeAttachmentPoint.END, uuidB, posB.immutable()));

        joc$installState(holderA, strand.getUUID(), true, strand);
        joc$installState(holderB, strand.getUUID(), false, null);

        ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
        if (manager == null) {
            joc$removeState(holderA, strand.getUUID(), true, strand);
            joc$removeState(holderB, strand.getUUID(), false, null);
            return false;
        }
        manager.addStrand(strand);
        holderA.blockEntity.notifyUpdate();
        holderB.blockEntity.notifyUpdate();
        return true;
    }

    private static void joc$installState(RopeStrandHolderBehavior holder, UUID ropeID, boolean owner, @Nullable ServerRopeStrand strand) {
        ((IMultiRopeBehavior) holder).joc$installExternalRope(ropeID, owner, strand);
    }

    private static void joc$removeState(RopeStrandHolderBehavior holder, UUID ropeID, boolean owner, @Nullable ServerRopeStrand strand) {
        ((IMultiRopeBehavior) holder).joc$removeExternalRope(ropeID, owner, strand);
    }

    private static boolean joc$isAlreadyConnected(RopeStrandHolderBehavior holderA, RopeStrandHolderBehavior holderB, ServerLevel level) {
        BlockPos aPos = holderA.blockEntity.getBlockPos();
        BlockPos bPos = holderB.blockEntity.getBlockPos();
        ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);

        for (UUID id : ((IMultiRopeBehavior) holderA).joc$getAllAttachedRopeIDs()) {
            ServerRopeStrand strand = manager.getStrand(id);
            if (strand == null) continue;
            RopeAttachment end = strand.getAttachment(RopeAttachmentPoint.END);
            if (end != null && bPos.equals(end.blockAttachment())) {
                return true;
            }
        }
        if (holderB instanceof IMultiRopeBehavior multiB) {
            for (UUID id : multiB.joc$getAllAttachedRopeIDs()) {
                ServerRopeStrand strand = manager.getStrand(id);
                if (strand == null) continue;
                RopeAttachment end = strand.getAttachment(RopeAttachmentPoint.END);
                if (end != null && aPos.equals(end.blockAttachment())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Vector3d> joc$buildInitialPoints(Vec3 vecA, Vec3 vecB) {
        double distance = vecA.distanceTo(vecB);
        int floorDistance = Mth.floor(distance);
        int count = Math.max(1, floorDistance + 1);
        Vec3 direction = distance > 1.0E-6 ? vecB.subtract(vecA).normalize() : Vec3.ZERO;
        double remainder = distance - floorDistance;

        List<Vector3d> points = new ArrayList<>(count + 1);
        points.add(joc$toVector3d(vecA));
        for (int i = 0; i < count; i++) {
            points.add(joc$toVector3d(vecA.add(direction.scale(i + remainder))));
        }
        return points;
    }

    private static Vector3d joc$toVector3d(Vec3 vec) {
        return new Vector3d(vec.x, vec.y, vec.z);
    }
}