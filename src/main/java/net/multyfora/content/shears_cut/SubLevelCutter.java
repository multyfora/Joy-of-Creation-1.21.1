package net.multyfora.content.shears_cut;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;

public final class SubLevelCutter {

    public record Result(int pieceCount, int largestSize) {}

    /** Cuts a sub-level along a plane, limited to the rectangle spanning point1..point2.
     *  Blocks still connected to the rest of the structure around the plane's extent are
     *  left untouched; disconnected groups become their own new sub-levels, with the
     *  largest group staying as the original. Returns null if nothing separated. */
    public static Result cut(ServerLevel level, ServerSubLevel subLevel,
                             BlockPos point1, BlockPos point2, Direction.Axis axis, int planeCoord) {
        LevelPlot plot = subLevel.getPlot();

        int rMinX = Math.min(point1.getX(), point2.getX());
        int rMaxX = Math.max(point1.getX(), point2.getX());
        int rMinY = Math.min(point1.getY(), point2.getY());
        int rMaxY = Math.max(point1.getY(), point2.getY());
        int rMinZ = Math.min(point1.getZ(), point2.getZ());
        int rMaxZ = Math.max(point1.getZ(), point2.getZ());

        var plotBounds = plot.getBoundingBox().toMojang();
        Set<BlockPos> allBlocks = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosedStream(plotBounds).map(BlockPos::immutable).toList()) {
            if (!isAir(plot, pos)) allBlocks.add(pos);
        }
        if (allBlocks.isEmpty()) return null;

        List<List<BlockPos>> components = floodFillComponents(
                allBlocks, axis, planeCoord, rMinX, rMaxX, rMinY, rMaxY, rMinZ, rMaxZ);

        if (components.size() <= 1) return null;

        components.sort((a, b) -> b.size() - a.size());
        List<List<BlockPos>> toExtract = components.subList(1, components.size());

        for (List<BlockPos> comp : toExtract) {
            extractComponent(level, subLevel, comp);
        }

        plot.updateBoundingBox();
        subLevel.buildMassTracker();
        subLevel.updateMergedMassData(0);

        return new Result(components.size(), components.get(0).size());
    }

    private static List<List<BlockPos>> floodFillComponents(
            Set<BlockPos> allBlocks, Direction.Axis axis, int planeCoord,
            int rMinX, int rMaxX, int rMinY, int rMaxY, int rMinZ, int rMaxZ) {

        Set<BlockPos> visited = new HashSet<>();
        List<List<BlockPos>> components = new ArrayList<>();

        for (BlockPos start : allBlocks) {
            if (visited.contains(start)) continue;

            List<BlockPos> comp = new ArrayList<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);

            while (!queue.isEmpty()) {
                BlockPos cur = queue.poll();
                comp.add(cur);

                for (Direction dir : Direction.values()) {
                    BlockPos next = cur.relative(dir);
                    if (!allBlocks.contains(next) || visited.contains(next)) continue;

                    if (dir.getAxis() == axis) {
                        int shared = Math.max(cur.get(axis), next.get(axis));
                        boolean inExtent = switch (axis) {
                            case Y -> cur.getX() >= rMinX && cur.getX() <= rMaxX && cur.getZ() >= rMinZ && cur.getZ() <= rMaxZ;
                            case Z -> cur.getX() >= rMinX && cur.getX() <= rMaxX && cur.getY() >= rMinY && cur.getY() <= rMaxY;
                            case X -> cur.getY() >= rMinY && cur.getY() <= rMaxY && cur.getZ() >= rMinZ && cur.getZ() <= rMaxZ;
                        };
                        if (shared == planeCoord && inExtent) continue; // cut here
                    }

                    visited.add(next);
                    queue.add(next);
                }
            }
            components.add(comp);
        }
        return components;
    }

    private static void extractComponent(ServerLevel level, ServerSubLevel subLevel, List<BlockPos> blocks) {
        BoundingBox3i bounds = BoundingBox3i.from(blocks);
        if (bounds == null) return;
        bounds.set(
                bounds.minX() - 1, bounds.minY() - 1, bounds.minZ() - 1,
                bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1
        );
        SubLevelAssemblyHelper.assembleBlocks(level, blocks.get(0), blocks, bounds);
        removeBlocks(subLevel, blocks);
    }

    private static boolean isAir(@UnknownNullability LevelPlot plot, BlockPos pos) {
        ChunkPos localChunk = plot.toLocal(new ChunkPos(pos));
        LevelChunk chunk = plot.getChunk(localChunk);
        if (chunk == null) return true;
        return chunk.getBlockState(pos).isAir();
    }

    private static void removeBlocks(ServerSubLevel subLevel, List<BlockPos> blocks) {
        LevelPlot plot = subLevel.getPlot();
        SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(subLevel.getLevel());

        for (BlockPos pos : blocks) {
            ChunkPos localChunk = plot.toLocal(new ChunkPos(pos));
            LevelChunk chunk = plot.getChunk(localChunk);
            if (chunk == null) continue;

            BlockState old = chunk.getBlockState(pos);
            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
            if (physicsSystem == null) continue;

            int localX = pos.getX() & 15;
            int localY = pos.getY() & 15;
            int localZ = pos.getZ() & 15;

            PlotChunkHolder holder = plot.getChunkHolder(localChunk);
            if (holder != null) {
                holder.handleBlockChange(localX, localY, localZ, old, Blocks.AIR.defaultBlockState());
            }

            SectionPos sectionPos = SectionPos.of(pos);
            var section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
            physicsSystem.handleBlockChange(sectionPos, section, localX, localY, localZ, old, Blocks.AIR.defaultBlockState());
        }
    }
}