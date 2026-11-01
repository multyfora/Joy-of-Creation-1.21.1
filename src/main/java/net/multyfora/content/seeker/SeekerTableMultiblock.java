package net.multyfora.content.seeker;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.index.JocBlocks;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class SeekerTableMultiblock {

    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 32;

    private enum Plane {
        FLAT(Direction.Axis.Y, Direction.EAST, Direction.SOUTH),
        ALONG_X(Direction.Axis.Z, Direction.EAST, Direction.UP),
        ALONG_Z(Direction.Axis.X, Direction.SOUTH, Direction.UP);

        final Direction.Axis normal;
        final Direction[] steps;

        Plane(Direction.Axis normal, Direction a, Direction b) {
            this.normal = normal;
            this.steps = new Direction[]{a, a.getOpposite(), b, b.getOpposite()};
        }

        boolean holds(Direction facing) {
            return facing.getAxis() == normal;
        }
    }

    private static boolean dissolving;

    private SeekerTableMultiblock() {}

    private static boolean isPart(Level level, BlockPos pos, Plane plane) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SeekerTableBlock)
            return plane.holds(state.getValue(SeekerTableBlock.FACING));
        return state.is(JocBlocks.SEEKER.get())
            && level.getBlockEntity(pos) instanceof SeekerBlockEntity seeker && !seeker.hasModule();
    }

    public static void tryForm(Level level, BlockPos origin, @Nullable Player placer) {
        if (level.isClientSide || dissolving)
            return;
        for (Plane plane : Plane.values()) {
            if (isPart(level, origin, plane) && tryForm(level, origin, placer, plane))
                return;
        }
    }

    private static boolean tryForm(Level level, BlockPos origin, @Nullable Player placer, Plane plane) {
        Set<BlockPos> group = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        group.add(origin);
        queue.add(origin);
        BlockPos min = origin, max = origin;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction direction : plane.steps) {
                BlockPos next = pos.relative(direction);
                if (group.contains(next) || !level.isLoaded(next) || !isPart(level, next, plane))
                    continue;
                group.add(next);
                if (group.size() > MAX_SIZE * MAX_SIZE)
                    return false;
                queue.add(next);
                min = BlockPos.min(min, next);
                max = BlockPos.max(max, next);
            }
        }

        SeekerTableBlockEntity.Settings settings = null;
        Direction facing = null;
        int bestArea = 1;
        for (BlockPos pos : group) {
            if (level.getBlockEntity(pos) instanceof SeekerTableBlockEntity table) {
                int area = table.getWidth() * table.getDepth();
                if (area > bestArea) {
                    bestArea = area;
                    settings = table.getSettings();
                    facing = table.getFacing();
                }
            }
        }
        if (facing == null)
            facing = plane == Plane.FLAT ? Direction.UP : chooseFacing(level, min, max, plane, placer);

        Direction right = SeekerTableBlock.toWorld(facing, Direction.EAST);
        Direction down = SeekerTableBlock.toWorld(facing, Direction.SOUTH);
        int width = extent(min, max, right.getAxis());
        int depth = extent(min, max, down.getAxis());
        if (width < MIN_SIZE || depth < MIN_SIZE || width > MAX_SIZE || depth > MAX_SIZE)
            return false;
        if (group.size() != width * depth)
            return false;

        BlockPos corner = new BlockPos(
            min.getX() == max.getX() ? min.getX() : pick(min, max, Direction.Axis.X, right, down),
            min.getY() == max.getY() ? min.getY() : pick(min, max, Direction.Axis.Y, right, down),
            min.getZ() == max.getZ() ? min.getZ() : pick(min, max, Direction.Axis.Z, right, down));
        BlockPos far = corner.relative(right, width - 1).relative(down, depth - 1);
        if (level.getBlockEntity(corner) instanceof SeekerTableBlockEntity existing && existing.isController()
            && existing.getFacing() == facing && existing.getWidth() == width && existing.getDepth() == depth)
            return true;

        BlockState base = JocBlocks.SEEKER_TABLE.get().defaultBlockState().setValue(SeekerTableBlock.FACING, facing);
        dissolving = true;
        try {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    BlockState state = base
                        .setValue(SeekerTableBlock.NORTH, dz > 0)
                        .setValue(SeekerTableBlock.SOUTH, dz < depth - 1)
                        .setValue(SeekerTableBlock.WEST, dx > 0)
                        .setValue(SeekerTableBlock.EAST, dx < width - 1);
                    BlockPos pos = corner.relative(right, dx).relative(down, dz);
                    level.setBlock(pos, state, Block.UPDATE_ALL);
                    if (level.getBlockEntity(pos) instanceof SeekerTableBlockEntity table && !pos.equals(corner))
                        table.resetAsPart();
                }
            }
        } finally {
            dissolving = false;
        }

        if (level.getBlockEntity(corner) instanceof SeekerTableBlockEntity controller)
            controller.assemble(width, depth, settings);

        Vec3 middle = Vec3.atCenterOf(corner).add(Vec3.atCenterOf(far)).scale(0.5);
        level.playSound(null, BlockPos.containing(middle), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
            SoundSource.BLOCKS, 1.0f, 0.8f);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 out = Vec3.atLowerCornerOf(facing.getNormal());
            double spreadX = facing.getAxis() == Direction.Axis.X ? 0.05 : 0.25;
            double spreadY = facing.getAxis() == Direction.Axis.Y ? 0.05 : 0.25;
            double spreadZ = facing.getAxis() == Direction.Axis.Z ? 0.05 : 0.25;
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    Vec3 at = Vec3.atCenterOf(corner.relative(right, dx).relative(down, dz)).add(out.scale(0.5));
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON,
                        at.x, at.y, at.z, 1, spreadX, spreadY, spreadZ, 0.0);
                }
            }
        }
        if (placer != null)
            placer.displayClientMessage(Component.translatable("joc.seeker_table.formed", width, depth), true);
        return true;
    }

    private static int extent(BlockPos min, BlockPos max, Direction.Axis axis) {
        return max.get(axis) - min.get(axis) + 1;
    }

    private static int pick(BlockPos min, BlockPos max, Direction.Axis axis, Direction right, Direction down) {
        Direction along = right.getAxis() == axis ? right : down;
        return along.getAxisDirection() == Direction.AxisDirection.POSITIVE ? min.get(axis) : max.get(axis);
    }

    private static Direction chooseFacing(Level level, BlockPos min, BlockPos max, Plane plane, @Nullable Player placer) {
        Direction positive = Direction.fromAxisAndDirection(plane.normal, Direction.AxisDirection.POSITIVE);
        double middle = (min.get(plane.normal) + max.get(plane.normal) + 1) / 2.0;
        if (placer != null) {
            Vec3 eye = placer.getEyePosition();
            SubLevel subLevel = Sable.HELPER.getContaining(level, min);
            if (subLevel != null)
                eye = subLevel.logicalPose().transformPositionInverse(eye);
            double side = plane.normal.choose(eye.x, eye.y, eye.z) - middle;
            if (Math.abs(side) > 0.01)
                return side > 0 ? positive : positive.getOpposite();
        }
        boolean openPositive = level.isEmptyBlock(min.relative(positive));
        boolean openNegative = level.isEmptyBlock(min.relative(positive.getOpposite()));
        return openNegative && !openPositive ? positive.getOpposite() : positive;
    }

    public static void dissolve(Level level, BlockPos removed, BlockState removedState) {
        if (level.isClientSide || dissolving)
            return;

        Direction facing = removedState.getValue(SeekerTableBlock.FACING);
        Direction right = SeekerTableBlock.toWorld(facing, Direction.EAST);
        Direction down = SeekerTableBlock.toWorld(facing, Direction.SOUTH);
        BlockPos controller = SeekerTableBlock.getControllerPos(level, removed, removedState);
        int width = 1, depth = 1;
        BlockState state = controller.equals(removed) ? removedState : level.getBlockState(controller);
        if (!SeekerTableBlock.isCellOf(state, facing))
            return;
        for (BlockPos.MutableBlockPos cursor = controller.mutable(); width < MAX_SIZE && state.getValue(SeekerTableBlock.EAST); width++) {
            cursor.move(right);
            state = cursor.equals(removed) ? removedState : level.getBlockState(cursor);
            if (!SeekerTableBlock.isCellOf(state, facing))
                break;
        }
        state = controller.equals(removed) ? removedState : level.getBlockState(controller);
        for (BlockPos.MutableBlockPos cursor = controller.mutable(); depth < MAX_SIZE && state.getValue(SeekerTableBlock.SOUTH); depth++) {
            cursor.move(down);
            state = cursor.equals(removed) ? removedState : level.getBlockState(cursor);
            if (!SeekerTableBlock.isCellOf(state, facing))
                break;
        }

        BlockState seeker = JocBlocks.SEEKER.get().defaultBlockState();
        dissolving = true;
        try {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    BlockPos pos = controller.relative(right, dx).relative(down, dz);
                    if (!pos.equals(removed) && SeekerTableBlock.isCellOf(level.getBlockState(pos), facing))
                        level.setBlock(pos, seeker, Block.UPDATE_ALL);
                }
            }
        } finally {
            dissolving = false;
        }
    }
}
