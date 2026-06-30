package net.multyfora.content.playerdir;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.multyfora.index.JocBlockEntityTypes;

/**
 * Player Direction block: tracks the look direction of the last player who right-clicked it
 * and outputs variable redstone signal strengths on all 6 sides based on where that player
 * is facing. No orientation property — the block behaves identically no matter how it was
 * placed, since the redstone math is computed directly in world space.
 *
 * Right-clicking while off sets the tracked player and powers on; right-clicking while on
 * powers off.
 **/
public class PlayerDirectionBlock extends Block implements IBE<PlayerDirectionBlockEntity>, IWrenchable {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public PlayerDirectionBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(PlayerDirectionBlock::new);
    }

    /**
     * Right-click behaviour:
     *  - If currently OFF: registers this player as tracked and powers on.
     *  - If currently ON:  powers off (tracked player is preserved for next activation).
     **/
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level,
                                            BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof PlayerDirectionBlockEntity be) {
                boolean poweredNow = state.getValue(POWERED);
                if (!poweredNow) {
                    // Register the clicking player, then turn on
                    be.setTrackedPlayer(player);
                    level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
                } else {
                    // Turn off; tracked player UUID is kept in NBT for next activation
                    level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
                }
                be.onPowerToggled();
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Returns weak redstone signal strength for the given direction on all 6 sides.
     * Delegates entirely to the block entity, which uses a single uniform formula now
     * that there's no FACING property to special-case around.
     **/
    @Override
    public int getSignal(BlockState state, BlockGetter getter, BlockPos pos, Direction direction) {
        if (getter.getBlockEntity(pos) instanceof PlayerDirectionBlockEntity be) {
            return be.getRedstoneStrength(direction);
        }
        return 0;
    }

    /**
     * Strong (direct) power: provided straight down only, matching the common convention
     * for omnidirectional signal-source blocks (e.g. observers, redstone blocks).
     **/
    @Override
    public int getDirectSignal(BlockState state, BlockGetter getter, BlockPos pos, Direction direction) {
        if (direction == Direction.DOWN) {
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