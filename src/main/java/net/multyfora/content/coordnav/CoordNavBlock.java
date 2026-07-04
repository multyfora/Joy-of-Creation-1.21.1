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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.multyfora.content.VoxelUtils;
import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.network.CoordNavPayloads;
import org.jetbrains.annotations.NotNull;

import static net.multyfora.content.VoxelUtils.rotateShape;

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
    protected @NotNull MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(CoordNavBlock::new);
    }

    // Places the block facing the clicked face
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue( FACING, ctx.getClickedFace() );
    }

    @Override
    protected @NotNull VoxelShape getShape(
        BlockState state, @NotNull BlockGetter level,
        @NotNull BlockPos pos, @NotNull CollisionContext ctx
    ) {
        final VoxelShape SHAPE = VoxelUtils.combineVoxelShapes(
            //MAGNETS
            Block.box(3, 0,    3, 16-3, 16,  16-3),
//            Block.box(3, 16-3, 3, 16-3, 16, 16-3),
            //POLES
            Block.box(0,    0, 0,    2,    16, 2 ),
            Block.box(16-2, 0, 0,    16,   16, 2 ),
            Block.box(0,    0, 16-2, 2,    16, 16),
            Block.box(16-2, 0, 16-2, 16,   16, 16),
            //CENTER
//            Block.box(8-1, 8-1, 8-1, 8+1, 8+1 ,8+1),
            //PADS
            Block.box(0,    8-2, 0,    4-2, 8+2, 4-1),
            Block.box(0,    8-2, 16-2, 3,   8+2, 16 ),
            Block.box(0,    8-2, 16-3, 2,   8+2, 16 ),
            Block.box(0,    8-2, 0,    4-1, 8+2, 4-2),
            Block.box(16-3, 8-2, 0,    16,  8+2, 2  ),
            Block.box(16-2, 8-2, 0,    16,  8+2, 3  ),
            Block.box(16-2, 8-2, 16-3, 16,  8+2, 16 ),
            Block.box(16-3, 8-2, 16-2, 16,  8+2, 16 )
        );

        Direction.Axis axis = state.getValue(FACING).getAxis();
        return rotateShape(axis, SHAPE);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull ItemStack stack,
            @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof CoordNavBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Shift + right-click: extract the installed module, if any
        if (player.isShiftKeyDown()) {
            if (be.hasModule()) {
                if (!level.isClientSide) {
                    be.extractModule(player);
                }
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Right-click with a valid module item while empty: install it
        if (!be.hasModule() && !stack.isEmpty()) {
            if (!level.isClientSide) {
                boolean inserted = be.insertModule(stack, player);
                return inserted ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            return ItemInteractionResult.SUCCESS;
        }

        // Right-click with empty hand while a module is installed: behavior depends on module type
        if (stack.isEmpty() && be.hasModule()) {
            switch (be.getModule()) {
                case SPYGLASS -> {
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection.send(new CoordNavPayloads.OpenCoordNavPayload(pos));
                    }
                    return ItemInteractionResult.SUCCESS;
                }
                case PLAYER_DIR -> {
                    if (!level.isClientSide) {
                        be.onPlayerDirActivated(player);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
                default -> {}
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Returns redstone signal strength for the given direction.
     * The face the block is pointing toward always returns 0 (the "indicator" face has no output).
     **/
    @Override
    public int getSignal(
            @NotNull BlockState state, BlockGetter getter,
            @NotNull BlockPos pos, @NotNull Direction direction
    ) {
        if( getter.getBlockEntity(pos) instanceof CoordNavBlockEntity blockEntity ) {
            return blockEntity.getRedstoneStrength(direction);
        }
        return 0;
    }

    /**
     * Strong (direct) redstone power: outputs identically to weak power.
     **/
    @Override
    public int getDirectSignal(
            BlockState state, @NotNull BlockGetter getter,
            @NotNull BlockPos pos, Direction direction
    ) {
        return getSignal(state, getter, pos, direction);
    }

    @Override
    public boolean isSignalSource(@NotNull BlockState state) {
        return true;
    }

    @Override
    public void onRemove(
        BlockState state, @NotNull Level level,
        @NotNull BlockPos pos, BlockState newState,
        boolean movedByPiston
    ) {
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
