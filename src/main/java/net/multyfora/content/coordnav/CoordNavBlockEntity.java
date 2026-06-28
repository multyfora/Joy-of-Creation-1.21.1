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
    // Computed angle from the block toward the target (0-360 degrees)
    private float relativeAngle;
    // Tick counter for periodic distance recalculation
    private int ticks;
    private double distanceToTarget;
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
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            selectivelyUpdateNeighbors();
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
