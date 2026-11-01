package net.multyfora.content.seeker;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocItems;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class SeekerTableBlock extends Block implements IBE<SeekerTableBlockEntity>, IWrenchable {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final DirectionProperty FACING = DirectionProperty.create("facing", d -> d != Direction.DOWN);

    public static final int TABLE_MAP_Y = 13, WALL_MAP_Y = 5;
    public static final int RIM_RISE = 2;

    private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : FACING.getPossibleValues()) {
            VoxelShape[] shapes = new VoxelShape[16];
            boolean wall = facing != Direction.UP;
            double y = wall ? WALL_MAP_Y : TABLE_MAP_Y, rim = y + RIM_RISE;
            for (int flags = 0; flags < 16; flags++) {
                VoxelShape shape = localBox(facing, 0, 0, 0, 16, y, 16);
                if ((flags & 1) == 0) shape = Shapes.or(shape, localBox(facing, 0, y, 0, 16, rim, 2));
                if ((flags & 2) == 0) shape = Shapes.or(shape, localBox(facing, 14, y, 0, 16, rim, 16));
                if ((flags & 4) == 0) shape = Shapes.or(shape, localBox(facing, 0, y, 14, 16, rim, 16));
                if ((flags & 8) == 0) shape = Shapes.or(shape, localBox(facing, 0, y, 0, 2, rim, 16));
                if (wall && (flags & 4) == 0) shape = Shapes.or(shape, localBox(facing, 0, rim, 13, 16, rim + 3, 16));
                shapes[flags] = shape.optimize();
            }
            SHAPES.put(facing, shapes);
        }
    }

    public SeekerTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP)
            .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(SeekerTableBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING, NORTH, EAST, SOUTH, WEST));
    }

    public static BooleanProperty connection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    public static boolean isWall(BlockState state) {
        return state.getValue(FACING) != Direction.UP;
    }

    public static Direction toWorld(Direction facing, Direction local) {
        if (facing == Direction.UP)
            return local;
        return switch (local) {
            case NORTH -> Direction.UP;
            case SOUTH -> Direction.DOWN;
            case EAST -> facing.getCounterClockWise();
            case WEST -> facing.getClockWise();
            case UP -> facing;
            case DOWN -> facing.getOpposite();
        };
    }

    public static VoxelShape localBox(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        if (facing == Direction.UP)
            return Block.box(x0, y0, z0, x1, y1, z1);
        Vec3 a = localToCell(facing, x0, y0, z0), b = localToCell(facing, x1, y1, z1);
        return Block.box(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
            Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    private static Vec3 localToCell(Direction facing, double x, double y, double z) {
        Direction r = toWorld(facing, Direction.EAST);
        x -= 8;
        y -= 8;
        z -= 8;
        return new Vec3(
            8 + x * r.getStepX() + y * facing.getStepX(),
            8 - z,
            8 + x * r.getStepZ() + y * facing.getStepZ());
    }

    public static boolean isController(BlockState state) {
        return !state.getValue(NORTH) && !state.getValue(WEST);
    }

    public static BlockPos getControllerPos(BlockGetter level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction west = toWorld(facing, Direction.WEST), north = toWorld(facing, Direction.NORTH);
        BlockPos.MutableBlockPos cursor = pos.mutable();
        BlockState current = state;
        for (int i = 0; i < SeekerTableMultiblock.MAX_SIZE && current.getValue(WEST); i++) {
            BlockState next = level.getBlockState(cursor.move(west));
            if (!isCellOf(next, facing)) {
                cursor.move(west.getOpposite());
                break;
            }
            current = next;
        }
        for (int i = 0; i < SeekerTableMultiblock.MAX_SIZE && current.getValue(NORTH); i++) {
            BlockState next = level.getBlockState(cursor.move(north));
            if (!isCellOf(next, facing)) {
                cursor.move(north.getOpposite());
                break;
            }
            current = next;
        }
        return cursor.immutable();
    }

    static boolean isCellOf(BlockState state, Direction facing) {
        return state.getBlock() instanceof SeekerTableBlock && state.getValue(FACING) == facing;
    }

    @Nullable
    public static SeekerTableBlockEntity getController(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SeekerTableBlock))
            return null;
        return level.getBlockEntity(getControllerPos(level, pos, state)) instanceof SeekerTableBlockEntity be
            && be.isController() ? be : null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        if (isWall(state))
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        BlockState turned = state;
        for (Direction side : Direction.Plane.HORIZONTAL)
            turned = turned.setValue(connection(rotation.rotate(side)), state.getValue(connection(side)));
        return turned;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE)
            return state;
        if (isWall(state))
            return state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
                .setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
        BlockState mirrored = state;
        for (Direction side : Direction.Plane.HORIZONTAL)
            mirrored = mirrored.setValue(connection(mirror.mirror(side)), state.getValue(connection(side)));
        return mirrored;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int flags = (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
            | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
        return SHAPES.get(state.getValue(FACING))[flags];
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(JocItems.SEEKER.get());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        SeekerTableMarker style = SeekerTableMarker.forItem(stack);
        if (style == null || hit.getDirection() != state.getValue(FACING))
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        InteractionResult result = useMap(level, pos, player, hit, style, SeekerTableMarker.colorFor(stack));
        return result.consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (hit.getDirection() != state.getValue(FACING))
            return InteractionResult.PASS;
        return useMap(level, pos, player, hit, SeekerTableMarker.PIN, SeekerTableMarker.DEFAULT_COLOR);
    }

    private static InteractionResult useMap(Level level, BlockPos pos, Player player, BlockHitResult hit,
                                            SeekerTableMarker style, int color) {
        SeekerTableBlockEntity table = getController(level, pos);
        if (table == null)
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            if (table.hasMarker()) {
                table.clearMarker();
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 1.2f);
            }
            return InteractionResult.SUCCESS;
        }

        if (table.placeMarkerAt(hit.getLocation(), pos, style, color))
            level.playSound(null, pos, style.placeSound, SoundSource.BLOCKS, 0.8f, style.pitch);
        return InteractionResult.SUCCESS;
    }

    public static boolean markFromProjectile(Level level, BlockHitResult hit, SeekerTableMarker style, int color) {
        if (level.isClientSide || hit.getType() != HitResult.Type.BLOCK)
            return false;
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SeekerTableBlock) || hit.getDirection() != state.getValue(FACING))
            return false;
        SeekerTableBlockEntity table = getController(level, pos);
        if (table == null || !table.placeMarkerAt(hit.getLocation(), pos, style, color))
            return false;
        level.playSound(null, pos, style.placeSound, SoundSource.BLOCKS, 0.8f, style.pitch);
        return true;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        SeekerTableBlockEntity table = getController(context.getLevel(), context.getClickedPos());
        if (table == null)
            return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            table.cycleScale();
            if (context.getPlayer() != null)
                context.getPlayer().displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("joc.seeker_table.scale_set", table.getScale()), true);
        }
        IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()))
            SeekerTableMultiblock.dissolve(level, pos, state);
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<SeekerTableBlockEntity> getBlockEntityClass() {
        return SeekerTableBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SeekerTableBlockEntity> getBlockEntityType() {
        return JocBlockEntityTypes.SEEKER_TABLE.get();
    }
}
