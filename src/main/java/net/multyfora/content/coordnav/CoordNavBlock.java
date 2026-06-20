package net.multyfora.content.coordnav;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.network.CoordNavPayloads;

// Coordinate Navigator block: a directional redstone source that points toward a configurable
// target coordinate. It outputs redstone signals of varying strength on its lateral sides,
// proportional to how closely each direction aligns with the target. The front face (facing
// direction) never outputs a signal.
public class CoordNavBlock extends DirectionalBlock implements IBE<CoordNavBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    // Voxel shape: a small base plate, a thin pole, and a wider top cap
    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape POLE = Block.box(6, 2, 6, 10, 14, 10);
    private static final VoxelShape TOP = Block.box(1, 14, 1, 15, 16, 15);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.join(BASE, POLE, BooleanOp.OR), TOP, BooleanOp.OR);

    public CoordNavBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(CoordNavBlock::new);
    }

    // Places the block facing the clicked face
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // Opens the coordinate configuration GUI when right-clicked with an empty hand
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new CoordNavPayloads.OpenCoordNavPayload(pos));
        }
        return ItemInteractionResult.SUCCESS;
    }

    // Returns redstone signal strength for the given direction.
    // The face the block is pointing toward always returns 0 (the "indicator" face has no output).
    @Override
    public int getSignal(BlockState state, BlockGetter getter, BlockPos pos, Direction direction) {
        if (getter.getBlockEntity(pos) instanceof CoordNavBlockEntity be) {
            if (direction.getAxis() == state.getValue(FACING).getAxis()) {
                return 0;
            }
            return be.getRedstoneStrength(direction);
        }
        return 0;
    }

    // Strong (direct) redstone power: only outputs on the face opposite the facing direction
    // when the block faces horizontally, and also on the bottom face
    @Override
    public int getDirectSignal(BlockState state, BlockGetter getter, BlockPos pos, Direction direction) {
        if (direction.getAxis() == state.getValue(FACING).getAxis()) {
            return 0;
        }
        if (state.getValue(FACING).getAxis().isHorizontal() && direction == Direction.DOWN) {
            return getSignal(state, getter, pos, direction);
        }
        return 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            IBE.onRemove(state, level, pos, newState);
        }
    }

    @Override
    public Class<CoordNavBlockEntity> getBlockEntityClass() {
        return CoordNavBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CoordNavBlockEntity> getBlockEntityType() {
        return JocBlockEntityTypes.COORD_NAV.get();
    }
}
