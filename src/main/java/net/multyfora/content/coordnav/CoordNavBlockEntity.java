package net.multyfora.content.coordnav;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.createmod.catnip.animation.LerpedFloat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.index.JocBlockEntityTypes;

import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Block entity for the Coordinate Navigator.
 * Computes redstone signal strengths on each lateral side based on the direction from the
 * block to a configurable target coordinate. Includes sublevel-aware coordinate transforms
 * so it works correctly on moving ships.
 **/
public class CoordNavBlockEntity extends SmartBlockEntity {

    // Target coordinates (in logical sublevel space)
    private double targetX;
    private double targetY;
    private double targetZ;
    // Cached target vector, null when no target is set
    private Vec3 currentTarget;
    private boolean hasTarget;

    // Whether the block is currently emitting power
    public boolean isPowering;
    // Smoothed angle for client-side rendering of the pointer
    public final LerpedFloat lerpedAngleDegrees = LerpedFloat.angular();
    // Computed angle from the block toward the target (0-360 degrees)
    private float relativeAngle;
    // Tick counter for periodic distance recalculation
    private int ticks;
    private double distanceToTarget;
    private double lastDistanceToTarget;
    // Cache of last-known signal strengths per direction to avoid unnecessary neighbor updates
    private final Map<Direction, Integer> signalStrengthCache = new EnumMap<>(Direction.class);
    // Reference to the sublevel containing this block, if any
    private SubLevel subLevel;

    // Maximum range: targets beyond this distance return 0 signal
    private static final double MAX_RANGE = 200.0;
    // Deadzone radius: targets within this distance return 0 signal
    private static final float DEADZONE = 2.0f;
    private static final double EPSILON = 1.0e-4;

    public CoordNavBlockEntity(BlockPos pos, BlockState state) {
        super(JocBlockEntityTypes.COORD_NAV.get(), pos, state);
        targetX = 0;
        targetY = 0;
        targetZ = 0;
        currentTarget = null;
        hasTarget = false;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    // Main tick: updates target tracking, redstone strengths, and client-side angle interpolation
    @Override
    public void tick() {
        super.tick();

        if (level == null) return;

        // Tick the lerped angle on the client for smooth pointer animation
        if (level.isClientSide) {
            lerpedAngleDegrees.tickChaser();
        }

        // Skip server-side logic on the client (except for virtual renders) and for virtual BE
        if (level.isClientSide && !isVirtual()) return;
        if (isVirtual()) return;

        // Attempt to get the sublevel containing this block
        try {
            subLevel = Sable.HELPER.getContaining(this);
        } catch (Exception ignored) {}

        if (!level.isClientSide) {
            updateTarget();
            updateCurrentAngle();

            // Recalculate distance to target every tick
            Vec3 targetLocal = getTargetPosition(false);
            if (targetLocal != null) {
                distanceToTarget = getProjectedSelfPos().distanceTo(getTargetPosition(true));
                if (distanceToTarget >= 5000) {
                    lastDistanceToTarget = distanceToTarget;
                }
                sendData();
            }
        }

        // Update neighbor blocks if signal strength changed on any side
        if (selectivelyUpdateNeighbors()) {
            notifyUpdate();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }

        // Periodic distance recalculation every 10 ticks to reduce CPU load
        if (!level.isClientSide && getTargetPosition(false) != null) {
            ticks++;
            if (ticks >= 10) {
                ticks = 0;
                lastDistanceToTarget = distanceToTarget;
                distanceToTarget = getProjectedSelfPos().distanceTo(getTargetPosition(true));
            }
        }
    }

    // Updates the cached currentTarget vector from stored coordinates
    private void updateTarget() {
        if (hasTarget) {
            currentTarget = new Vec3(targetX, targetY, targetZ);
        } else {
            currentTarget = null;
        }
        notifyUpdate();
    }

    // Sets the target coordinates and triggers redstone recalculation
    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.hasTarget = true;
        this.currentTarget = new Vec3(x, y, z);
        setChanged();
        sendData();
        if (level != null && !level.isClientSide) {
            for (Direction dir : Direction.values()) {
                signalStrengthCache.put(dir, getRedstoneStrength(dir));
            }
            selectivelyUpdateNeighbors();
        }
    }

    // Clears the target and resets redstone output
    public void clearTarget() {
        this.hasTarget = false;
        this.currentTarget = null;
        this.targetX = 0;
        this.targetY = 0;
        this.targetZ = 0;
        setChanged();
        sendData();
        if (level != null && !level.isClientSide) {
            selectivelyUpdateNeighbors();
        }
    }

    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetZ() { return targetZ; }
    public boolean hasTarget() { return hasTarget; }

    /**
     * Computes the redstone signal strength for a given direction.
     * The strength is based on the dot product of the direction vector with the projected
     * target direction on the plane perpendicular to the block's facing axis.
     * Uses arcsin to map the angle to a 0-15 signal range.
     **/
    public int getRedstoneStrength(Direction direction) {
        if (level == null) return 0;

        // Client-side virtual render path: uses the lerped angle for display purposes
        if (level.isClientSide && isVirtual()) {
            Direction facing = getBlockState().getValue(CoordNavBlock.FACING);
            Vec3i facingNormal = facing.getNormal();
            double angleRad = Math.toRadians(lerpedAngleDegrees.getValue());
            Vec3 dirVec = new Vec3(Math.cos(angleRad), 0, Math.sin(angleRad));
            Vec3 projected = getPlaneProjectedPos(dirVec, facingNormal);
            double dot = projected.dot(Vec3.atLowerCornerOf(direction.getNormal()));
            return (int) (Math.asin(dot) / Math.PI * 30.0 + 0.5);
        }

        Vec3 targetLocal = getTargetPosition(false);
        if (targetLocal == null) return 0;

        Vec3 targetWorld = getTargetPosition(true);
        Vec3 selfPos = getProjectedSelfPos();
        Vec3 toTarget = targetWorld.subtract(selfPos);

        Direction facing = getBlockState().getValue(CoordNavBlock.FACING);
        Vec3i facingNormal = facing.getNormal();

        /**
         * Rotate the target vector by the sublevel's orientation so the computation
         * works correctly on ships/rotated sublevels
         **/
        Quaterniond rot = getSublevelRot();
        toTarget = rotateQuat(toTarget, rot);

        // Project the target vector onto the plane perpendicular to the facing direction
        Vec3 projected = getPlaneProjectedPos(toTarget, facingNormal);
        double planeDist = projected.length();

        // Range and deadzone checks
        if (MAX_RANGE > 0 && planeDist > MAX_RANGE - EPSILON) return 0;
        if (planeDist <= DEADZONE - EPSILON) return 0;

        // Compute dot product with the queried direction, normalise, and map via arcsin to 0-15
        double dot = projected.dot(Vec3.atLowerCornerOf(direction.getNormal()));
        double normalized = dot / planeDist;
        return (int) (Math.asin(normalized) / Math.PI * 30.0 + 0.5);
    }

    // Returns the current target angle for client-side rendering, or 0 if no target
    public float getClientTargetAngle(float partialTick) {
        if (level != null && level.isClientSide) {
            float val = lerpedAngleDegrees.getValue(partialTick);
            return (float) -Math.toRadians(val);
        }
        return 0;
    }

    // Sets the target angle chase value with exponential smoothing
    public void forceCurrentAngle(float angle) {
        lerpedAngleDegrees.chase(angle, 0.8, LerpedFloat.Chaser.EXP);
    }

    /**
     * Recalculates the angle from this block toward the target, accounting for sublevel rotation
     * and block facing direction. Only runs on the server side.
     **/
    private void updateCurrentAngle() {
        if (level.isClientSide) {
            relativeAngle = 0;
            return;
        }

        Vec3 target = getTargetPosition(false);
        if (target == null) {
            relativeAngle = 0;
            return;
        }

        Vec3 self = getProjectedSelfPos();
        Vec3 toTarget = getTargetPosition(true).subtract(self);
        Vec3 normalized = toTarget.normalize();

        // Remove sublevel rotation so the angle is relative to the sublevel's orientation
        Quaterniond rot = getSublevelRot();
        normalized = rotateQuat(normalized, rot);

        // Rotate by the block's facing direction so the pointer aligns with the block's face
        Direction facing = getBlockState().getValue(CoordNavBlock.FACING);
        Quaternionf facingRot = facing.getRotation();
        normalized = rotateQuat(normalized, facingRot);

        // Flatten to the horizontal plane for angle computation
        normalized = new Vec3(normalized.x, 0, normalized.z);

        // Compute angle in degrees relative to the positive X axis, normalised to 0-360
        float angle = (float) Math.toDegrees(Math.atan2(normalized.z, normalized.x));
        relativeAngle = (360.0f + angle) % 360.0f;
        forceCurrentAngle(relativeAngle);
    }

    // Returns the block's position transformed by the sublevel's logical pose (for sublevel-aware positioning)
    public Vec3 getProjectedSelfPos() {
        Vec3 pos = Vec3.atCenterOf(worldPosition);
        if (subLevel != null) {
            try {
                pos = subLevel.logicalPose().transformPosition(pos);
            } catch (Exception ignored) {}
        }
        return pos;
    }

    // Returns the target position, optionally transformed into world space from sublevel space
    public Vec3 getTargetPosition(boolean worldSpace) {
        if (currentTarget == null) return null;
        if (worldSpace && subLevel != null) {
            try {
                return Sable.HELPER.projectOutOfSubLevel(level, currentTarget);
            } catch (Exception ignored) {}
        }
        return currentTarget;
    }

    // Returns the orientation quaternion of the containing sublevel, or identity if not in a sublevel
    public Quaterniond getSublevelRot() {
        Quaterniond q = new Quaterniond();
        if (subLevel != null) {
            try {
                q = subLevel.logicalPose().orientation();
            } catch (Exception ignored) {}
        }
        return q;
    }

    public double distanceToTarget() {
        return distanceToTarget;
    }

    public double lastDistanceToTarget() {
        return lastDistanceToTarget;
    }

    /**
     * Checks each lateral direction for signal strength changes and updates neighbors accordingly.
     * Returns true if any strength changed.
     **/
    private boolean selectivelyUpdateNeighbors() {
        if (level == null || level.isClientSide) return false;

        BlockState state = getBlockState();
        boolean updated = false;

        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == state.getValue(CoordNavBlock.FACING).getAxis()) continue;

            int prev = signalStrengthCache.getOrDefault(dir, -1);
            int curr = getRedstoneStrength(dir);
            if (prev != curr) {
                signalStrengthCache.put(dir, curr);
                level.updateNeighborsAt(worldPosition.relative(dir), state.getBlock());
                updated = true;
            }
        }
        return updated;
    }

    /**
     * Projects a vector onto a plane defined by a normal vector.
     * Used to flatten the target direction onto the plane perpendicular to the block's facing axis.
     **/
    public static Vec3 getPlaneProjectedPos(Vec3 vec, Vec3i normal) {
        Vec3 normalVec = Vec3.atLowerCornerOf(normal);
        double dot = vec.dot(normalVec);
        return vec.subtract(normalVec.scale(dot));
    }

    // Rotates a Vec3 by a double-precision quaternion (for sublevel orientation)
    public static Vec3 rotateQuat(Vec3 vec, Quaterniond quat) {
        if (quat.equals(new Quaterniond())) return vec;
        Vector3d v = new Vector3d(vec.x, vec.y, vec.z);
        quat.transform(v);
        return new Vec3(v.x, v.y, v.z);
    }

    // Rotates a Vec3 by a float-precision quaternion (for block facing rotation)
    public static Vec3 rotateQuat(Vec3 vec, Quaternionf quat) {
        if (quat.equals(new Quaternionf())) return vec;
        org.joml.Vector3f v = new org.joml.Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
        quat.transform(v);
        return new Vec3(v.x, v.y, v.z);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("target_x", targetX);
        tag.putDouble("target_y", targetY);
        tag.putDouble("target_z", targetZ);
        tag.putBoolean("has_target", hasTarget);
        tag.putFloat("relative_angle", relativeAngle);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        targetX = tag.getDouble("target_x");
        targetY = tag.getDouble("target_y");
        targetZ = tag.getDouble("target_z");
        hasTarget = tag.getBoolean("has_target");
        if (hasTarget) {
            currentTarget = new Vec3(targetX, targetY, targetZ);
        } else {
            currentTarget = null;
        }
        isPowering = hasTarget;
        relativeAngle = tag.getFloat("relative_angle");
        if (clientPacket) {
            // On the client, start chasing the server-provided angle for smooth animation
            lerpedAngleDegrees.chase(relativeAngle, 1.0, LerpedFloat.Chaser.EXP);
        }
    }
}
