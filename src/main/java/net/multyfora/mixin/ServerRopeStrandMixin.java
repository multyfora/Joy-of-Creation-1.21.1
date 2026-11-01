package net.multyfora.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

import net.multyfora.IChainRopeStrand;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = ServerRopeStrand.class, remap = false)
public abstract class ServerRopeStrandMixin implements IChainRopeStrand {

    @Unique private static final double joc$MIN_AIMABLE_DISTANCE = 1.0E-4;
    @Unique private static final double joc$TAUT_PATH_RATIO = 1.01;
    @Unique private static final double joc$TRIM_RATE = 0.02;
    @Unique private static final double joc$MIN_TRIMMABLE_SEGMENT = 0.5;
    @Unique private static final double joc$TRIM_STEP = 1.0E-3;

    @Shadow @Nullable private PhysicsConstraintHandle constraint;
    @Shadow private double lastFirstSegmentExtension;
    @Shadow public abstract RopeAttachment getAttachment(RopeAttachmentPoint point);
    @Shadow public abstract double getCurrentExtension();

    @Unique private boolean joc$chain;
    @Unique @Nullable private GenericConstraintHandle joc$lengthLimit;
    @Unique private double joc$lengthLimitValue = Double.POSITIVE_INFINITY;
    @Unique private double joc$firstSegmentTrim;
    @Unique private double joc$appliedFirstSegment = Double.NaN;
    @Unique private final Vector3d joc$startAnchor = new Vector3d();
    @Unique private final Vector3d joc$endAnchor = new Vector3d();

    @Override
    public boolean joc$isChain() {
        return joc$chain;
    }

    @Override
    public void joc$setChain(boolean chain) {
        this.joc$chain = chain;
    }

    @Inject(method = "removeConstraints", at = @At("HEAD"))
    private void joc$removeLengthLimit(CallbackInfo ci) {
        if (joc$lengthLimit != null) {
            joc$lengthLimit.remove();
            joc$lengthLimit = null;
        }
        joc$lengthLimitValue = Double.POSITIVE_INFINITY;
    }

    @Inject(method = "prePhysicsTick", at = @At("TAIL"))
    private void joc$enforceChainLength(SubLevelPhysicsSystem physicsSystem, ServerLevel level, double timeStep, CallbackInfo ci) {
        if (!joc$chain) {
            return;
        }

        double length = lastFirstSegmentExtension + (((ServerRopeStrand) (Object) this).getPoints().size() - 2) * ServerRopeStrand.SEGMENT_LENGTH;
        double path = getCurrentExtension();
        if (constraint == null) {
            if (!joc$makeRoomToTrim(level)) {
                joc$trimFirstSegment(joc$firstSegmentTrim + (path - length) * joc$TRIM_RATE);
            }
            return;
        }

        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        ServerSubLevel startBody = joc$getBody(container, RopeAttachmentPoint.START);
        ServerSubLevel endBody = joc$getBody(container, RopeAttachmentPoint.END);

        if (joc$lengthLimit == null) {
            joc$updateAnchor(level, RopeAttachmentPoint.START, joc$startAnchor);
            joc$updateAnchor(level, RopeAttachmentPoint.END, joc$endAnchor);
            joc$lengthLimit = physicsSystem.getPipeline().addConstraint(startBody, endBody,
                    new GenericConstraintConfiguration(joc$startAnchor, joc$endAnchor, new Quaterniond(), new Quaterniond()));
        }

        Vector3d start = startBody != null ? startBody.logicalPose().transformPosition(joc$startAnchor, new Vector3d()) : new Vector3d(joc$startAnchor);
        Vector3d end = endBody != null ? endBody.logicalPose().transformPosition(joc$endAnchor, new Vector3d()) : new Vector3d(joc$endAnchor);
        Vector3d direction = end.sub(start);
        double distance = direction.length();
        if (distance < joc$MIN_AIMABLE_DISTANCE) {
            return;
        }

        direction.div(distance);
        Quaterniond aim = new Quaterniond().rotationTo(1.0, 0.0, 0.0, direction.x, direction.y, direction.z);
        if (startBody != null) {
            aim.premul(new Quaterniond(startBody.logicalPose().orientation()).conjugate());
        }
        joc$lengthLimit.setFrame1(joc$startAnchor, aim);

        boolean bent = path > length * joc$TAUT_PATH_RATIO;
        if (bent) {
            joc$lengthLimitValue = Math.min(joc$lengthLimitValue, distance);
        } else if (length >= joc$lengthLimitValue) {
            joc$lengthLimitValue = length;
        } else {
            joc$lengthLimitValue = Math.max(length, Math.min(joc$lengthLimitValue, distance));
        }
        joc$lengthLimit.setLimit(ConstraintJointAxis.LINEAR_X, -joc$lengthLimitValue, joc$lengthLimitValue);

        if (!bent || length < joc$lengthLimitValue) {
            joc$trimFirstSegment(joc$firstSegmentTrim * (1.0 - joc$TRIM_RATE));
        } else if (!joc$makeRoomToTrim(level)) {
            joc$trimFirstSegment(joc$firstSegmentTrim + (path - length) * joc$TRIM_RATE);
        }
    }

    @Unique
    private boolean joc$makeRoomToTrim(ServerLevel level) {
        ServerRopeStrand strand = (ServerRopeStrand) (Object) this;
        if (lastFirstSegmentExtension >= joc$MIN_TRIMMABLE_SEGMENT || strand.getPoints().size() <= 2
                || level.getBlockEntity(getAttachment(RopeAttachmentPoint.START).blockAttachment()) instanceof RopeWinchBlockEntity) {
            return false;
        }
        strand.removeFirstPoint();
        strand.updateFirstSegmentExtension(lastFirstSegmentExtension + ServerRopeStrand.SEGMENT_LENGTH);
        return true;
    }

    @Unique
    private void joc$trimFirstSegment(double trim) {
        joc$firstSegmentTrim = Mth.clamp(trim, 0.0, lastFirstSegmentExtension);
        double firstSegment = lastFirstSegmentExtension - joc$firstSegmentTrim;
        if (Double.isNaN(joc$appliedFirstSegment) ? joc$firstSegmentTrim > 0.0 : Math.abs(firstSegment - joc$appliedFirstSegment) > joc$TRIM_STEP) {
            ((ServerRopeStrand) (Object) this).setFirstSegmentLength(firstSegment);
            joc$appliedFirstSegment = firstSegment;
        }
    }

    @Unique
    @Nullable
    private ServerSubLevel joc$getBody(ServerSubLevelContainer container, RopeAttachmentPoint point) {
        UUID id = getAttachment(point).subLevelID();
        return id != null ? (ServerSubLevel) container.getSubLevel(id) : null;
    }

    @Unique
    private void joc$updateAnchor(ServerLevel level, RopeAttachmentPoint point, Vector3d anchor) {
        RopeAttachment attachment = getAttachment(point);
        if (level.getBlockEntity(attachment.blockAttachment()) instanceof SmartBlockEntity smartBlockEntity) {
            RopeStrandHolderBehavior ropeHolder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
            if (ropeHolder != null) {
                anchor.set(JOMLConversion.toJOML(ropeHolder.getAttachmentPoint()));
                return;
            }
        }
        anchor.set(JOMLConversion.toJOML(attachment.blockAttachment().getCenter()));
    }
}
