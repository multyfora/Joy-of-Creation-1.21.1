package net.multyfora.content.playerdir;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.index.JocBlockEntityTypes;

import org.joml.Quaterniond;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Block entity for the Player Direction block.
 * Tracks the player who last right-clicked the block (not just the nearest), captures
 * their look direction, and computes redstone signal outputs on all 6 sides via a single
 * uniform direct-dot formula (no FACING property, no axis branching).
 * Fully sublevel-aware.
 **/
public class PlayerDirectionBlockEntity extends SmartBlockEntity {

    // The player's look direction, cached and transformed into the block's local space
    private Vec3 lookDir;
    // Reference to the containing sublevel, if any
    private SubLevel subLevel;
    // Cache of last-known signal strengths per direction
    private final Map<Direction, Integer> signalStrengthCache = new EnumMap<>(Direction.class);

    // UUID of the player who last activated this block via right-click
    @Nullable
    private UUID trackedPlayerUUID = null;

    private static final double EPSILON = 1.0e-6;
    // Maximum distance to track players
    private static final double TRACK_RANGE = 10.0;

    public PlayerDirectionBlockEntity(BlockPos pos, BlockState state) {
        super(JocBlockEntityTypes.PLAYER_DIRECTION.get(), pos, state);
        lookDir = Vec3.ZERO;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    /**
     * Call this from PlayerDirectionBlock#useWithoutItem when a player right-clicks the block.
     * Sets that player as the one whose look direction is tracked going forward.
     */
    public void setTrackedPlayer(Player player) {
        trackedPlayerUUID = player.getUUID();
        signalStrengthCache.clear();
        setChanged();
    }

    /**
     * Returns the tracked player from the current level, or null if they are
     * offline, dead, spectating, or no player has been registered yet.
     */
    @Nullable
    private Player getTrackedPlayer() {
        if (trackedPlayerUUID == null || level == null) return null;
        Player player = level.getPlayerByUUID(trackedPlayerUUID);
        if (player == null || !player.isAlive() || player.isSpectator()) return null;
        return player;
    }

    /**
     * Server tick: looks up the tracked player, captures their look direction, inverts
     * and rotates it into the block's local coordinate space, and updates redstone outputs.
     **/
    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        if (level.isClientSide) return;
        if (isVirtual()) return;

        // Refresh sublevel reference each tick in case the block was moved
        try {
            subLevel = Sable.HELPER.getContaining(this);
        } catch (Exception ignored) {}

        // Skip if the block is not powered (toggled off)
        if (!getBlockState().getValue(PlayerDirectionBlock.POWERED)) return;

        Player trackedPlayer = getTrackedPlayer();
        if (trackedPlayer == null) return;

        /**
         * Get the player's look angle, invert it (we want the direction from player to block)
         * and rotate by inverse sublevel rotation to get local-space direction.
         **/
        Vec3 lookAngle = trackedPlayer.getLookAngle();
        Quaterniond invRot = new Quaterniond(getSublevelRot()).invert();
        lookDir = rotateQuat(lookAngle.normalize(), invRot).scale(-1);

        if (selectivelyUpdateNeighbors()) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    // Called when the block is toggled on/off; updates tracking immediately
    public void onPowerToggled() {
        if (level == null || level.isClientSide) return;

        try {
            subLevel = Sable.HELPER.getContaining(this);
        } catch (Exception ignored) {}

        signalStrengthCache.clear();

        if (getBlockState().getValue(PlayerDirectionBlock.POWERED)) {
            // Immediately capture tracked player's look direction when turned on
            Player trackedPlayer = getTrackedPlayer();
            if (trackedPlayer != null) {
                Vec3 lookAngle = trackedPlayer.getLookAngle();
                Quaterniond invRot = new Quaterniond(getSublevelRot()).invert();
                lookDir = rotateQuat(lookAngle.normalize(), invRot).scale(-1);
            }
        }

        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    /**
     * Computes redstone signal strength for a given direction.
     *
     * Uses a single direct dot-product formula for ALL 6 directions: dot(lookDir, dirNormal)
     * is cos(theta) between the player's look direction and that side's normal (both unit
     * vectors), and arcsin(cos(theta)) = 90 - theta is a clean linear remap of theta into
     * the [-15, 15] signal range. No FACING property, no plane-projection special case —
     * every side is computed identically, so there is no per-axis asymmetry.
     **/
    public int getRedstoneStrength(Direction direction) {
        if (level == null || !getBlockState().getValue(PlayerDirectionBlock.POWERED)) return 0;
        if (lookDir.lengthSqr() <= EPSILON) return 0;

        Vec3 dirNormal = Vec3.atLowerCornerOf(direction.getNormal());
        double dot = Math.max(-1.0, Math.min(1.0, lookDir.dot(dirNormal)));
        return (int) (Math.asin(dot) / Math.PI * 30.0 + 0.5);
    }

    // Returns the orientation quaternion of the containing sublevel, or identity if not in one
    public Quaterniond getSublevelRot() {
        Quaterniond q = new Quaterniond();
        if (subLevel != null) {
            try {
                q = subLevel.logicalPose().orientation();
            } catch (Exception ignored) {}
        }
        return q;
    }


    // Compares current signal strengths with cached values and updates neighbors if any changed
    private boolean selectivelyUpdateNeighbors() {
        if (level == null || level.isClientSide) return false;

        BlockState state = getBlockState();
        boolean updated = false;

        for (Direction dir : Direction.values()) {
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
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (trackedPlayerUUID != null) {
            tag.putUUID("TrackedPlayer", trackedPlayerUUID);
        }
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.hasUUID("TrackedPlayer")) {
            trackedPlayerUUID = tag.getUUID("TrackedPlayer");
        }
    }

    // Rotates a Vec3 by a double-precision quaternion
    public static Vec3 rotateQuat(Vec3 vec, Quaterniond quat) {
        if (quat.equals(new Quaterniond())) return vec;
        Vector3d v = new Vector3d(vec.x, vec.y, vec.z);
        quat.transform(v);
        return new Vec3(v.x, v.y, v.z);
    }
}