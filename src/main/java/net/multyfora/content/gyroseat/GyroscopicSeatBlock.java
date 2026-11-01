package net.multyfora.content.gyroseat;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.common.util.FakePlayer;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GyroscopicSeatBlock extends SeatBlock implements EntityBlock {

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);

    private static final VoxelShape[] FRAME_SHAPES = new VoxelShape[8];
    private static final VoxelShape[] CUBE_SHAPES = new VoxelShape[8];

    static {
        VoxelShape frame = Shapes.empty();
        for (int a = -1; a <= 1; a += 2) {
            for (int b = -1; b <= 1; b += 2) {
                frame = Shapes.or(frame,
                    centredBox(-16, 13 * a, 13 * b, 16, 16 * a, 16 * b),
                    centredBox(13 * a, -16, 13 * b, 16 * a, 16, 16 * b),
                    centredBox(13 * a, 13 * b, -16, 16 * a, 16 * b, 16));
            }
            frame = Shapes.or(frame,
                centredBox(-13, 14.75 * a, -1.5, 13, 16 * a, 1.5),
                centredBox(-1.5, 14.75 * a, -13, 1.5, 16 * a, 13),
                centredBox(-2.5, 14.5 * a, -2.5, 2.5, 16 * a, 2.5));
        }
        for (int part = 0; part < 8; part++) {
            BlockPos offset = offsetOf(part);
            VoxelShape cell = Shapes.box(offset.getX(), offset.getY(), offset.getZ(),
                offset.getX() + 1, offset.getY() + 1, offset.getZ() + 1);
            FRAME_SHAPES[part] = Shapes.join(frame, cell, BooleanOp.AND)
                .move(-offset.getX(), -offset.getY(), -offset.getZ())
                .optimize();
            CUBE_SHAPES[part] = Shapes.box(-offset.getX(), -offset.getY(), -offset.getZ(),
                2 - offset.getX(), 2 - offset.getY(), 2 - offset.getZ());
        }
    }

    private static VoxelShape centredBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Shapes.box(
            (Math.min(x1, x2) + 16) / 16, (Math.min(y1, y2) + 16) / 16, (Math.min(z1, z2) + 16) / 16,
            (Math.max(x1, x2) + 16) / 16, (Math.max(y1, y2) + 16) / 16, (Math.max(z1, z2) + 16) / 16);
    }

    public GyroscopicSeatBlock(Properties properties, DyeColor color) {
        super(properties, color);
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(PART));
    }

    public static BlockPos offsetOf(int part) {
        return new BlockPos(part & 1, (part >> 1) & 1, (part >> 2) & 1);
    }

    private static int partOf(int dx, int dy, int dz) {
        return dx | (dy << 1) | (dz << 2);
    }

    public static BlockPos getMasterPos(BlockPos pos, BlockState state) {
        return pos.subtract(offsetOf(state.getValue(PART)));
    }

    public static Vec3 getCenter(BlockPos master) {
        return Vec3.atLowerCornerOf(master).add(1, 1, 1);
    }

    public static AABB getCubeBounds(BlockPos master) {
        return new AABB(master).expandTowards(1, 1, 1);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos master = findMasterPos(context);
        BlockState state = super.getStateForPlacement(context);
        if (master == null || state == null)
            return null;
        BlockPos rel = context.getClickedPos().subtract(master);
        return state.setValue(PART, partOf(rel.getX(), rel.getY(), rel.getZ()));
    }

    @Nullable
    private BlockPos findMasterPos(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Direction forward = context.getHorizontalDirection();
        Direction right = forward.getClockWise();
        Direction[] verticals = context.getClickedFace() == Direction.DOWN
            ? new Direction[]{Direction.DOWN, Direction.UP}
            : new Direction[]{Direction.UP, Direction.DOWN};
        for (Direction vertical : verticals) {
            for (Direction depth : new Direction[]{forward, forward.getOpposite()}) {
                for (Direction side : new Direction[]{right, right.getOpposite()}) {
                    BlockPos far = pos.relative(depth).relative(side).relative(vertical);
                    BlockPos master = new BlockPos(Math.min(pos.getX(), far.getX()),
                        Math.min(pos.getY(), far.getY()), Math.min(pos.getZ(), far.getZ()));
                    if (canFit(context, master))
                        return master;
                }
            }
        }
        return null;
    }

    private static boolean canFit(BlockPlaceContext context, BlockPos master) {
        Level level = context.getLevel();
        for (int part = 0; part < 8; part++) {
            BlockPos pos = master.offset(offsetOf(part));
            if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos))
                return false;
            if (!pos.equals(context.getClickedPos()) && !level.getBlockState(pos).canBeReplaced())
                return false;
        }
        return level.isUnobstructed(null, Shapes.create(getCubeBounds(master)));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide)
            return;
        BlockPos master = getMasterPos(pos, state);
        for (int part = 0; part < 8; part++) {
            BlockPos partPos = master.offset(offsetOf(part));
            if (partPos.equals(pos))
                continue;
            boolean water = level.getFluidState(partPos).getType() == Fluids.WATER;
            level.setBlock(partPos, state.setValue(PART, part).setValue(WATERLOGGED, water), Block.UPDATE_ALL);
        }
        if (placer != null && level.getBlockEntity(master) instanceof GyroscopicSeatBlockEntity be)
            be.rememberYaw(placer.getYRot() + 180);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        BlockPos neighborOffset = offsetOf(state.getValue(PART)).relative(direction);
        boolean insideCube = neighborOffset.getX() >= 0 && neighborOffset.getX() <= 1
            && neighborOffset.getY() >= 0 && neighborOffset.getY() <= 1
            && neighborOffset.getZ() >= 0 && neighborOffset.getZ() <= 1;
        if (insideCube && !(neighborState.is(this) && neighborState.getValue(PART)
            == partOf(neighborOffset.getX(), neighborOffset.getY(), neighborOffset.getZ())))
            return Blocks.AIR.defaultBlockState();
        return updated;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) != 0) {
            BlockPos master = getMasterPos(pos, state);
            BlockState masterState = level.getBlockState(master);
            if (masterState.is(this) && masterState.getValue(PART) == 0) {
                level.setBlock(master, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, master, Block.getId(masterState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new GyroscopicSeatBlockEntity(pos, state) : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (isRiderOf(context, pos, state))
            return Shapes.empty();
        return CUBE_SHAPES[state.getValue(PART)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (isRiderOf(context, pos, state))
            return Shapes.empty();
        return FRAME_SHAPES[state.getValue(PART)];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private static boolean isRiderOf(CollisionContext context, BlockPos pos, BlockState state) {
        return context instanceof EntityCollisionContext ecc
            && ecc.getEntity() != null
            && ecc.getEntity().getVehicle() instanceof GyroscopicSeatEntity seat
            && seat.position().distanceToSqr(getCenter(getMasterPos(pos, state))) < 1.0E-4;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || player instanceof FakePlayer)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        BlockPos master = getMasterPos(pos, state);
        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, getCubeBounds(master));
        if (!seats.isEmpty()) {
            SeatEntity seatEntity = seats.get(0);
            List<Entity> passengers = seatEntity.getPassengers();
            if (!passengers.isEmpty() && passengers.get(0) instanceof Player)
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) {
                seatEntity.ejectPassengers();
                player.startRiding(seatEntity);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        sitDown(level, master, getLeashed(level, player).or(player));
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter reader, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
    }

    public static void sitDown(Level level, BlockPos master, Entity entity) {
        if (level.isClientSide)
            return;
        GyroscopicSeatEntity seat = new GyroscopicSeatEntity(level);
        Vec3 center = getCenter(master);
        seat.setPos(center.x, center.y, center.z);
        level.addFreshEntity(seat);
        entity.startRiding(seat, true);
        if (entity instanceof TamableAnimal ta)
            ta.setInSittingPose(true);
    }
}
