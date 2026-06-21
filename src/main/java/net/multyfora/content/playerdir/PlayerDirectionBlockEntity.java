package net.multyfora.content.playerdir;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.index.JocBlockEntityTypes;

import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Block entity for the Player Direction block.
 * Finds the nearest player within 10 blocks, captures their look direction, and computes
 * redstone signal outputs on each lateral side based on where the player is facing,
 * relative to the block's facing direction. Fully sublevel-aware.
 **/
public class PlayerDirectionBlockEntity extends SmartBlockEntity {

    // The player's look direction, cached and transformed into the block's local space
    private Vec3 lookDir;
    // Reference to the containing sublevel, if any
    private SubLevel subLevel;
    // Cache of last-known signal strengths per direction
    private final Map<Direction, Integer> signalStrengthCache = new EnumMap<>(Direction.class);

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
     * Server tick: finds the nearest player, captures their look direction, inverts and
     * rotates it into the block's local coordinate space, and updates redstone outputs
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

        // Find nearest non-spectator, alive player within tracking range
        Player nearestPlayer = level.getNearestPlayer(
            worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), TRACK_RANGE,
            p -> p.isAlive() && !p.isSpectator()
        );
        if (nearestPlayer == null) return;

        /**
         * Get the player's look angle, invert it (we want the direction from player to block)
         * and rotate by inverse sublevel rotation to get local-space direction
         **/
        Vec3 lookAngle = nearestPlayer.getLookAngle();
        Quaterniond invRot = new Quaterniond(getSublevelRot()).invert();
        // Scale by -1 to invert the direction (we want the direction the player is looking toward, not away from)
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
            // Immediately capture player look direction when turned on
            Player nearestPlayer = level.getNearestPlayer(
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), TRACK_RANGE,
                p -> p.isAlive() && !p.isSpectator()
            );
            if (nearestPlayer != null) {
                Vec3 lookAngle = nearestPlayer.getLookAngle();
                Quaterniond invRot = new Quaterniond(getSublevelRot()).invert();
                lookDir = rotateQuat(lookAngle.normalize(), invRot).scale(-1);
            }
        }

        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    /**
     * Computes redstone signal strength for a given direction based on how closely the
     * player's look direction aligns with that direction on the block's facing plane.
     **/
    public int getRedstoneStrength(Direction direction) {
        if (level == null || !getBlockState().getValue(PlayerDirectionBlock.POWERED)) return 0;

        Direction facing = getBlockState().getValue(PlayerDirectionBlock.FACING);
        Vec3i facingNormal = facing.getNormal();

        // Project the look direction onto the plane perpendicular to the block's facing axis
        Vec3 projected = getPlaneProjectedPos(lookDir, facingNormal);
        double planeDist = projected.length();

        if (planeDist <= EPSILON) return 0;

        // Dot product with the queried direction, normalised, mapped via arcsin to 0-15
        double dot = projected.dot(Vec3.atLowerCornerOf(direction.getNormal()));
        return (int) (Math.asin(dot / planeDist) / Math.PI * 30.0 + 0.5);
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
            if (dir.getAxis() == state.getValue(PlayerDirectionBlock.FACING).getAxis()) continue;

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

    // Projects a vector onto the plane perpendicular to the given normal
    public static Vec3 getPlaneProjectedPos(Vec3 vec, Vec3i normal) {
        Vec3 normalVec = Vec3.atLowerCornerOf(normal);
        double dot = vec.dot(normalVec);
        return vec.subtract(normalVec.scale(dot));
    }

    // Rotates a Vec3 by a double-precision quaternion
    public static Vec3 rotateQuat(Vec3 vec, Quaterniond quat) {
        if (quat.equals(new Quaterniond())) return vec;
        Vector3d v = new Vector3d(vec.x, vec.y, vec.z);
        quat.transform(v);
        return new Vec3(v.x, v.y, v.z);
    }
}
