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
import org.spongepowered.asm.mixin.Unique;

/**
 * Coordinate Navigator block: a directional redstone source that points toward a configurable
 * target coordinate. It outputs redstone signals of varying strength on its lateral sides,
 * proportional to how closely each direction aligns with the target. The front face (facing
 * direction) never outputs a signal.
 **/
public class CoordNavBlock extends DirectionalBlock implements IBE<CoordNavBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;

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
        return getShape();
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

    /**
     * Returns redstone signal strength for the given direction.
     * The face the block is pointing toward always returns 0 (the "indicator" face has no output).
     **/
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

    /**
     * Strong (direct) redstone power: only outputs on the face opposite the facing direction
     * when the block faces horizontally, and also on the bottom face
     **/
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

    @Unique
    private static VoxelShape getShape() {
        final VoxelShape MAGNETS; {
            final VoxelShape BOTTOM_MAGNET = Block.box(3, 0,    3, 16-3, 3,  16-3);
            final VoxelShape    TOP_MAGNET = Block.box(3, 16-3, 3, 16-3, 16, 16-3);

            MAGNETS = Shapes.join(BOTTOM_MAGNET, TOP_MAGNET, BooleanOp.OR);
        }

        final VoxelShape POLES; {
            final VoxelShape NORTHEAST = Block.box(0,    0, 0,    2,    16, 2 );
            final VoxelShape NORTHWEST = Block.box(16-2, 0, 0,    16,   16, 2 );
            final VoxelShape SOUTHEAST = Block.box(0,    0, 16-2, 2,    16, 16);
            final VoxelShape SOUTHWEST = Block.box(16-2, 0, 16-2, 16,   16, 16);

            POLES = Shapes.join(
                Shapes.join(NORTHEAST, NORTHWEST, BooleanOp.OR),
                Shapes.join(SOUTHEAST, SOUTHWEST, BooleanOp.OR),
                BooleanOp.OR
            );
        }

        final VoxelShape CENTER = Block.box(8-1, 8-1, 8-1, 8+1, 8+1 ,8+1);

        final VoxelShape PADS; {
            final VoxelShape NORTHEAST_FIRST  = Block.box(0, 8-2, 0, 4-2, 8+2, 4-1);
            final VoxelShape NORTHEAST_SECOND = Block.box(0, 8-2, 0, 4-1, 8+2, 4-2);
            final VoxelShape NORTHWEST_FIRST  = Block.box(0, 8-2, 16-2, 3, 8+2, 16);
            final VoxelShape NORTHWEST_SECOND = Block.box(0, 8-2, 16-3, 2, 8+2, 16);
            final VoxelShape SOUTHEAST_FIRST  = Block.box(16-2, 8-2, 0, 16, 8+2, 3);
            final VoxelShape SOUTHEAST_SECOND = Block.box(16-3, 8-2, 0, 16, 8+2, 2);
            final VoxelShape SOUTHWEST_FIRST  = Block.box(16-2, 8-2, 16-3, 16, 8+2, 16);
            final VoxelShape SOUTHWEST_SECOND = Block.box(16-3, 8-2, 16-2, 16, 8+2, 16);

            PADS = Shapes.join(
                Shapes.join(
                    Shapes.join(NORTHEAST_FIRST, NORTHEAST_SECOND, BooleanOp.OR),
                    Shapes.join(NORTHWEST_FIRST, NORTHWEST_SECOND, BooleanOp.OR),
                    BooleanOp.OR
                ),
                Shapes.join(
                    Shapes.join(SOUTHEAST_FIRST, SOUTHEAST_SECOND, BooleanOp.OR),
                    Shapes.join(SOUTHWEST_FIRST, SOUTHWEST_SECOND, BooleanOp.OR),
                    BooleanOp.OR
                ),
                BooleanOp.OR
            );
        }

        return Shapes.join(
            Shapes.join(MAGNETS, POLES, BooleanOp.OR),
            Shapes.join(CENTER, PADS, BooleanOp.OR),
            BooleanOp.OR
        );
    }
}
