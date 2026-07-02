package net.multyfora.content.coordnav;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.createmod.catnip.animation.LerpedFloat;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.client.coordnav.CoordNavMenu;
import net.multyfora.content.Pointer;
import net.multyfora.content.SpaceUtils;
import net.multyfora.index.JocBlockEntityTypes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    // Spyglass Pointer
    public final Pointer spyglassPointer = new Pointer();

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

    // Main tick: updates target tracking, redstone strengths, and client-side angle interpolation
    @Override
    public void tick() {
        super.tick();

        if(level == null) {
            return;
        }

        // Tick the lerped angle on the client for smooth pointer animation
        if(level.isClientSide) {
            spyglassPointer.lerpedPitchDegrees.tickChaser();
            spyglassPointer.lerpedYawDegrees.tickChaser();
        }

        // Skip server-side logic on the client (except for virtual renders) and for virtual BE
        if(isVirtual() || level.isClientSide) {
            return;
        }

        // Attempt to get the sublevel containing this block
        try {
            this.subLevel = Sable.HELPER.getContaining(this);
        } catch(Exception ignored) {}

        // Recalculate distance to target every tick
        updateTarget();
        Vec3 targetLocal = getTargetPosition(false);
        if (targetLocal != null) {
            distanceToTarget = SpaceUtils
                .getProjectedSelfPos(subLevel, worldPosition)
                .distanceTo( getTargetPosition(true) )
            ;
            if (distanceToTarget >= 5000) {
                lastDistanceToTarget = distanceToTarget;
            }
            spyglassPointer.tick(this);
            sendData();
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
                distanceToTarget = SpaceUtils
                    .getProjectedSelfPos(subLevel, worldPosition)
                    .distanceTo( getTargetPosition(true) )
                ;
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
        spyglassPointer.calculateRelativeAngle(this);
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

    public int getRedstoneStrength(Direction direction) {
        if (this.level == null || getTargetPosition(false) == null) {
            return 0;
        }

        Vec3 targetWorld = getTargetPosition(true);
        if (targetWorld == null) return 0;

        Vec3 selfPos = SpaceUtils.getProjectedSelfPos(subLevel, worldPosition);
        Vec3 diffWorld = targetWorld.subtract(selfPos);

        if (diffWorld.lengthSqr() < 1e-6) return 0;

        var forwardRot = SpaceUtils.getSublevelRot(subLevel);
        var inverseRot = new org.joml.Quaterniond(forwardRot).conjugate();
        Vec3 diffLocal = SpaceUtils.rotateQuat(diffWorld, inverseRot).normalize();

        Vec3 dirNormal = Vec3.atLowerCornerOf(direction.getNormal());
        double dot = -diffLocal.dot(dirNormal);

        if (dot <= 0) return 0;

        int power = (int) Math.round((Math.asin(dot) / Math.PI) * 30.0);
        return Math.min(15, Math.max(0, power));
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

    /**
     * Checks each lateral direction for signal strength changes and updates neighbors accordingly.
     * Returns true if any strength changed.
     **/
    private boolean selectivelyUpdateNeighbors() {
        if (level == null || level.isClientSide) return false;

        BlockState state = getBlockState();
        boolean updated = false;

        for (Direction neighborDir : Direction.values()) {
            Direction queryDir = neighborDir.getOpposite();

            int prev = signalStrengthCache.getOrDefault(queryDir, -1);
            int curr = getRedstoneStrength(queryDir);

            if (prev != curr) {
                signalStrengthCache.put(queryDir, curr);
                level.updateNeighborsAt(worldPosition.relative(neighborDir), state.getBlock());
                updated = true;
            }
        }
        return updated;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("target_x", targetX);
        tag.putDouble("target_y", targetY);
        tag.putDouble("target_z", targetZ);
        tag.putBoolean("has_target", hasTarget);
        tag.putFloat("relative_angle", spyglassPointer.getYaw() );
        tag.putFloat("tilt_angle",     spyglassPointer.getPitch()   );
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        targetX   = tag.getDouble("target_x");
        targetY   = tag.getDouble("target_y");
        targetZ   = tag.getDouble("target_z");
        hasTarget = tag.getBoolean("has_target");
        if (hasTarget) {
            currentTarget = new Vec3(targetX, targetY, targetZ);
        } else {
            currentTarget = null;
        }
        isPowering = hasTarget;


        float yaw   = tag.getFloat("relative_angle");
        float pitch = tag.getFloat("tilt_angle");
        spyglassPointer.setYaw(yaw);
        spyglassPointer.setPitch(pitch);

        if (clientPacket) {
            spyglassPointer.lerpedYawDegrees.chase(yaw,   1.0, LerpedFloat.Chaser.EXP);
            spyglassPointer.lerpedPitchDegrees.chase(pitch, 1.0, LerpedFloat.Chaser.EXP);
        }
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

    public double getTargetX() {
        return targetX;
    }
    public double getTargetY() {
        return targetY;
    }
    public double getTargetZ() {
        return targetZ;
    }
    public boolean hasTarget() {
        return hasTarget;
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

    public BlockPos getWorldPosition() {
        return worldPosition;
    }

    public SubLevel getSubLevel() {
        return subLevel;
    }

}
