package net.multyfora.content.playerdir;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.multyfora.index.JocBlockEntityTypes;

/**
 * Player Direction block: tracks the nearest player's look direction within 10 blocks and
 * outputs variable redstone signal strengths on its lateral sides based on where the player
 * is looking. Right-clicking toggles the block on/off via the POWERED property.
 **/
public class PlayerDirectionBlock extends DirectionalBlock implements IBE<PlayerDirectionBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public PlayerDirectionBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(PlayerDirectionBlock::new);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    // Toggles the block on/off when right-clicked with an empty hand
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            BlockState newState = state.cycle(POWERED);
            level.setBlock(pos, newState, 3);
            if (level.getBlockEntity(pos) instanceof PlayerDirectionBlockEntity be) {
                be.onPowerToggled();
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Returns redstone signal strength for the given direction.
     * The face the block is pointing toward always returns 0.
     **/
    @Override
    public int getSignal(BlockState state, BlockGetter getter, BlockPos pos, Direction direction) {
        if (getter.getBlockEntity(pos) instanceof PlayerDirectionBlockEntity be) {
            if (direction.getAxis() == state.getValue(FACING).getAxis()) {
                return 0;
            }
            return be.getRedstoneStrength(direction);
        }
        return 0;
    }

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
    public Class<PlayerDirectionBlockEntity> getBlockEntityClass() {
        return PlayerDirectionBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PlayerDirectionBlockEntity> getBlockEntityType() {
        return JocBlockEntityTypes.PLAYER_DIRECTION.get();
    }
}
