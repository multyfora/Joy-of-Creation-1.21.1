package net.multyfora.content.shatter_assembler;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ShatterLogic {

    /**
     * Shatters a collection of blocks by turning them into individual SubLevels.
     */
    public static void shatterStructure(Level level, Collection<BlockPos> blocks) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (BlockPos anchor : blocks) {
            if (VoxelNeighborhoodState.isSolid(serverLevel, anchor, serverLevel.getBlockState(anchor))) {
                assembleSingleBlock(serverLevel, anchor);
            }
        }
    }

    private static void assembleSingleBlock(ServerLevel level, BlockPos anchor) {
        BoundingBox3i bounds = new BoundingBox3i(
                anchor.getX(), anchor.getY(), anchor.getZ(),
                anchor.getX() + 1, anchor.getY() + 1, anchor.getZ() + 1
        );

        bounds.set(
                bounds.minX() - 1, bounds.minY() - 1, bounds.minZ() - 1,
                bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1
        );

        SubLevelAssemblyHelper.assembleBlocks(level, anchor, List.of(anchor), bounds);
    }
    public static Collection<BlockPos> getConnectedBlocks(Level level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(startPos);
        visited.add(startPos);

        int maxBlocks = 512;

        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = queue.poll();

            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                BlockState state = level.getBlockState(neighbor);

                if (!state.isAir() && !visited.contains(neighbor) && state.isSolidRender(level, neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }
}