package net.multyfora.content.shatter_assembler;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ShatterLogic {

    private static final TagKey<Block> SIMULATED_NON_MOVABLE =
            BlockTags.create(ResourceLocation.fromNamespaceAndPath("simulated", "non_movable"));

    public static boolean canAssemble(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (state.is(SIMULATED_NON_MOVABLE)) return false;
        return BlockMovementChecks.isMovementAllowed(state, level, pos);
    }

    public static void shatterStructure(Level level, Collection<BlockPos> blocks) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (BlockPos anchor : blocks) {
            BlockState state = serverLevel.getBlockState(anchor);
            if (canAssemble(serverLevel, anchor, state) && VoxelNeighborhoodState.isSolid(serverLevel, anchor, state)) {
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

        if (!canAssemble(level, startPos, level.getBlockState(startPos))) {
            return visited;
        }

        queue.add(startPos);
        visited.add(startPos);

        int maxBlocks = 512;

        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = queue.poll();

            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                BlockState state = level.getBlockState(neighbor);

                if (!visited.contains(neighbor) && state.isSolidRender(level, neighbor) && canAssemble(level, neighbor, state)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }
}