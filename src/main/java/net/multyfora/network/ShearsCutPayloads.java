package net.multyfora.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

import java.util.*;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.shears_cut.ShearsCutState;
import net.multyfora.content.shears_cut.SubLevelCutter;
import org.jetbrains.annotations.UnknownNullability;

public class ShearsCutPayloads {

    public record ShearsCutPayload(BlockPos point1, BlockPos point2, Direction orientation) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "shears_cut");
        public static final Type<ShearsCutPayload> TYPE = new Type<>(ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, ShearsCutPayload> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, ShearsCutPayload::point1,
                        BlockPos.STREAM_CODEC, ShearsCutPayload::point2,
                        Direction.STREAM_CODEC, ShearsCutPayload::orientation,
                        ShearsCutPayload::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public void handle(ServerPlayer player) {
            ServerLevel level = player.serverLevel();

            SubLevel containing = Sable.HELPER.getContaining(level, point1);
            if (!(containing instanceof ServerSubLevel subLevel) || subLevel.isRemoved()) {
                player.sendSystemMessage(Component.literal("No sublevel found at that position"));
                return;
            }

            SubLevelCutter.Result result = SubLevelCutter.cut(
                    level,
                    subLevel,
                    point1,
                    point2,
                    orientation.getAxis(),
                    (int) ShearsCutState.facePlaneCoord(point1, orientation)
            );

            if (result == null) {
                player.sendSystemMessage(Component.literal(
                        "Nothing separated - structure is still connected around the cut"));
                return;
            }

            player.sendSystemMessage(Component.literal(
                    "Cut sub-level into " + result.pieceCount() + " pieces"));
        }
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
                        if (shared == planeCoord && inExtent) continue;
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