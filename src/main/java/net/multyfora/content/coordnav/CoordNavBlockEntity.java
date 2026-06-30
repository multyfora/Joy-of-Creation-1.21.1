package net.multyfora.content.coordnav;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlock;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.createmod.catnip.animation.LerpedFloat;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.client.coordnav.CoordNavMenu;
import net.multyfora.index.JocBlockEntityTypes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
public class CoordNavBlockEntity extends SmartBlockEntity implements MenuProvider {

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
    public final LerpedFloat lerpedTiltDegrees = LerpedFloat.angular();
    // Computed angle from the block toward the target (0-360 degrees)
    private float relativeAngle;
    // Same thing as the relative, but for up and down rotation
    private float tiltAngle;
    // Tick counter for periodic distance recalculation
    private int ticks;
    private double distanceToTarget;
    private double lastDistanceToTarget;
    // Cache of last-known signal strengths per direction to avoid unnecessary neighbor updates
    private final Map<Direction, Integer> signalStrengthCache = new EnumMap<>(Direction.class);
    // Reference to the sublevel containing this block, if any
    private SubLevel subLevel;

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
    public float getTiltAngle() { return tiltAngle; }
    // Main tick: updates target tracking, redstone strengths, and client-side angle interpolation
    @Override
    public void tick() {
        super.tick();

        if (level == null) return;

        // Tick the lerped angle on the client for smooth pointer animation
        if (level.isClientSide) {
            lerpedAngleDegrees.tickChaser();
            lerpedTiltDegrees.tickChaser();
        }

        // Skip server-side logic on the client (except for virtual renders) and for virtual BE
        if (level.isClientSide && !isVirtual()) return;
        if (isVirtual()) return;

        // Attempt to get the sublevel containing this block
        try {
            subLevel = Sable.HELPER.getContaining(this);
        } catch (Exception ignored) {}

        double lastDistanceToTarget;
        if (!level.isClientSide) {
            updateTarget();

            // Recalculate distance to target every tick
            Vec3 targetLocal = getTargetPosition(false);
            if (targetLocal != null) {
                distanceToTarget = getProjectedSelfPos().distanceTo(getTargetPosition(true));
                if (distanceToTarget >= 5000) {
                    lastDistanceToTarget = distanceToTarget;
                }
                relativeAngle = calculateRelativeAngle();
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
        this.targetX = x; this.targetY = y; this.targetZ = z;
        this.hasTarget = true;
        this.currentTarget = new Vec3(x, y, z);
        relativeAngle = calculateRelativeAngle();
        setChanged();
        sendData();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            selectivelyUpdateNeighbors();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
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

            // Force an update when clearing, too
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
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
        if( this.level != null && this.level.isClientSide && this.isVirtual() ) {
            Direction facing = (Direction)this.getBlockState().getValue(NavTableBlock.FACING);
            Vec3i normal = facing.getNormal();
            double angleRad = Math.toRadians((double)this.lerpedAngleDegrees.getValue());
            Vec3 targetPos = new Vec3(Math.cos(angleRad), (double)0.0F, Math.sin(angleRad));
            targetPos = NavigationTarget.getPlaneProjectedPos(targetPos, normal);
            double dot = -targetPos.dot(Vec3.atLowerCornerOf(direction.getNormal()));
            return (int)(Math.asin(dot) / Math.PI * (double)30.0F + (double)0.5F);
        } else {
            int power = 0;
            net.multyfora.content.coordnav.NavigationTarget nti = new net.multyfora.content.coordnav.NavigationTarget(
                new GlobalPos(
                    this.level.dimension(),
                    this.getBlockPos()
                )
            );
            if( nti != null && this.getTargetPosition(false) != null ) {
                power = nti.getRedstoneStrength(this, direction);
            }

            return power;
        }
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
        tag.putFloat("tilt_angle", tiltAngle);
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
        tiltAngle = tag.getFloat("tilt_angle");
        if (clientPacket) {
            // On the client, start chasing the server-provided angle for smooth animation
            lerpedAngleDegrees.chase(relativeAngle, 1.0, LerpedFloat.Chaser.EXP);
            lerpedTiltDegrees.chase(tiltAngle, 1.0, LerpedFloat.Chaser.EXP);
        }
    }
    /**
     * Computes the angle (degrees) that, when applied as a YP rotation after the
     * per-facing reorientation in the renderer, points the spyglass toward the
     * projected target direction.
     *
     * Derivation sketch (all cases verified with rotation-matrix algebra):
     *   proj = normalise( project(targetDir, facingNormal) )
     *   FACING=UP/DOWN : atan2( proj.x,  proj.z)   — sweep in XZ, no reorientation
     *   FACING=NORTH   : atan2( proj.x, -proj.y)   — reorient XP90, tip starts at −Y
     *   FACING=SOUTH   : atan2( proj.x,  proj.y)   — reorient XP−90, tip starts at +Y
     *   FACING=EAST    : atan2( proj.y,  proj.z)   — reorient ZP90,  tip starts at +Y in YZ
     *   FACING=WEST    : atan2(-proj.y,  proj.z)   — reorient ZP−90, tip starts at −Y in YZ
     */
    private float calculateRelativeAngle() {
        if (currentTarget == null || level == null) return relativeAngle;

        Direction facing = getBlockState().getValue(CoordNavBlock.FACING);
        Vec3 targetWorld = getTargetPosition(true);
        if (targetWorld == null) return relativeAngle;

        Vec3 diff = rotateQuat(targetWorld.subtract(getProjectedSelfPos()), getSublevelRot());

        // Planar component (for yaw)
        Vec3 proj = net.multyfora.content.coordnav.NavigationTarget.getPlaneProjectedPos(diff, facing.getNormal());
        double planarLen = proj.length();

        // Tilt: angle between planar projection and full vector
        double fullLen = diff.length();
        if (fullLen < 1e-6) {
            tiltAngle = 0;
            return relativeAngle;
        }

        Vec3 facingNormal = Vec3.atLowerCornerOf(facing.getNormal());
        double outComponent = diff.dot(facingNormal);
        tiltAngle = (float) Math.toDegrees(Math.atan2(-outComponent, planarLen));

        // Yaw: angle in the sweep plane
        if (planarLen < 1e-6) return relativeAngle;
        Vec3 projNorm = proj.scale(1.0 / planarLen);

        double radians = switch (facing) {
            case UP    -> Math.atan2( projNorm.x,  projNorm.z);
            case DOWN  -> Math.atan2( projNorm.x, -projNorm.z);
            case NORTH -> Math.atan2( projNorm.x,  projNorm.y);
            case SOUTH -> Math.atan2( projNorm.x, -projNorm.y);
            case EAST  -> Math.atan2(-projNorm.y,  projNorm.z);
            case WEST  -> Math.atan2( projNorm.y,  projNorm.z);
        };
        return (float) Math.toDegrees(radians);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("screen.joc.coord_navigator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
        int i, @NotNull Inventory inventory, @NotNull Player player
    ) {
        return new CoordNavMenu(i, inventory, this);
    }

    public BlockPos getTarget() {
        if(!hasTarget) {
            return null;
        }
        return new BlockPos(
            (int)targetX,
            (int)targetY,
            (int)targetZ
        );
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = new CompoundTag();
        write(tag, registries, false);
        return tag;
    }

    @Override
    public @NotNull ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
